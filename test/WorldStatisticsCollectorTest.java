import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collector;

public final class WorldStatisticsCollectorTest {
    // Energy loss uses repeated floating-point subtraction.
    private static final double TOLERANCE = 1e-9;

    public static void main(String[] args) {
        testEmptyAndInactivePopulations();
        testMetricsMatchLoopAndExcludeMilitary();
        testCollectorContract(WorldStatistics.collector(42));
        testPublishedSnapshotsRemainStable();
        System.out.println("WorldStatisticsCollectorTest passed");
    }

    private static void testEmptyAndInactivePopulations() {
        checkEmpty(WorldStatistics.capture(7, List.of()));
        Human inactive = civilianAfterTicks(50);
        inactive.enterBuilding(new Building(0, 0, 150, 100));
        inactive.deactivate();
        Military soldier = new Military(200, 200);
        soldier.deactivate();
        Zombie zombie = new Zombie(300, 300);
        zombie.deactivate();
        Food food = new Food(400, 400, 45);
        food.deactivate();
        checkEmpty(WorldStatistics.capture(7, List.of(inactive, soldier, zombie, food)));
        WorldStatistics militaryOnly = WorldStatistics.capture(8,
                List.of(new Military(200, 200)));
        check(militaryOnly.getMilitaryCount() == 1 && militaryOnly.getHumanCount() == 0,
                "Soldiers must retain their separate population category");
        check(militaryOnly.getAverageCivilianEnergy().isEmpty()
                        && militaryOnly.getLowestCivilianEnergy().isEmpty()
                        && militaryOnly.getShelteredCivilianCount() == 0,
                "Soldiers must not supply civilian health or shelter metrics");
    }

    private static void testMetricsMatchLoopAndExcludeMilitary() {
        Human low = civilianAfterTicks(450);
        Human middle = civilianAfterTicks(400);
        Human high = civilianAfterTicks(50);
        high.enterBuilding(new Building(0, 0, 150, 100));
        Human inactive = new Human(300, 300);
        inactive.enterBuilding(new Building(0, 0, 150, 100));
        inactive.deactivate();
        Military soldier = new Military(200, 200);
        soldier.enterBuilding(new Building(0, 0, 150, 100));
        List<Entity> entities = List.of(low, middle, high, inactive, soldier,
                new Zombie(500, 500), new Food(600, 600, 45), new Building(0, 0, 150, 100));
        WorldStatistics result = WorldStatistics.capture(19, entities);
        long civilians = 0, military = 0, zombies = 0, foods = 0, sheltered = 0;
        double energySum = 0, energyMin = Double.POSITIVE_INFINITY;
        for (Entity entity : entities) {
            if (!entity.isActive()) continue;
            if (entity.getClass() == Human.class) {
                Human human = (Human) entity;
                civilians++;
                energySum += human.getEnergy();
                energyMin = Math.min(energyMin, human.getEnergy());
                if (human.isHidden()) sheltered++;
            } else if (entity.getClass() == Military.class) military++;
            else if (entity.getClass() == Zombie.class) zombies++;
            else if (entity.getClass() == Food.class) foods++;
        }
        check(result.getTick() == 19 && result.getHumanCount() == civilians
                        && result.getMilitaryCount() == military && result.getZombieCount() == zombies
                        && result.getFoodCount() == foods && result.getShelteredCivilianCount() == sheltered,
                "All counts must agree with an independent ordinary-loop oracle");
        close(result.getAverageCivilianEnergy().orElseThrow(), energySum / civilians);
        close(result.getLowestCivilianEnergy().orElseThrow(), energyMin);
        close(result.getAverageCivilianEnergy().orElseThrow(), 40);
        close(result.getLowestCivilianEnergy().orElseThrow(), 10);
        check(result.getShelteredCivilianCount() == 1, "Only the active sheltered civilian counts");
    }

    private static <A> void testCollectorContract(Collector<Entity, A, WorldStatistics> collector) {
        Human low = civilianAfterTicks(450);
        Human middle = civilianAfterTicks(400);
        Human high = civilianAfterTicks(50);
        high.enterBuilding(new Building(0, 0, 150, 100));
        Human inactive = new Human(300, 300);
        inactive.deactivate();
        List<Entity> first = List.of(low, middle, new Food(200, 200, 45));
        List<Entity> second = List.of(high, new Military(200, 200));
        List<Entity> third = List.of(inactive, new Zombie(400, 400));
        List<Entity> all = new ArrayList<>();
        all.addAll(first);
        all.addAll(second);
        all.addAll(third);
        WorldStatistics expected = WorldStatistics.capture(42, all);

        A combined = collector.combiner().apply(accumulate(collector, first), accumulate(collector, second));
        WorldStatistics uneven = collector.finisher().apply(combined);
        close(uneven.getAverageCivilianEnergy().orElseThrow(), 40);
        check(uneven.getMilitaryCount() == 1 && uneven.getFoodCount() == 1
                        && uneven.getShelteredCivilianCount() == 1,
                "Combination must merge categories and shelter counts as well as energy");

        A left = collector.combiner().apply(
                collector.combiner().apply(accumulate(collector, first), accumulate(collector, second)),
                accumulate(collector, third));
        A right = collector.combiner().apply(accumulate(collector, first),
                collector.combiner().apply(accumulate(collector, second), accumulate(collector, third)));
        same(collector.finisher().apply(left), expected);
        same(collector.finisher().apply(right), expected);
        same(collector.finisher().apply(collector.combiner().apply(
                accumulate(collector, all), collector.supplier().get())), expected);
        same(collector.finisher().apply(collector.combiner().apply(
                collector.supplier().get(), accumulate(collector, all))), expected);

        A independent = collector.supplier().get();
        A growing = accumulate(collector, first);
        checkEmpty(collector.finisher().apply(independent));
        WorldStatistics finished = collector.finisher().apply(growing);
        collector.accumulator().accept(growing, high);
        check(finished.getHumanCount() == 2 && finished.getShelteredCivilianCount() == 0,
                "Finishing must not expose the mutable accumulator");
        close(finished.getAverageCivilianEnergy().orElseThrow(), 15);
        same(collector.finisher().apply(growing),
                WorldStatistics.capture(42, Arrays.asList(low, middle, first.get(2), high)));
        check(!collector.characteristics().contains(Collector.Characteristics.CONCURRENT)
                        && !collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH),
                "The collector must not advertise concurrent mutation or an identity finisher");
    }

    private static void testPublishedSnapshotsRemainStable() {
        World world = new World(1000, 750, (ignoredWorld, tick) -> { });
        PopulationHistory history = new PopulationHistory(4);
        List<WorldStatistics> delivered = new ArrayList<>();
        world.addObserver(history);
        world.addObserver(delivered::add);
        Human human = civilianAfterTicks(50);
        human.enterBuilding(new Building(0, 0, 150, 100));
        world.addEntity(human);
        world.publishStatistics();
        WorldStatistics old = delivered.get(0);
        double oldEnergy = human.getEnergy();
        check(history.getSamples().get(0) == old, "Consumers must share the same published snapshot");
        human.leaveBuilding();
        world.update();
        check(delivered.get(1).getShelteredCivilianCount() == 0,
                "Later publications must reflect leaving shelter");
        close(delivered.get(1).getAverageCivilianEnergy().orElseThrow(), oldEnergy - 0.2);
        human.deactivate();
        world.reset();
        check(old.getHumanCount() == 1 && old.getShelteredCivilianCount() == 1,
                "Entity mutation and world reset must not change an old snapshot");
        close(old.getAverageCivilianEnergy().orElseThrow(), oldEnergy);
        close(old.getLowestCivilianEnergy().orElseThrow(), oldEnergy);
        checkEmpty(world.getStatistics());
    }

    private static Human civilianAfterTicks(int ticks) {
        Human human = new Human(500, 500);
        World empty = new World(1000, 750, (world, tick) -> { });
        for (int tick = 0; tick < ticks; tick++) human.update(empty);
        check(human.isActive(), "The energy fixture must remain alive");
        return human;
    }

    private static <A> A accumulate(Collector<Entity, A, WorldStatistics> collector, List<Entity> entities) {
        A container = collector.supplier().get();
        for (Entity entity : entities) collector.accumulator().accept(container, entity);
        return container;
    }

    private static void same(WorldStatistics actual, WorldStatistics expected) {
        check(actual.getTick() == expected.getTick() && actual.getHumanCount() == expected.getHumanCount()
                        && actual.getMilitaryCount() == expected.getMilitaryCount()
                        && actual.getZombieCount() == expected.getZombieCount()
                        && actual.getFoodCount() == expected.getFoodCount()
                        && actual.getShelteredCivilianCount() == expected.getShelteredCivilianCount(),
                "Partitioning must preserve the tick and exact population/shelter counts");
        sameEnergy(actual.getAverageCivilianEnergy(), expected.getAverageCivilianEnergy());
        sameEnergy(actual.getLowestCivilianEnergy(), expected.getLowestCivilianEnergy());
    }

    private static void sameEnergy(OptionalDouble actual, OptionalDouble expected) {
        check(actual.isPresent() == expected.isPresent(), "Energy presence must agree");
        if (actual.isPresent()) close(actual.getAsDouble(), expected.getAsDouble());
    }

    private static void checkEmpty(WorldStatistics result) {
        check(result.getHumanCount() == 0 && result.getMilitaryCount() == 0
                        && result.getZombieCount() == 0 && result.getFoodCount() == 0
                        && result.getShelteredCivilianCount() == 0
                        && result.getAverageCivilianEnergy().isEmpty()
                        && result.getLowestCivilianEnergy().isEmpty(),
                "An empty/inactive population must have zero counts and absent energy");
    }

    private static void close(double actual, double expected) {
        check(Math.abs(actual - expected) <= TOLERANCE, "Expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
