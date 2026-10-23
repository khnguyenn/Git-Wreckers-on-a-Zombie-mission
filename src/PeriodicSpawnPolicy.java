import java.util.Objects;
import java.util.Random;

/** The existing food-every-25 and human-every-100 arrival schedule. */
public final class PeriodicSpawnPolicy implements SpawnPolicy {
    private static final int FOOD_SPAWN_INTERVAL_TICKS = 25;
    private static final int HUMAN_SPAWN_INTERVAL_TICKS = 100;
    private static final double FOOD_ENERGY_VALUE = 45.0;

    private final Random random;

    public PeriodicSpawnPolicy() {
        this(new Random());
    }

    public PeriodicSpawnPolicy(Random random) {
        this.random = Objects.requireNonNull(random, "Random source cannot be null");
    }

    @Override
    public void spawn(World world, long completedTick) {
        if (completedTick % FOOD_SPAWN_INTERVAL_TICKS == 0) {
            world.addEntity(new Food(random.nextDouble() * world.getWidth(),
                    random.nextDouble() * world.getHeight(), FOOD_ENERGY_VALUE));
        }

        if (completedTick % HUMAN_SPAWN_INTERVAL_TICKS == 0) {
            world.addEntity(new Human(random.nextDouble() * world.getWidth(),
                    random.nextDouble() * world.getHeight()));
        }
    }
}
