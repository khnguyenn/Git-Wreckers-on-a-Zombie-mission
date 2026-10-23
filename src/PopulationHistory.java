import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Retains a bounded sequence of immutable population samples for the current run. */
public final class PopulationHistory implements WorldObserver {
    private final int capacity;
    private final Deque<WorldStatistics> samples = new ArrayDeque<>();

    public PopulationHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("History capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public void onWorldChanged(WorldStatistics statistics) {
        Objects.requireNonNull(statistics, "Statistics cannot be null");
        if (samples.size() == capacity) {
            samples.removeFirst();
        }
        samples.addLast(statistics);
    }

    public List<WorldStatistics> getSamples() {
        return List.copyOf(samples);
    }

    public void clear() {
        samples.clear();
    }
}
