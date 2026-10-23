import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Startup placement checks adapted to the fork's selected populations. */
public final class MainSpawnTest {
    private static final SimulationSettings SETTINGS = new SimulationSettings(4, 3, 2);

    public static void main(String[] args) throws Exception {
        testExtremeStartsAllowMilitaryPatrolProgress();
        testControlledInitialLayoutsRepeatAndVary();
        testStaticSceneAndInsertionOrderArePreserved();
        testResetPublishesSelectedPopulationOnce();
        testZeroMobilePopulationIsPreserved();
        System.out.println("MainSpawnTest passed");
    }

    private static void testExtremeStartsAllowMilitaryPatrolProgress() throws Exception {
        for (double value : new double[]{0.0, Math.nextDown(1.0)}) {
            World world = populated(SETTINGS, new FixedRandom(value));
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Human || entity instanceof Zombie) {
                    double marginX = entity instanceof Military ? 180 : 80;
                    double marginY = entity instanceof Military ? 140 : 80;
                    double x = entity.getX();
                    double y = entity.getY();
                    if (entity instanceof Military) {
                        Military military = (Military) entity;
                        double initialY = military.getY();
                        World patrolWorld = new World(1000, 750);
                        patrolWorld.addEntity(military);
                        for (int tick = 0; tick < 130; tick++) {
                            military.update(patrolWorld);
                        }
                        check(Math.abs(military.getY() - initialY) > 10,
                                "Edge spawns must let military progress past the first patrol waypoint");
                    }
                    check(x >= marginX && x <= world.getWidth() - marginX
                                    && y >= marginY && y <= world.getHeight() - marginY,
                            "Initial mobile positions should remain inside their spawn margins");
                }
            }
        }
    }

    private static void testControlledInitialLayoutsRepeatAndVary() throws Exception {
        List<String> first = positions(populated(SETTINGS, new Random(13)));
        List<String> repeat = positions(populated(SETTINGS, new Random(13)));
        List<String> different = positions(populated(SETTINGS, new Random(29)));
        check(first.equals(repeat), "Equal random inputs should reproduce the initial layout");
        check(first.size() == 9 && different.size() == 9,
                "Placement should retain the selected mobile population");
        for (int index = 0; index < first.size(); index++) {
            check(!first.get(index).equals(different.get(index)),
                    "Each mobile agent should use the selected random input");
        }
    }

    private static void testStaticSceneAndInsertionOrderArePreserved() throws Exception {
        World world = populated(SETTINGS, new Random(7));
        checkPopulation(world.getStatistics(), SETTINGS);
        List<String> stationary = new ArrayList<>();
        List<String> order = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            order.add(entity.getClass().getSimpleName());
            if (entity instanceof Building) {
                Building building = (Building) entity;
                stationary.add("Building:" + building.getX() + ":" + building.getY());
                check(building.getWidth() == 150 && building.getHeight() == 100,
                        "Shelter dimensions should remain unchanged");
            } else if (entity instanceof Food) {
                stationary.add("Food:" + entity.getX() + ":" + entity.getY());
                check(((Food) entity).getEnergyValue() == 45,
                        "Initial food should retain its energy value");
            }
        }
        check(stationary.equals(List.of("Building:155.0:125.0", "Building:580.0:490.0",
                        "Food:250.0:300.0", "Food:500.0:600.0", "Food:850.0:150.0")),
                "Placement must retain the fork's fixed scenery");
        check(order.equals(List.of("Building", "Building", "Human", "Human", "Human", "Human",
                        "Food", "Food", "Food", "Military", "Military", "Military", "Zombie", "Zombie")),
                "Placement must preserve the fork's entity insertion and update order");
    }

    private static void testResetPublishesSelectedPopulationOnce() throws Exception {
        World world = new World(1000, 750);
        List<WorldStatistics> samples = new ArrayList<>();
        world.addObserver(samples::add);
        populate(world, SETTINGS, new Random(13));
        List<String> first = positions(world);
        check(samples.size() == 1, "Setup should publish one complete initial population");
        checkPopulation(samples.get(0), SETTINGS);
        world.update();
        world.reset();
        check(samples.size() == 2, "Reset must not publish the empty world");
        populate(world, SETTINGS, new Random(29));
        check(samples.size() == 3, "Repopulation should retain subscriptions and publish once");
        checkPopulation(samples.get(2), SETTINGS);
        check(!first.equals(positions(world)), "A new random input should change the reset layout");
    }

    private static void testZeroMobilePopulationIsPreserved() throws Exception {
        SimulationSettings empty = new SimulationSettings(0, 0, 0);
        World world = populated(empty, new Random(7));
        checkPopulation(world.getStatistics(), empty);
        check(positions(world).isEmpty(), "Zero settings must not add default mobile populations");
    }

    private static World populated(SimulationSettings settings, Random random) throws Exception {
        World world = new World(1000, 750);
        populate(world, settings, random);
        return world;
    }

    private static void populate(World world, SimulationSettings settings, Random random) throws Exception {
        Method populate = Main.class.getDeclaredMethod(
                "populateWorld", World.class, SimulationSettings.class, Random.class);
        populate.setAccessible(true);
        populate.invoke(null, world, settings, random);
    }

    private static List<String> positions(World world) {
        List<String> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Human || entity instanceof Zombie) {
                result.add(entity.getClass().getSimpleName() + ":" + entity.getX() + ":" + entity.getY());
            }
        }
        return result;
    }

    private static void checkPopulation(WorldStatistics statistics, SimulationSettings settings) {
        check(statistics.getTick() == 0 && statistics.getHumanCount() == settings.getHumanCount()
                        && statistics.getMilitaryCount() == settings.getMilitaryCount()
                        && statistics.getZombieCount() == settings.getZombieCount()
                        && statistics.getFoodCount() == 3,
                "Setup/reset should publish the complete selected population at tick zero");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedRandom extends Random {
        private final double value;

        private FixedRandom(double value) {
            super(0);
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}
