/** Adds arrivals after cleanup and tick advancement, before statistics publication. */
@FunctionalInterface
public interface SpawnPolicy {
    /** Called once per completed tick on the simulation thread; new entities update next tick. */
    void spawn(World world, long completedTick);
}
