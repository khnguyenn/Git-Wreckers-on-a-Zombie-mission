import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Runs a batch of feeding attempts concurrently on an otherwise idle world. */
public final class ParallelFeeding {
    private ParallelFeeding() { }

    /**
     * Each human must occur once. The caller must keep the entities out of all
     * other updates, rendering and feeding batches until this batch returns.
     */
    public static int feed(List<Human> humans, Food food) {
        Objects.requireNonNull(humans, "humans");
        Objects.requireNonNull(food, "food");
        List<Human> batch = List.copyOf(humans);
        Set<Human> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Human human : batch) {
            if (!distinct.add(Objects.requireNonNull(human, "human"))) {
                throw new IllegalArgumentException("Each human must occur only once");
            }
        }
        return batch.parallelStream().mapToInt(human -> human.eat(food) ? 1 : 0).sum();
    }
}
