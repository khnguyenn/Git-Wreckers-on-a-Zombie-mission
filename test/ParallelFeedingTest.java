import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ParallelFeedingTest {
    public static void main(String[] args) throws Exception {
        Human first = new Human(100, 100);
        Human second = new Human(100, 100);
        World world = new World(1000, 750, (ignored, tick) -> { });
        first.update(world);
        second.update(world);
        double firstBefore = first.getEnergy();
        double secondBefore = second.getEnergy();
        Food food = new CoordinatedFood();
        int successes = ParallelFeeding.feed(List.of(first, second), food);
        int gained = (first.getEnergy() > firstBefore ? 1 : 0)
                + (second.getEnergy() > secondBefore ? 1 : 0);
        System.out.println("successful attempts=" + successes + ", humans gaining energy=" + gained);
        if (successes != 1 || gained != 1 || food.isActive()) {
            throw new AssertionError("One food item must feed exactly one human");
        }
        System.out.println("ParallelFeedingTest passed");
    }

    /** Holds both workers after their active checks, exposing the unsafe interleaving. */
    private static final class CoordinatedFood extends Food {
        private final CyclicBarrier barrier = new CyclicBarrier(2);
        CoordinatedFood() { super(100, 100, 20); }
        @Override public double getEnergyValue() {
            try {
                barrier.await(1, TimeUnit.SECONDS);
            } catch (TimeoutException | BrokenBarrierException ignored) {
                // In the fixed version only the winning worker reaches this point.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return super.getEnergyValue();
        }
    }
}
