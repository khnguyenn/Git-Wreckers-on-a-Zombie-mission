import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class Main {

    private static final int POPULATION_HISTORY_CAPACITY = 500;
    private static final double AGENT_SPAWN_MARGIN = 80;
    // Reserve the default patrol's 160/110-unit excursions and sprite clearance.
    private static final double MILITARY_SPAWN_MARGIN_X = 180;
    private static final double MILITARY_SPAWN_MARGIN_Y = 140;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                SimulationSettings settings = showSettingsDialog();
                if (settings != null) {
                    startSimulation(settings);
                }
            } catch (IllegalStateException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "The simulation could not start:\n"
                                + exception.getMessage(),
                        "Simulation startup error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private static SimulationSettings showSettingsDialog() {
        JSpinner humanSpinner = createCountSpinner(3);
        JSpinner militarySpinner = createCountSpinner(2);
        JSpinner zombieSpinner = createCountSpinner(1);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        addSetting(panel, "Humans:", humanSpinner);
        addSetting(panel, "Military:", militarySpinner);
        addSetting(panel, "Zombies:", zombieSpinner);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Simulation settings",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null;
            }

            try {
                return new SimulationSettings(
                        (int) humanSpinner.getValue(),
                        (int) militarySpinner.getValue(),
                        (int) zombieSpinner.getValue()
                );
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        exception.getMessage(),
                        "Invalid simulation settings",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private static JSpinner createCountSpinner(int initialValue) {
        return new JSpinner(new SpinnerNumberModel(initialValue, 0, 100, 1));
    }

    private static void addSetting(
            JPanel panel,
            String label,
            JComponent input
    ) {
        panel.add(new JLabel(label));
        panel.add(input);
    }

    private static void startSimulation(SimulationSettings settings) {
        World world = new World(1000,750);
        PopulationHistory history = new PopulationHistory(POPULATION_HISTORY_CAPACITY);
        SimPanel simPanel = createSimulationPanel(world, settings, history);

        JFrame frame =
                new JFrame("Zombie Simulation");

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        frame.add(simPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Keep game updates at the existing speed.
        Timer updateTimer = new Timer(simPanel.getTimerDelay(), null);
        updateTimer.addActionListener(event -> {
            int delay = simPanel.getTimerDelay();
            if (updateTimer.getDelay() != delay) {
                updateTimer.setDelay(delay);
            }

            if (simPanel.isRunning()) {
                world.update();
            }
        });

        // Draw independently at approximately 60 FPS.
        Timer renderTimer = new Timer(17, event -> simPanel.repaint());

        updateTimer.start();
        renderTimer.start();
    }

    private static SimPanel createSimulationPanel(
            World world,
            SimulationSettings settings,
            PopulationHistory history
    ) {
        SimPanel panel = new SimPanel(world, () -> {
            history.clear();
            world.reset();
            populateWorld(world, settings);
        });
        world.addObserver(panel::setStatistics);
        world.addObserver(history);
        populateWorld(world, settings);
        return panel;
    }

    private static void populateWorld(
            World world,
            SimulationSettings settings
    ) {
        populateWorld(world, settings, ThreadLocalRandom.current());
    }

    private static void populateWorld(
            World world,
            SimulationSettings settings,
            Random random
    ) {
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Part B: people wander until they find and consume nearby food.
        for (int index = 0; index < settings.getHumanCount(); index++) {
            world.addEntity(new Human(
                    getRandomSpawnCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    getRandomSpawnCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)
            ));
        }

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        for (int index = 0; index < settings.getMilitaryCount(); index++) {
            world.addEntity(new Military(
                    getRandomSpawnCoordinate(random, world.getWidth(), MILITARY_SPAWN_MARGIN_X),
                    getRandomSpawnCoordinate(random, world.getHeight(), MILITARY_SPAWN_MARGIN_Y)
            ));
        }

        for (int index = 0; index < settings.getZombieCount(); index++) {
            world.addEntity(new Zombie(
                    getRandomSpawnCoordinate(random, world.getWidth(), AGENT_SPAWN_MARGIN),
                    getRandomSpawnCoordinate(random, world.getHeight(), AGENT_SPAWN_MARGIN)
            ));
        }
        world.publishStatistics();
    }

    private static double getRandomSpawnCoordinate(Random random, int dimension, double margin) {
        return margin + random.nextDouble() * (dimension - 2 * margin);
    }
}
