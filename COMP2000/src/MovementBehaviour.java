/**
 * Strategy for how a {@link Military} unit moves during a single simulation
 * tick. Concrete implementations are swapped at runtime by
 * {@link Military#update(World)} depending on the tactical situation, for
 * example patrolling a route versus hunting a nearby zombie.
 */
public interface MovementBehaviour {

    /** Moves the given unit one step for the current tick. */
    void move(Military unit, World world);

    /** Short human-readable name for this behaviour, shown in the HUD. */
    String describe();
}
