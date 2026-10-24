import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.SwingUtilities;

public final class MainReportingTest {
    public static void main(String[] args) throws Exception {
        check(!Main.isReportRequested(new String[0]), "Default startup must remain quiet");
        check(Main.isReportRequested(new String[]{"--report"}), "The report flag must opt in");
        check(Main.isReportRequested(new String[]{"other", "--report", "--report"}),
                "The flag may occur among other arguments or be repeated");
        check(!Main.isReportRequested(new String[]{"report", "--reports", "--REPORT"}),
                "Only the exact --report flag may enable reporting");
        testFormattingUsesSnapshotMetrics();
        SwingUtilities.invokeAndWait(() -> {
            try {
                testStartupCheckpointsPauseAndReset(true);
                testStartupCheckpointsPauseAndReset(false);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("MainReportingTest passed");
    }

    private static void testFormattingUsesSnapshotMetrics() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            World world = closedWorld();
            Human civilian = new Human(500, 500);
            for (int tick = 0; tick < 50; tick++) civilian.update(world);
            civilian.enterBuilding(new Building(0, 0, 150, 100));
            world.addEntity(civilian);
            world.addEntity(new Military(200, 200));
            world.addEntity(new Zombie(300, 300));
            world.addEntity(new Food(400, 400, 45));
            List<String> lines = new ArrayList<>();
            Main.configureReporting(world, true, lines::add);
            world.publishStatistics();
            check(lines.equals(List.of("tick=0 civilians=1 military=1 zombies=1 food=1 "
                            + "meanEnergy=90.0 lowestEnergy=90.0 sheltered=1")),
                    "Reports must use civilian-only snapshot metrics and stable decimal formatting");
            civilian.infect();
            world.publishStatistics();
            check(lines.get(1).equals("tick=0 civilians=0 military=1 zombies=1 food=1 "
                            + "meanEnergy=n/a lowestEnergy=n/a sheltered=0"),
                    "No active civilians must print absent energy as n/a");
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static void testStartupCheckpointsPauseAndReset(boolean reportRequested) throws Exception {
        World world = closedWorld();
        PopulationHistory history = new PopulationHistory(500);
        List<String> lines = new ArrayList<>();
        Main.configureReporting(world, reportRequested, lines::add);
        Method configure = Main.class.getDeclaredMethod("createSimulationPanel",
                World.class, SimulationSettings.class, PopulationHistory.class);
        configure.setAccessible(true);
        SimPanel panel = (SimPanel) configure.invoke(null, world, new SimulationSettings(4, 3, 2), history);
        panel.setSize(1000, 750);
        checkInitial(lines, history, reportRequested);
        press(panel, "start");
        check(lines.size() == (reportRequested ? 1 : 0), "Starting must not duplicate the initial report");
        for (Entity entity : world.getEntities()) entity.deactivate();
        world.removeInactiveEntities();
        for (int tick = 1; tick <= 50; tick++) {
            world.update();
            check(lines.size() == (reportRequested ? 1 + tick / 25 : 0),
                    "Only completed 25-tick checkpoints may emit a report");
        }
        if (reportRequested) {
            check(lines.get(1).equals("tick=25 civilians=0 military=0 zombies=0 food=0 "
                            + "meanEnergy=n/a lowestEnergy=n/a sheltered=0"),
                    "The checkpoint must describe its delivered completed-tick snapshot");
            check(lines.get(2).startsWith("tick=50 "), "The next checkpoint must be tick 50");
        }
        check(history.getSamples().size() == 51, "Reporting must not add history publications");
        press(panel, "pause");
        int before = lines.size();
        BufferedImage image = new BufferedImage(1000, 750, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        for (int repaint = 0; repaint < 3; repaint++) panel.paint(graphics);
        graphics.dispose();
        check(world.getTick() == 50 && lines.size() == before && history.getSamples().size() == 51,
                "Paused repaints must not emit reports or advance history");
        for (int reset = 0; reset < 2; reset++) {
            lines.clear();
            press(panel, "start");
            checkInitial(lines, history, reportRequested);
            world.update();
            check(lines.size() == (reportRequested ? 1 : 0) && history.getSamples().size() == 2,
                    "Reset must retain one subscription without duplicating the reporter or history");
        }
    }

    private static void checkInitial(List<String> lines, PopulationHistory history, boolean enabled) {
        check(history.getSamples().size() == 1 && history.getSamples().get(0).getTick() == 0,
                "Setup/reset must publish exactly one initial history sample");
        check(lines.equals(enabled ? List.of("tick=0 civilians=4 military=3 zombies=2 food=3 "
                        + "meanEnergy=100.0 lowestEnergy=100.0 sheltered=0") : List.of()),
                "Only opt-in setup/reset should print the complete selected population once");
    }

    private static World closedWorld() {
        return new World(1000, 750, (world, tick) -> { });
    }

    private static void press(SimPanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(
                new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, action));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
