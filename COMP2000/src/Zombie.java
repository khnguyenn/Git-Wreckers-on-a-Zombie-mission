import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.Random;
import javax.swing.ImageIcon;

/**
 * An undead entity that chases the nearest visible human and
 * infects them on contact. Zombies cannot enter buildings.
 */
public class Zombie extends Entity {

    private static final double CHASE_SPEED = 1.4;
    private static final double WANDER_SPEED = 0.6;
    private static final double INFECT_DISTANCE = 12.0;
    private static final double DETECTION_RADIUS = 300.0;
    private static final int CHARACTER_WIDTH = 38;
    private static final int CHARACTER_HEIGHT = 54;
    private static final int TICKS_PER_WALK_FRAME = 8;

    private final Random random = new Random();
    private Human target;
    private double directionX;
    private double directionY;
    private int wanderTicksRemaining;
    private final Image[] frontWalkFrames;
    private final Image[] sideWalkFrames;
    private final Image frontIdleFrame;
    private final Image sideIdleFrame;
    private final Image backFrame;
    private int walkFrame;
    private int ticksOnCurrentFrame;
    private boolean facingLeft;
    private Facing facing = Facing.FRONT;
    private boolean moving;

    public Zombie(double x, double y) {
        super(x, y);

        frontWalkFrames = new Image[]{
                loadImage("/ZombieFrontWalk1.png"),
                loadImage("/ZombieFrontWalk2.png"),
                loadImage("/ZombieFrontWalk3.png"),
                loadImage("/ZombieFrontWalk4.png")
        };

        sideWalkFrames = new Image[]{
                loadImage("/ZombieWalk1.png"),
                loadImage("/ZombieWalk2.png"),
                loadImage("/ZombieWalk3.png")
        };

        frontIdleFrame = loadImage("/ZombieFrontIdle.png");
        sideIdleFrame = loadImage("/ZombieSideIdle.png");
        backFrame = loadImage("/ZombieBack.png");
        walkFrame = 0;
        chooseNewWanderDirection();
    }

    /** Returns the human this zombie is currently chasing. */
    public Human getTarget() {
        return target;
    }

    @Override
    public void update(World world) {
        if (!isActive()) {
            return;
        }

        target = findTarget(world);

        if (target == null) {
            wander(world);
            return;
        }

        moveTowards(
                target.getX(),
                target.getY(),
                CHASE_SPEED,
                world
        );

        if (distanceTo(target) <= INFECT_DISTANCE) {
            infect(target);
        }
    }

    /**
     * Locates the nearest visible human within detection range.
     * Hidden humans (sheltering inside buildings) are skipped.
     *
     * @return the closest reachable Human, or null if none
     */
    public Human findTarget(World world) {

        return world.getNearby(
                this, DETECTION_RADIUS
        ).stream()
                .filter(e -> e instanceof Human
                        && !(e instanceof Military))
                .map(e -> (Human) e)
                .filter(h -> h.isActive() && !h.isHidden())
                .min(Comparator.comparingDouble(
                        this::distanceTo))
                .orElse(null);
    }

    /**
     * Infects a single human on contact. The victim is
     * deactivated and the World spawns a new zombie at that
     * position.
     */
    public void infect(Human human) {
        if (human != null
                && human.isActive()
                && !human.isHidden()) {
            human.infect();
        }
    }

    // ---- movement ------------------------------------------------

    private void moveTowards(double targetX, double targetY,
                             double speed, World world) {

        double xDifference = targetX - getX();
        double yDifference = targetY - getY();
        double distance =
                Math.hypot(xDifference, yDifference);

        if (distance > 0) {
            move(
                    xDifference / distance,
                    yDifference / distance,
                    speed,
                    world
            );
        }
    }

    private void wander(World world) {
        if (wanderTicksRemaining-- <= 0) {
            chooseNewWanderDirection();
        }

        move(directionX, directionY, WANDER_SPEED, world);
    }

    private void chooseNewWanderDirection() {
        double angle = random.nextDouble() * Math.PI * 2;
        directionX = Math.cos(angle);
        directionY = Math.sin(angle);
        wanderTicksRemaining = 30 + random.nextInt(40);
    }

    private void move(double xDirection, double yDirection,
                      double speed, World world) {

        moving = Math.abs(xDirection) > 0.01
                || Math.abs(yDirection) > 0.01;

        if (Math.abs(yDirection) > Math.abs(xDirection)) {
            facing = yDirection < 0
                    ? Facing.BACK
                    : Facing.FRONT;
        } else if (Math.abs(xDirection) > 0.01) {
            facing = Facing.SIDE;
            facingLeft = xDirection < 0;
        }

        double nextX = getX() + xDirection * speed;
        double nextY = getY() + yDirection * speed;

        nextX = Math.max(
                CHARACTER_WIDTH / 2.0,
                Math.min(
                        world.getWidth()
                                - CHARACTER_WIDTH / 2.0,
                        nextX
                )
        );

        nextY = Math.max(
                CHARACTER_HEIGHT / 2.0,
                Math.min(
                        world.getHeight()
                                - CHARACTER_HEIGHT / 2.0,
                        nextY
                )
        );

        setPosition(nextX, nextY);
        advanceWalkAnimation();
    }

    // ---- sprites -------------------------------------------------

    private Image loadImage(String imagePath) {
        URL imageUrl =
                Zombie.class.getResource(imagePath);

        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }

        String fileName = imagePath.startsWith("/")
                ? imagePath.substring(1)
                : imagePath;

        File sourceAsset = new File("src", fileName);

        if (!sourceAsset.isFile()) {
            sourceAsset = new File("COMP2000/src", fileName);
        }

        if (!sourceAsset.isFile()) {
            return createFallbackSprite();
        }

        return new ImageIcon(
                sourceAsset.getAbsolutePath()
        ).getImage();
    }

    /** Keeps the simulation runnable when optional Zombie artwork is absent. */
    private Image createFallbackSprite() {
        BufferedImage sprite = new BufferedImage(
                CHARACTER_WIDTH,
                CHARACTER_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = sprite.createGraphics();
        try {
            graphics.setColor(new Color(70, 120, 80));
            graphics.fillOval(8, 2, 22, 22);
            graphics.fillRoundRect(7, 20, 24, 30, 8, 8);
            graphics.setColor(Color.WHITE);
            graphics.fillOval(13, 10, 5, 5);
            graphics.fillOval(21, 10, 5, 5);
            graphics.setColor(new Color(30, 30, 30));
            graphics.fillOval(15, 12, 2, 2);
            graphics.fillOval(23, 12, 2, 2);
            graphics.drawLine(14, 19, 24, 19);
        } finally {
            graphics.dispose();
        }
        return sprite;
    }

    private void advanceWalkAnimation() {
        ticksOnCurrentFrame++;

        if (ticksOnCurrentFrame >= TICKS_PER_WALK_FRAME) {
            walkFrame = (walkFrame + 1) % 12;
            ticksOnCurrentFrame = 0;
        }
    }

    @Override
    public void draw(Graphics2D graphics) {
        if (!isActive()) {
            return;
        }

        Image frame = getCurrentFrame();

        int left = (int) getX() - CHARACTER_WIDTH / 2;

        int top = (int) getY() - CHARACTER_HEIGHT / 2
                - (moving && walkFrame % 2 == 1 ? 2 : 0);

        if (facing == Facing.SIDE && facingLeft) {
            graphics.drawImage(
                    frame,
                    left + CHARACTER_WIDTH,
                    top,
                    -CHARACTER_WIDTH,
                    CHARACTER_HEIGHT,
                    null
            );
        } else {
            graphics.drawImage(
                    frame,
                    left,
                    top,
                    CHARACTER_WIDTH,
                    CHARACTER_HEIGHT,
                    null
            );
        }
    }

    private Image getCurrentFrame() {
        if (!moving) {
            if (facing == Facing.SIDE) {
                return sideIdleFrame;
            }
            return facing == Facing.BACK
                    ? backFrame
                    : frontIdleFrame;
        }

        if (facing == Facing.SIDE) {
            return sideWalkFrames[
                    walkFrame % sideWalkFrames.length
            ];
        }

        if (facing == Facing.BACK) {
            return backFrame;
        }

        return frontWalkFrames[
                walkFrame % frontWalkFrames.length
        ];
    }

    private enum Facing {
        FRONT,
        SIDE,
        BACK
    }
}
