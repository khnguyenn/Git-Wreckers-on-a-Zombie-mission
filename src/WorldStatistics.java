import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collector;

/** Immutable population and civilian health values captured at one simulation tick. */
public final class WorldStatistics {
    private final long tick;
    private final long humanCount;
    private final long militaryCount;
    private final long zombieCount;
    private final long foodCount;
    private final OptionalDouble averageCivilianEnergy;
    private final OptionalDouble lowestCivilianEnergy;
    private final long shelteredCivilianCount;

    private WorldStatistics(long tick, Map<Class<?>, Long> counts,
                            OptionalDouble averageCivilianEnergy,
                            OptionalDouble lowestCivilianEnergy, long shelteredCivilianCount) {
        this.tick = tick;
        this.humanCount = counts.getOrDefault(Human.class, 0L);
        this.militaryCount = counts.getOrDefault(Military.class, 0L);
        this.zombieCount = counts.getOrDefault(Zombie.class, 0L);
        this.foodCount = counts.getOrDefault(Food.class, 0L);
        this.averageCivilianEnergy = averageCivilianEnergy;
        this.lowestCivilianEnergy = lowestCivilianEnergy;
        this.shelteredCivilianCount = shelteredCivilianCount;
    }

    static WorldStatistics capture(long tick, List<Entity> entities) {
        // Entity state belongs to the simulation thread; keep live collection sequential.
        return entities.stream().collect(collector(tick));
    }

    static Collector<Entity, ?, WorldStatistics> collector(long tick) {
        return Collector.of(Accumulator::new, Accumulator::add, Accumulator::combine,
                accumulator -> accumulator.finish(tick));
    }

    private static final class Accumulator {
        private final Map<Class<?>, Long> counts = new HashMap<>();
        private final DoubleSummaryStatistics energy = new DoubleSummaryStatistics();
        private long sheltered;

        private void add(Entity entity) {
            if (!entity.isActive()) {
                return;
            }
            counts.merge(entity.getClass(), 1L, Long::sum);
            // Military extends Human, but does not use civilian hunger/shelter behaviour.
            if (entity.getClass() == Human.class) {
                Human civilian = (Human) entity;
                energy.accept(civilian.getEnergy());
                if (civilian.isHidden()) {
                    sheltered++;
                }
            }
        }

        private Accumulator combine(Accumulator other) {
            other.counts.forEach((type, count) -> counts.merge(type, count, Long::sum));
            energy.combine(other.energy);
            sheltered += other.sheltered;
            return this;
        }

        private WorldStatistics finish(long tick) {
            boolean empty = energy.getCount() == 0;
            return new WorldStatistics(tick, counts,
                    empty ? OptionalDouble.empty() : OptionalDouble.of(energy.getAverage()),
                    empty ? OptionalDouble.empty() : OptionalDouble.of(energy.getMin()), sheltered);
        }
    }

    public long getTick() {
        return tick;
    }

    public long getHumanCount() {
        return humanCount;
    }

    public long getMilitaryCount() {
        return militaryCount;
    }

    public long getZombieCount() {
        return zombieCount;
    }

    public long getFoodCount() {
        return foodCount;
    }

    /** Empty when no active civilians remain. */
    public OptionalDouble getAverageCivilianEnergy() {
        return averageCivilianEnergy;
    }

    /** Empty when no active civilians remain. */
    public OptionalDouble getLowestCivilianEnergy() {
        return lowestCivilianEnergy;
    }

    public long getShelteredCivilianCount() {
        return shelteredCivilianCount;
    }
}
