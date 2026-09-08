import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            try {
                startSimulation();
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

    private static void startSimulation() {
        World world = new World(1000,750);
        populateWorld(world);

        SimPanel simPanel = new SimPanel(world, () -> {
            world.reset();
            populateWorld(world);
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

    private static void populateWorld(World world) {
        world.addEntity(new Building(155, 125, 150, 100));
        world.addEntity(new Building(580, 490, 150, 100));

        // Part B: people wander until they find and consume nearby food.
        world.addEntity(new Human(100, 400));
        world.addEntity(new Human(450, 200));
        world.addEntity(new Human(800, 350));

        world.addEntity(new Food(250, 300, 45));
        world.addEntity(new Food(500, 600, 45));
        world.addEntity(new Food(850, 150, 45));

        world.addEntity(new Military(500, 375));
        world.addEntity(new Military(200, 620));
        world.addEntity(new Zombie(650, 250));
        world.addEntity(new Zombie(350, 500));
    }
}
