import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.SwingUtilities;

public final class SpawnPolicyTest {
    public static void main(String[] args) throws Exception {
        testPolicyRunsAfterCleanupBeforePublication();
        testControlledPeriodicPositionsAndTiming();
        testClosedPopulationAddsNoArrivals();
        testInvalidPolicyArguments();
        SwingUtilities.invokeAndWait(() -> {
            try {
                testMainSelectionAndReset(false);
                testMainSelectionAndReset(true);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("SpawnPolicyTest passed");
    }

    private static void testPolicyRunsAfterCleanupBeforePublication() {
        List<Long> policyTicks = new ArrayList<>();
        CountingEntity child = new CountingEntity();
        World world = new World(1000, 750, (current, completedTick) -> {
            policyTicks.add(completedTick);
            check(current.getTick() == completedTick, "Policy should receive the completed tick");
            check(current.getEntities().stream().allMatch(Entity::isActive),
                    "Inactive entities should be removed before the policy runs");
            if (completedTick == 1) {
                current.addEntity(child);
            }
        });
        CountingEntity removed = new CountingEntity();
        removed.deactivate();
        world.addEntity(removed);
        List<Long> observedTicks = new ArrayList<>();
        world.addObserver(statistics -> {
            check(world.getEntities().contains(child), "Observers should see policy additions");
            observedTicks.add(statistics.getTick());
        });
        check(policyTicks.isEmpty(), "Construction must not invoke the policy at tick zero");
        world.update();
        check(child.updates == 0, "A policy addition must wait until the next tick to update");
        world.update();
        check(child.updates == 1 && policyTicks.equals(List.of(1L, 2L))
                        && observedTicks.equals(policyTicks),
                "Policy and publication should run once per completed tick");
    }

    private static void testControlledPeriodicPositionsAndTiming() {
        for (double value : new double[]{0.0, Math.nextDown(1.0)}) {
            World world = new World(1000, 750, new PeriodicSpawnPolicy(new FixedRandom(value)));
            advance(world, 24);
            check(world.getEntities().isEmpty(), "Periodic mode should wait until tick 25");
            world.update();
            check(world.getStatistics().getFoodCount() == 1, "Periodic mode should add food at tick 25");
            advance(world, 75);
            WorldStatistics statistics = world.getStatistics();
            check(statistics.getFoodCount() == 4 && statistics.getHumanCount() == 1,
                    "Periodic mode should retain food every 25 ticks and humans every 100 ticks");
            for (Entity entity : world.getEntities()) {
                check(entity.getX() >= 0 && entity.getX() < world.getWidth()
                                && entity.getY() >= 0 && entity.getY() < world.getHeight(),
                        "Controlled periodic positions should remain inside the world");
                if (entity instanceof Food) {
                    check(((Food) entity).getEnergyValue() == 45,
                            "Periodic food should retain its existing energy value");
                } else if (entity instanceof Human) {
                    check(((Human) entity).getEnergy() == 100,
                            "Periodic humans must not update during their spawn tick");
                }
            }
        }
    }

    private static void testClosedPopulationAddsNoArrivals() {
        World world = new World(1000, 750, (current, completedTick) -> { });
        CountingEntity initial = new CountingEntity();
        world.addEntity(initial);
        advance(world, 200);
        check(world.getEntities().equals(List.of(initial)) && initial.updates == 200,
                "Closed mode must preserve normal updates while adding no periodic arrivals");
    }

    private static void testMainSelectionAndReset(boolean closedPopulation) throws Exception {
        Method select = Main.class.getDeclaredMethod("createSpawnPolicy", boolean.class);
        select.setAccessible(true);
        SpawnPolicy policy = (SpawnPolicy) select.invoke(null, closedPopulation);
        World world = new World(1000, 750, policy);
        SimulationSettings settings = new SimulationSettings(4, 3, 2);
        PopulationHistory history = new PopulationHistory(500);
        Method configure = Main.class.getDeclaredMethod("createSimulationPanel",
                World.class, SimulationSettings.class, PopulationHistory.class);
        configure.setAccessible(true);
        SimPanel panel = (SimPanel) configure.invoke(null, world, settings, history);
        pressStart(panel);
        for (int run = 0; run < 2; run++) {
            WorldStatistics initial = history.getSamples().get(0);
            check(history.getSamples().size() == 1 && initial.getTick() == 0
                            && initial.getHumanCount() == 4 && initial.getMilitaryCount() == 3
                            && initial.getZombieCount() == 2 && initial.getFoodCount() == 3,
                    "Both modes should retain Main's complete selected initial population");
            // Isolate later arrivals from food consumption, combat and shelter decisions.
            for (Entity entity : world.getEntities()) {
                entity.deactivate();
            }
            world.removeInactiveEntities();
            advance(world, 100);
            WorldStatistics latest = history.getSamples().get(100);
            check(latest.getFoodCount() == (closedPopulation ? 0 : 4)
                            && latest.getHumanCount() == (closedPopulation ? 0 : 1),
                    "The selected arrivals mode must apply before and after reset");
            pressStart(panel);
        }
    }

    private static void testInvalidPolicyArguments() {
        expectNull(() -> new World(1000, 750, null));
        expectNull(() -> new PeriodicSpawnPolicy(null));
    }

    private static void pressStart(SimPanel panel) {
        panel.getActionMap().get("start").actionPerformed(new ActionEvent(panel, 0, "start"));
    }

    private static void advance(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.update();
        }
    }

    private static void expectNull(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("Null policy inputs should be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class CountingEntity extends Entity {
        private int updates;

        private CountingEntity() { super(10, 10); }
        @Override public void update(World world) { updates++; }
        @Override public void draw(Graphics2D graphics) { }
    }

    private static final class FixedRandom extends Random {
        private final double value;
        private FixedRandom(double value) { this.value = value; }
        @Override public double nextDouble() { return value; }
    }
}
