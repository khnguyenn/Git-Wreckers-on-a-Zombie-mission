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
import java.util.concurrent.ThreadLocalRandom;


public class Main {

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
        populateWorld(world, settings);

        SimPanel simPanel = new SimPanel(world, () -> {
            world.reset();
            populateWorld(world, settings);
        });

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

        Timer timer = new Timer(100, null);
        timer.addActionListener(event -> {
            if (timer.getDelay() != simPanel.getTimerDelay()) {
                timer.setDelay(simPanel.getTimerDelay());
            }
            if (simPanel.isRunning()) {
                world.update();
                simPanel.repaint();
            }
        });

        timer.start();
    }

    private static void populateWorld(
            World world,
            SimulationSettings settings
    ) {
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Part B: people wander until they find and consume nearby food.
        for (int index = 0; index < settings.getHumanCount(); index++) {
            world.addEntity(new Human(
                    getSpawnCoordinate(index, 137, world.getWidth()),
                    getSpawnCoordinate(index, 83, world.getHeight())
            ));
        }

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        for (int index = 0; index < settings.getMilitaryCount(); index++) {
            world.addEntity(new Military(
                    getRandomSpawnCoordinate(world.getWidth()),
                    getRandomSpawnCoordinate(world.getHeight())
            ));
        }

        for (int index = 0; index < settings.getZombieCount(); index++) {
            world.addEntity(new Zombie(
                    getSpawnCoordinate(index, 173, world.getWidth()),
                    getSpawnCoordinate(index, 149, world.getHeight())
            ));
        }
    }

    private static double getSpawnCoordinate(
            int index,
            int step,
            int dimension
    ) {
        return 80 + (index * step) % (dimension - 160);
    }

    private static double getRandomSpawnCoordinate(int dimension) {
        return ThreadLocalRandom.current().nextDouble(80, dimension - 80);
    }
}
