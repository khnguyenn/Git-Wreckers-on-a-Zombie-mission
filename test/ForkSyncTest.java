import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Compatibility checks for combining team changes with the fork's features. */
public class ForkSyncTest {

    public static void main(String[] args) throws Exception {
        boolean legacyImages = args.length > 0 && args[0].equals("--legacy-images");
        testZombieRendering(legacyImages);
        testZombieTargetingAndInfection();
        SwingUtilities.invokeAndWait(() -> {
            try {
                testSettingsAndControlsSurviveReset();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("Fork sync compatibility checks passed"
                + (legacyImages ? " with legacy zombie images" : " with the team sprite sheet"));
    }

    private static void testZombieRendering(boolean legacyImages) throws Exception {
        BufferedImage source = ImageIO.read(new File("src/assets/"
                + (legacyImages ? "ZombieFrontIdle.png" : "ZombieSprites.png")));
        BufferedImage frame = legacyImages ? source : source.getSubimage(
                0, 0, source.getWidth() / 5, source.getHeight() / 2);
        BufferedImage expected = new BufferedImage(80, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D expectedGraphics = expected.createGraphics();
        expectedGraphics.drawImage(frame, 21, 23, 38, 54, null);
        expectedGraphics.dispose();

        BufferedImage actual = new BufferedImage(80, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = actual.createGraphics();
        Zombie zombie = new Zombie(40, 50);
        zombie.draw(graphics);
        graphics.dispose();
        check(Arrays.equals(expected.getRGB(0, 0, 80, 100, null, 0, 80),
                        actual.getRGB(0, 0, 80, 100, null, 0, 80)),
                "Zombie should render the available sprite source");

        zombie.deactivate();
        BufferedImage inactive = new BufferedImage(80, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D inactiveGraphics = inactive.createGraphics();
        zombie.draw(inactiveGraphics);
        inactiveGraphics.dispose();
        check(Arrays.stream(inactive.getRGB(0, 0, 80, 100, null, 0, 80))
                        .allMatch(pixel -> pixel == 0),
                "Inactive zombies should remain invisible");
    }

    private static void testZombieTargetingAndInfection() {
        World world = new World(1000, 750);
        Zombie zombie = new Zombie(100, 100);
        Human boundaryTarget = new Human(400, 100);
        Human hidden = new Human(100, 100);
        hidden.enterBuilding(new Building(100, 100, 40, 40));
        Human inactive = new Human(100, 100);
        inactive.deactivate();
        world.addEntity(zombie);
        world.addEntity(hidden);
        world.addEntity(inactive);
        world.addEntity(new Military(100, 100));
        world.addEntity(boundaryTarget);
        check(zombie.findTarget(world) == boundaryTarget,
                "Zombie should find the visible civilian at the detection boundary");
        zombie.infect(hidden);
        check(hidden.isActive(), "Sheltered humans should remain protected");
        zombie.infect(boundaryTarget);
        check(!boundaryTarget.isActive(), "Infection should deactivate its victim");
        check(zombie.findTarget(world) == null,
                "Soldiers, inactive and sheltered humans should not be targets");
    }

    private static void testSettingsAndControlsSurviveReset() throws Exception {
        World world = new World(1000, 750);
        SimulationSettings settings = new SimulationSettings(4, 3, 2);
        Method populate = Main.class.getDeclaredMethod(
                "populateWorld", World.class, SimulationSettings.class);
        populate.setAccessible(true);
        populate.invoke(null, world, settings);
        checkPopulation(world);
        SimPanel panel = new SimPanel(world, () -> {
            world.reset();
            try {
                populate.invoke(null, world, settings);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        check(!panel.isRunning(), "Simulation should wait for start");
        press(panel, "start");
        check(panel.isRunning(), "Start should run the simulation");
        press(panel, "pause");
        check(!panel.isRunning(), "Pause should stop simulation updates");
        press(panel, "pause");
        check(panel.isRunning(), "Pause again should resume");
        for (int index = 0; index < 10; index++) {
            press(panel, "increase-speed");
        }
        check(panel.getTimerDelay() == 40, "Maximum speed should remain bounded");
        for (int index = 0; index < 10; index++) {
            press(panel, "decrease-speed");
        }
        check(panel.getTimerDelay() == 100, "Minimum speed should remain bounded");
        world.update();
        press(panel, "start");
        check(world.getTick() == 0, "Reset should restart the tick counter");
        check(panel.isRunning(), "Reset should leave the simulation running");
        check(panel.getTimerDelay() == 70, "Reset should restore the default speed");
        checkPopulation(world);
    }

    private static void checkPopulation(World world) {
        check(world.getEntities().stream().filter(entity -> entity.getClass() == Human.class)
                .count() == 4, "Selected civilian population should be retained");
        check(world.getEntities().stream().filter(entity -> entity instanceof Military)
                .count() == 3, "Selected military population should be retained");
        check(world.getEntities().stream().filter(entity -> entity instanceof Zombie)
                .count() == 2, "Selected zombie population should be retained");
        check(world.getEntities().stream().filter(entity -> entity instanceof Building)
                .count() == 2, "Both buildings should be retained");
        check(world.getEntities().stream().filter(entity -> entity instanceof Food)
                .count() == 3, "Initial food should be retained");
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Human || entity instanceof Zombie) {
                check(entity.getX() >= 80 && entity.getX() < world.getWidth() - 80
                                && entity.getY() >= 80 && entity.getY() < world.getHeight() - 80,
                        "Spawn positions should remain within the fork's safe margins");
            }
        }
    }

    private static void press(SimPanel panel, String action) {
        panel.getActionMap().get(action).actionPerformed(new ActionEvent(
                panel, ActionEvent.ACTION_PERFORMED, action));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
