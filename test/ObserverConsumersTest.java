import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

public final class ObserverConsumersTest {

    public static void main(String[] args) throws Exception {
        testHistoryIsBoundedAndReadOnly();
        testInvalidHistoryArguments();
        SwingUtilities.invokeAndWait(() -> {
            testPaintingUsesCachedStatistics();
            try {
                testMainWiresStartupTicksAndReset();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("ObserverConsumersTest passed");
    }

    private static void testPaintingUsesCachedStatistics() {
        World world = new World(1000, 750);
        SimPanel panel = new SimPanel(world);
        panel.setSize(1000, 750);
        press(panel, "start");
        int[] before = paintCounts(panel);
        world.addEntity(new Human(500, 500));
        check(Arrays.equals(before, paintCounts(panel)),
                "Painting should read cached counts until a new snapshot is delivered");
        panel.setStatistics(world.getStatistics());
        int[] delivered = paintCounts(panel);
        check(!Arrays.equals(before, delivered), "Delivered population changes should reach the HUD");
        world.reset();
        world.update();
        check(Arrays.equals(delivered, paintCounts(panel)),
                "The displayed tick should belong to the cached snapshot too");
        expectException(NullPointerException.class, () -> panel.setStatistics(null));
        check(Arrays.equals(delivered, paintCounts(panel)),
                "Rejecting a null snapshot should preserve the current display");
    }

    private static void testHistoryIsBoundedAndReadOnly() {
        PopulationHistory history = new PopulationHistory(3);
        World world = new World(1000, 750);
        world.addObserver(history);
        world.publishStatistics();
        for (int tick = 0; tick < 4; tick++) {
            world.update();
        }
        List<WorldStatistics> samples = history.getSamples();
        check(samples.size() == 3 && samples.get(0).getTick() == 2
                        && samples.get(1).getTick() == 3 && samples.get(2).getTick() == 4,
                "History should retain the newest samples in tick order");
        expectException(UnsupportedOperationException.class, samples::clear);
        history.clear();
        check(history.getSamples().isEmpty(), "History should clear between runs");
        check(samples.size() == 3, "Previously returned history should remain a stable snapshot");
        world.update();
        check(history.getSamples().size() == 1 && history.getSamples().get(0).getTick() == 5,
                "Clearing history should keep its subscription active");
    }

    private static void testInvalidHistoryArguments() {
        expectException(IllegalArgumentException.class, () -> new PopulationHistory(0));
        expectException(IllegalArgumentException.class, () -> new PopulationHistory(-1));
        PopulationHistory history = new PopulationHistory(1);
        history.onWorldChanged(new World(1000, 750).getStatistics());
        expectException(NullPointerException.class, () -> history.onWorldChanged(null));
        check(history.getSamples().size() == 1,
                "Rejecting an invalid sample must not discard retained history");
    }

    private static void testMainWiresStartupTicksAndReset() throws Exception {
        World world = new World(1000, 750);
        PopulationHistory history = new PopulationHistory(500);
        SimulationSettings settings = new SimulationSettings(4, 3, 2);
        Method configure = Main.class.getDeclaredMethod("createSimulationPanel",
                World.class, SimulationSettings.class, PopulationHistory.class);
        configure.setAccessible(true);
        SimPanel panel = (SimPanel) configure.invoke(null, world, settings, history);
        panel.setSize(1000, 750);
        checkInitialSample(history);
        check(!panel.isRunning(), "Setup should preserve the start screen");
        press(panel, "start");
        check(history.getSamples().size() == 1, "Starting should not repeat the initial sample");
        // Keep entity rendering out of the HUD comparison so only delivered statistics can change it.
        for (Entity entity : world.getEntities()) {
            entity.deactivate();
        }
        world.removeInactiveEntities();
        int[] initialDisplay = paintCounts(panel);
        world.update();
        check(history.getSamples().size() == 2 && history.getSamples().get(1).getTick() == 1,
                "Main should register history for completed tick publications");
        check(!Arrays.equals(initialDisplay, paintCounts(panel)),
                "Main should register the display for completed tick publications");
        press(panel, "pause");
        check(!panel.isRunning(), "Pause should still stop updates");
        for (int repaint = 0; repaint < 3; repaint++) {
            paintCounts(panel);
        }
        check(world.getTick() == 1 && history.getSamples().size() == 2,
                "Paused repainting should not advance ticks or append history");
        press(panel, "pause");
        check(panel.isRunning(), "Pause again should resume");
        press(panel, "increase-speed");
        check(panel.getTimerDelay() == 55, "Existing speed controls should remain available");
        press(panel, "start");
        checkInitialSample(history);
        check(panel.isRunning() && panel.getTimerDelay() == 70,
                "Reset should resume at the default speed");
        world.update();
        check(history.getSamples().size() == 2,
                "Reset must not duplicate observer registrations");
        press(panel, "start");
        checkInitialSample(history);
    }

    private static void checkInitialSample(PopulationHistory history) {
        check(history.getSamples().size() == 1, "Setup/reset should publish exactly one initial sample");
        WorldStatistics initial = history.getSamples().get(0);
        check(initial.getTick() == 0 && initial.getHumanCount() == 4
                        && initial.getMilitaryCount() == 3 && initial.getZombieCount() == 2
                        && initial.getFoodCount() == 3,
                "Initial samples should contain the complete selected population");
    }

    private static void expectException(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Unexpected exception type", exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static int[] paintCounts(SimPanel panel) {
        BufferedImage image = new BufferedImage(1000, 750, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        return image.getRGB(15, 15, 220, 116, null, 0, 220);
    }

    private static void press(SimPanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(
                new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, action));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
