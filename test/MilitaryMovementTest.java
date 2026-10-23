import java.awt.geom.Point2D;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Random;

public final class MilitaryMovementTest {
    public static void main(String[] args) throws Exception {
        testDefaultUnitsWander();
        testIndependentDirectionsAndDirectionChanges();
        testWanderingReflectsAtWorldEdges();
        testHuntingAndResumingWandering();
        testShootingCooldownAndInactiveUnits();
        testExplicitPatrolRoutesRemainAvailable();
        testInvalidMovementInputsAreRejected();
        System.out.println("MilitaryMovementTest passed");
    }

    private static void testDefaultUnitsWander() {
        Military soldier = new Military(300, 300);
        check(soldier.getBehaviourName().equals("Wandering"),
                "The default constructor must not put every soldier on the same phased box route");
    }

    private static void testIndependentDirectionsAndDirectionChanges() throws Exception {
        World world = closedWorld();
        Military east = wandering(300, 300, new ControlledRandom(0.0, 0.25));
        Military west = wandering(300, 300, new ControlledRandom(0.5));
        Military north = wandering(300, 300, new ControlledRandom(0.75));
        world.addEntity(east);
        world.addEntity(west);
        world.addEntity(north);
        world.update();
        check(east.getX() > 300 && west.getX() < 300 && north.getY() < 300,
                "Each idle soldier should follow its own selected direction");
        check(close(east.getY(), 300) && close(west.getY(), 300) && close(north.getX(), 300),
                "Controlled directions should produce distinct, straight initial headings");
        for (int tick = 1; tick < 20; tick++) world.update();
        double x = east.getX();
        double y = east.getY();
        world.update();
        check(close(east.getX(), x) && east.getY() > y,
                "After its own wander interval, a soldier should choose a new heading");
        check(west.getX() < 300 && north.getY() < 300,
                "One soldier changing direction must not change the others' wander state");
    }

    private static void testWanderingReflectsAtWorldEdges() throws Exception {
        Military[] soldiers = {
                wandering(19, 300, new ControlledRandom(0.5)),
                wandering(981, 300, new ControlledRandom(0.0)),
                wandering(300, 27, new ControlledRandom(0.75)),
                wandering(300, 723, new ControlledRandom(0.25))
        };
        World world = closedWorld();
        for (Military soldier : soldiers) world.addEntity(soldier);
        world.update();
        world.update();
        check(soldiers[0].getX() > 19 && soldiers[1].getX() < 981
                        && soldiers[2].getY() > 27 && soldiers[3].getY() < 723,
                "An outward wander direction at a boundary should turn back into the world");
        for (int tick = 0; tick < 120; tick++) {
            world.update();
            for (Military soldier : soldiers) {
                check(soldier.getX() >= 19 && soldier.getX() <= 981
                                && soldier.getY() >= 27 && soldier.getY() <= 723,
                        "Wandering should preserve military sprite clearance at every world edge");
            }
        }
    }

    private static void testHuntingAndResumingWandering() throws Exception {
        World world = closedWorld();
        Military soldier = wandering(300, 300, new ControlledRandom(0));
        world.addEntity(soldier);
        soldier.update(world);
        Zombie target = new Zombie(soldier.getX(), 500);
        world.addEntity(target);
        double x = soldier.getX();
        double y = soldier.getY();
        soldier.update(world);
        check(soldier.getTarget() == target && soldier.getBehaviourName().equals("Engaging")
                        && close(soldier.getX(), x) && soldier.getY() > y,
                "A detected zombie should still override wandering with pursuit");
        target.deactivate();
        world.removeInactiveEntities();
        x = soldier.getX();
        y = soldier.getY();
        soldier.update(world);
        check(soldier.getTarget() == null && soldier.getBehaviourName().equals("Wandering")
                        && soldier.getX() > x && close(soldier.getY(), y),
                "Losing the target should resume that soldier's own wander state");
    }

    private static void testShootingCooldownAndInactiveUnits() throws Exception {
        World world = closedWorld();
        Military soldier = wandering(300, 300, new ControlledRandom(0));
        Zombie first = new Zombie(350, 300);
        world.addEntity(soldier);
        world.addEntity(first);
        soldier.update(world);
        check(!first.isActive() && soldier.getKills() == 1,
                "A wandering soldier must still shoot an in-range zombie");
        Zombie second = new Zombie(350, 340);
        world.addEntity(second);
        for (int tick = 0; tick < 11; tick++) soldier.update(world);
        check(second.isActive(), "The existing firing cooldown must remain in effect");
        soldier.update(world);
        check(!second.isActive() && soldier.getKills() == 2,
                "Shooting should resume at the existing 12-tick cooldown boundary");
        double x = soldier.getX();
        double y = soldier.getY();
        soldier.deactivate();
        Zombie survivor = new Zombie(x + 10, y);
        world.addEntity(survivor);
        soldier.update(world);
        check(close(soldier.getX(), x) && close(soldier.getY(), y) && survivor.isActive(),
                "Inactive soldiers must neither move nor shoot");
    }

    private static void testExplicitPatrolRoutesRemainAvailable() {
        World world = closedWorld();
        Military patrol = new Military(300, 300,
                List.of(new Point2D.Double(320, 300), new Point2D.Double(320, 320)));
        patrol.update(world);
        check(patrol.getBehaviourName().equals("Patrolling") && patrol.getX() > 300
                        && close(patrol.getY(), 300),
                "A caller-supplied patrol must still start with its first waypoint");
        for (int tick = 0; tick < 12; tick++) patrol.update(world);
        check(patrol.getY() > 300, "An explicit patrol should still advance through its route");
    }

    private static Military wandering(double x, double y, Random random) throws Exception {
        Constructor<Military> constructor = Military.class.getDeclaredConstructor(
                double.class, double.class, MovementBehaviour.class);
        constructor.setAccessible(true);
        return constructor.newInstance(x, y, new WanderBehaviour(random));
    }

    private static void testInvalidMovementInputsAreRejected() {
        try {
            new Military(300, 300, null);
            throw new AssertionError("Null patrol routes should remain invalid");
        } catch (IllegalArgumentException expected) { }
        try {
            new Military(300, 300, List.of());
            throw new AssertionError("Empty patrol routes should remain invalid");
        } catch (IllegalArgumentException expected) { }
        try {
            new WanderBehaviour(null);
            throw new AssertionError("Null random sources should be rejected");
        } catch (NullPointerException expected) { }
    }

    private static World closedWorld() {
        return new World(1000, 750, (world, tick) -> { });
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1e-8;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ControlledRandom extends Random {
        private final double[] headings;
        private int next;

        private ControlledRandom(double... headings) { this.headings = headings; }
        @Override public double nextDouble() { return headings[Math.min(next++, headings.length - 1)]; }
        @Override public int nextInt(int bound) { return 0; }
    }
}
