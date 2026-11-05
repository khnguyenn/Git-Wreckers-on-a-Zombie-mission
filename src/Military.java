import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import javax.swing.ImageIcon;

/**
 * A soldier that wanders independently or follows a route and shoots zombies on sight.
 *
 * <p>A military unit extends {@link Human}, but the fork's zombies continue
 * to target civilians only. Soldiers do not starve, flee, or seek shelter. Combat states
 * coordinate movement strategies, ammunition and reloads. Weapon decorators
 * add upgrades earned through kills. Each unit retains its own wandering
 * strategy unless the caller supplies an explicit patrol route.
 */
public class Military extends Human {

    private static final double DETECTION_RADIUS = 260.0;
    private static final double SPEED = 2.4;
    private static final int MUZZLE_FLASH_TICKS = 3;
    private static final int CHARACTER_WIDTH = 38;
    private static final int CHARACTER_HEIGHT = 54;
    private static final int TICKS_PER_WALK_FRAME = 8;

    private static final Map<Integer, UnaryOperator<Weapon>> PROMOTIONS = Map.of(
            Rank.VETERAN.minKills(), ScopeDecorator::new,
            Rank.ELITE.minKills(), ExtendedMagazineDecorator::new);
    private final PatrolState patrolState;
    private final EngageState engageState;
    private MilitaryState state;
    private Weapon weapon = new Rifle();
    private int ammo = weapon.magazineSize();

    private Zombie target;
    private int fireCooldown;
    private int muzzleFlashTicks;
    private int kills;

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

    /** Creates a unit that patrols the supplied route. */
    public Military(double x, double y, List<Point2D> patrolRoute) {
        this(x, y, new PatrolBehaviour(patrolRoute));
    }

    private Military(double x, double y, MovementBehaviour idleBehaviour) {
        super(x, y);
        this.patrolState = new PatrolState(idleBehaviour);
        this.engageState = new EngageState(new HuntBehaviour());
        this.state = patrolState;

        frontWalkFrames = new Image[]{
                loadImage("/assets/MilitaryFrontWalk1.png"),
                loadImage("/assets/MilitaryFrontWalk2.png"),
                loadImage("/assets/MilitaryFrontWalk3.png"),
                loadImage("/assets/MilitaryFrontWalk4.png")
        };
        sideWalkFrames = new Image[]{
                loadImage("/assets/MilitaryWalk1.png"),
                loadImage("/assets/MilitaryWalk2.png"),
                loadImage("/assets/MilitaryWalk3.png")
        };
        frontIdleFrame = loadImage("/assets/MilitaryFrontIdle.png");
        sideIdleFrame = loadImage("/assets/MilitarySideIdle.png");
        backFrame = loadImage("/assets/MilitaryBack.png");
    }

    /** Creates a unit with its own wandering direction and turn interval. */
    public Military(double x, double y) {
        this(x, y, new WanderBehaviour());
    }

    public Zombie getTarget() {
        return target;
    }

    public int getKills() {
        return kills;
    }

    public String getBehaviourName() {
        return state.name();
    }

    public String getStateName() {
        return state.name();
    }

    public int getAmmo() {
        return ammo;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public Rank getRank() {
        return Rank.forKills(kills);
    }

    PatrolState patrolState() {
        return patrolState;
    }

    EngageState engageState() {
        return engageState;
    }

    void transitionTo(MilitaryState next) {
        state = Objects.requireNonNull(next, "State cannot be null");
    }

    void lockTarget(Zombie zombie) {
        target = zombie;
    }

    void clearTarget() {
        target = null;
    }

    boolean needsReload() {
        return ammo * 3 <= weapon.magazineSize();
    }

    void reload() {
        ammo = weapon.magazineSize();
    }

    @Override
    public void update(World world) {
        if (!isActive()) {
            return;
        }

        moving = false;
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (muzzleFlashTicks > 0) {
            muzzleFlashTicks--;
        }

        state.update(this, world);
    }

    /**
     * Fires at the current target when it is alive and inside
     * the current weapon's range. A hit spends one round and deactivates the
     * zombie so {@link World} removes it after the tick.
     *
     * @throws NoTargetException if there is no live zombie in firing range
     */
    public void shoot() throws NoTargetException {
        if (target == null
                || !target.isActive()
                || distanceTo(target) > weapon.range()) {
            throw new NoTargetException(
                    "No zombie within firing range of the military unit at ("
                            + (int) getX() + ", " + (int) getY() + ")");
        }

        faceDirection(target.getX() - getX(), target.getY() - getY());
        if (fireCooldown > 0 || ammo == 0) {
            return;
        }

        target.deactivate();
        ammo--;
        kills++;
        fireCooldown = weapon.fireIntervalTicks();
        muzzleFlashTicks = MUZZLE_FLASH_TICKS;
        Optional.ofNullable(PROMOTIONS.get(kills))
                .ifPresent(upgrade -> weapon = upgrade.apply(weapon));
    }

    /**
     * Moves one {@link #SPEED} step towards a point, clamped to the world
     * bounds, and updates the facing used for sprite selection. Called by
     * the {@link MovementBehaviour} strategies.
     */
    public void moveTowards(double targetX, double targetY, World world) {
        double xDifference = targetX - getX();
        double yDifference = targetY - getY();
        double distance = Math.hypot(xDifference, yDifference);
        if (distance < 1.0e-6) {
            return;
        }

        double directionX = xDifference / distance;
        double directionY = yDifference / distance;

        faceDirection(directionX, directionY);

        double nextX = getX() + directionX * SPEED;
        double nextY = getY() + directionY * SPEED;
        nextX = Math.max(CHARACTER_WIDTH / 2.0,
                Math.min(world.getWidth() - CHARACTER_WIDTH / 2.0, nextX));
        nextY = Math.max(CHARACTER_HEIGHT / 2.0,
                Math.min(world.getHeight() - CHARACTER_HEIGHT / 2.0, nextY));
        setPosition(nextX, nextY);

        moving = true;
        advanceWalkAnimation();
    }

    private void faceDirection(double directionX, double directionY) {
        if (Math.abs(directionY) > Math.abs(directionX)) {
            facing = directionY < 0 ? Facing.BACK : Facing.FRONT;
        } else if (Math.abs(directionX) > 0.01) {
            facing = Facing.SIDE;
            facingLeft = directionX < 0;
        }
    }

    /** Prefer threats near visible civilians, sharing targets with other active soldiers. */
    public Optional<Zombie> findThreat(World world) {
        List<Entity> nearby = world.getNearby(this, DETECTION_RADIUS);
        List<Human> civilians = nearby.stream()
                .filter(entity -> entity instanceof Human && !(entity instanceof Military))
                .map(entity -> (Human) entity)
                .filter(human -> !human.isHidden())
                .collect(java.util.stream.Collectors.toList());
        Comparator<Zombie> civilianDanger = Comparator.comparingDouble(zombie -> civilians.stream()
                .mapToDouble(zombie::distanceTo).min().orElse(Double.MAX_VALUE));
        return nearby.stream()
                .filter(entity -> entity instanceof Zombie)
                .map(entity -> (Zombie) entity)
                .filter(Entity::isActive)
                .filter(zombie -> !isLeftToCloserSoldier(zombie, world))
                .min(civilianDanger.thenComparingDouble(this::distanceTo));
    }

    private boolean isLeftToCloserSoldier(Zombie zombie, World world) {
        return world.getEntities().stream()
                .filter(entity -> entity instanceof Military && entity != this && entity.isActive())
                .map(entity -> (Military) entity)
                .anyMatch(other -> other.state.isEngaging() && other.target == zombie
                        && other.ammo > 0 && other.distanceTo(zombie) < distanceTo(zombie));
    }

    private void advanceWalkAnimation() {
        ticksOnCurrentFrame++;
        if (ticksOnCurrentFrame >= TICKS_PER_WALK_FRAME) {
            // Twelve is a shared cycle for four front frames and three side frames.
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
            // A negative width mirrors the current frame horizontally.
            graphics.drawImage(frame, left + CHARACTER_WIDTH, top,
                    -CHARACTER_WIDTH, CHARACTER_HEIGHT, null);
        } else {
            graphics.drawImage(frame, left, top,
                    CHARACTER_WIDTH, CHARACTER_HEIGHT, null);
        }

        Graphics2D overlay = (Graphics2D) graphics.create();
        try {
            drawCombatOverlay(overlay);
            overlay.setFont(new Font("Arial", Font.BOLD, 10));
            String status = state.name() + " " + ammo + "/" + weapon.magazineSize()
                    + " " + getRank().title();
            int labelX = (int) getX() - overlay.getFontMetrics().stringWidth(status) / 2;
            java.awt.Rectangle clip = overlay.getClipBounds();
            if (clip != null) {
                labelX = Math.max(clip.x + 2, Math.min(labelX,
                        clip.x + clip.width - overlay.getFontMetrics().stringWidth(status) - 2));
            }
            int labelY = Math.max(overlay.getFontMetrics().getAscent() + 2, top - 4);
            overlay.setColor(Color.BLACK);
            overlay.drawString(status, labelX + 1, labelY + 1);
            overlay.setColor(Color.WHITE);
            overlay.drawString(status, labelX, labelY);
        } finally {
            overlay.dispose();
        }
    }

    /** Draws the firing-range ring while engaging and a tracer on firing. */
    private void drawCombatOverlay(Graphics2D graphics) {
        int centreX = (int) getX();
        int centreY = (int) getY();

        if (state.isEngaging()) {
            int range = (int) weapon.range();
            graphics.setColor(new Color(200, 40, 40, 110));
            graphics.setStroke(new BasicStroke(1.0f));
            graphics.drawOval(
                    centreX - range,
                    centreY - range,
                    range * 2,
                    range * 2);
        }

        if (muzzleFlashTicks > 0 && target != null) {
            graphics.setColor(new Color(255, 220, 90));
            graphics.setStroke(new BasicStroke(2.0f));
            graphics.drawLine(centreX, centreY - CHARACTER_HEIGHT / 4,
                    (int) target.getX(), (int) target.getY());
        }
    }

    private Image getCurrentFrame() {
        if (!moving) {
            if (facing == Facing.SIDE) {
                return sideIdleFrame;
            }
            return facing == Facing.BACK ? backFrame : frontIdleFrame;
        }

        if (facing == Facing.SIDE) {
            return sideWalkFrames[walkFrame % sideWalkFrames.length];
        }
        if (facing == Facing.BACK) {
            return backFrame;
        }
        return frontWalkFrames[walkFrame % frontWalkFrames.length];
    }

    private Image loadImage(String imagePath) {
        URL imageUrl = Military.class.getResource(imagePath);
        if (imageUrl != null) {
            return new ImageIcon(imageUrl).getImage();
        }

        // When Java is run with only the generated out directory on its
        // classpath, image assets remain in src rather than being duplicated.
        String fileName = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
        File sourceAsset = new File("src", fileName);
        if (!sourceAsset.isFile()) {
            sourceAsset = new File("COMP2000/src", fileName);
        }
        if (!sourceAsset.isFile()) {
            throw new IllegalStateException(imagePath + " was not found");
        }
        return new ImageIcon(sourceAsset.getAbsolutePath()).getImage();
    }

    private enum Facing {
        FRONT,
        SIDE,
        BACK
    }
}
