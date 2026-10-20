import java.awt.Graphics2D;
import java.util.List;

public class WorldTest {

    public static void main(String[] args) {
        testWorldStoresEntities();
        testNearbyEntities();
        testInactiveEntitiesAreRemoved();
        testWorldUpdateAdvancesTick();
        testInvalidValuesAreRejected();
        testNullValuesAreRejected();

        System.out.println("World tests passed");
    }

    private static void testWorldStoresEntities() {
        World world = new World(100, 100);
        TestEntity entity = new TestEntity(10, 10);

        world.addEntity(entity);

        check(world.getEntities().size() == 1,
                "World should store added entities");
        check(world.getEntities().contains(entity),
                "World should return the added entity");
    }

    private static void testNearbyEntities() {
        World world = new World(100, 100);
        TestEntity source = new TestEntity(10, 10);
        TestEntity nearby = new TestEntity(13, 14);
        TestEntity farAway = new TestEntity(80, 80);
        TestEntity inactive = new TestEntity(12, 12);
        inactive.deactivate();

        world.addEntity(source);
        world.addEntity(nearby);
        world.addEntity(farAway);
        world.addEntity(inactive);

        List<Entity> result = world.getNearby(source, 6);

        check(result.contains(nearby),
                "Nearby active entities should be returned");
        check(!result.contains(source),
                "The source entity should not be returned");
        check(!result.contains(farAway),
                "Far-away entities should not be returned");
        check(!result.contains(inactive),
                "Inactive entities should not be returned");
    }

    private static void testInactiveEntitiesAreRemoved() {
        World world = new World(100, 100);
        TestEntity active = new TestEntity(10, 10);
        TestEntity inactive = new TestEntity(20, 20);
        inactive.deactivate();

        world.addEntity(active);
        world.addEntity(inactive);
        world.removeInactiveEntities();

        check(world.getEntities().size() == 1,
                "Inactive entities should be removed");
        check(world.getEntities().contains(active),
                "Active entities should remain");
    }

    private static void testWorldUpdateAdvancesTick() {
        World world = new World(100, 100);
        TestEntity entity = new TestEntity(10, 10);
        world.addEntity(entity);

        world.update();

        check(world.getTick() == 1,
                "World update should advance the tick");
        check(entity.updateCount == 1,
                "World update should update active entities");
    }

    private static void testInvalidValuesAreRejected() {
        expectException(() -> new World(0, 100),
                "World should reject invalid dimensions");

        World world = new World(100, 100);
        TestEntity entity = new TestEntity(10, 10);

        expectException(() -> world.getNearby(entity, -1),
                "World should reject a negative radius");
    }

    private static void testNullValuesAreRejected() {
        World world = new World(100, 100);
        TestEntity entity = new TestEntity(10, 10);

        expectNullException(() -> world.addEntity(null),
                "World should reject a null entity");
        expectNullException(() -> world.getNearby(null, 10),
                "World should reject a null source entity");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectException(
            Runnable action,
            String message
    ) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected result.
        }
    }

    private static void expectNullException(
            Runnable action,
            String message
    ) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (NullPointerException expected) {
            // Expected result.
        }
    }

    private static class TestEntity extends Entity {
        private int updateCount;

        private TestEntity(double x, double y) {
            super(x, y);
        }

        @Override
        public void update(World world) {
            updateCount++;
        }

        @Override
        public void draw(Graphics2D graphics) {
            // Drawing is not needed for World tests.
        }
    }
}
