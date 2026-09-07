import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A military unit that patrols, hunts nearby zombies, and shoots them. */
public class Military extends Human {
    private static final double DETECTION_RADIUS = 260.0;
    private static final double FIRING_RANGE = 140.0;
    private static final int FIRE_COOLDOWN_TICKS = 12;

    private final MovementBehaviour patrolBehaviour;
    private final MovementBehaviour huntBehaviour;
    private MovementBehaviour currentBehaviour;
    private Zombie target;
    private int fireCooldown;
    private int killCount;

    public Military(double x, double y) {
        super(x, y);
        patrolBehaviour = new PatrolBehaviour(defaultRoute(x, y));
        huntBehaviour = new HuntBehaviour();
        currentBehaviour = patrolBehaviour;
    }

    private static List<Point2D> defaultRoute(double x, double y) {
        List<Point2D> route = new ArrayList<>();
        route.add(new Point2D.Double(x - 140, y));
        route.add(new Point2D.Double(x, y - 100));
        route.add(new Point2D.Double(x + 140, y));
        route.add(new Point2D.Double(x, y + 100));
        return route;
    }

    public Zombie getTarget() {
        return target;
    }

    public int getKillCount() {
        return killCount;
    }

    public String getBehaviourName() {
        return currentBehaviour.describe();
    }

    @Override
    public void update(World world) {
        if (!isActive()) {
            return;
        }

        target = findNearestZombie(world);
        currentBehaviour = target == null ? patrolBehaviour : huntBehaviour;
        currentBehaviour.move(this, world);

        if (fireCooldown > 0) {
            fireCooldown--;
        }

        try {
            shoot();
        } catch (NoTargetException ignored) {
            // No zombie is in firing range this tick, so the unit continues moving.
        }
    }

    public void shoot() throws NoTargetException {
        if (target == null
                || !target.isActive()
                || distanceTo(target) > FIRING_RANGE) {
            throw new NoTargetException(
                    "No zombie is within firing range"
            );
        }

        if (fireCooldown == 0) {
            target.deactivate();
            killCount++;
            fireCooldown = FIRE_COOLDOWN_TICKS;
        }
    }

    private Zombie findNearestZombie(World world) {
        return world.getNearby(this, DETECTION_RADIUS).stream()
                .filter(entity -> entity instanceof Zombie)
                .map(entity -> (Zombie) entity)
                .filter(Entity::isActive)
                .min(Comparator.comparingDouble(this::distanceTo))
                .orElse(null);
    }
}
