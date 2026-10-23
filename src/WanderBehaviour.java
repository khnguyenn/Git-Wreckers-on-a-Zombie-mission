import java.util.Objects;
import java.util.Random;

/** Independent, short-lived headings like human wandering, using military movement. */
public final class WanderBehaviour implements MovementBehaviour {
    private final Random random;
    private double directionX;
    private double directionY;
    private int ticksRemaining;

    public WanderBehaviour() {
        this(new Random());
    }

    public WanderBehaviour(Random random) {
        this.random = Objects.requireNonNull(random, "Random source cannot be null");
    }

    @Override
    public void move(Military unit, World world) {
        if (ticksRemaining <= 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            directionX = Math.cos(angle);
            directionY = Math.sin(angle);
            ticksRemaining = 20 + random.nextInt(50);
        }
        ticksRemaining--;
        double x = unit.getX();
        double y = unit.getY();
        unit.moveTowards(x + directionX, y + directionY, world);
        // Turn back after clamping instead of walking against the edge for the whole interval.
        if (unit.getX() == x && Math.abs(directionX) > 1e-8) directionX = -directionX;
        if (unit.getY() == y && Math.abs(directionY) > 1e-8) directionY = -directionY;
    }

    @Override
    public String describe() {
        return "Wandering";
    }
}
