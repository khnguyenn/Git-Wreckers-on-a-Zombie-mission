import java.util.Objects;
import java.util.Optional;

/** Idle state: follows the unit's own wandering or caller-supplied patrol strategy. */
public final class PatrolState implements MilitaryState {
    private final MovementBehaviour movement;

    public PatrolState(MovementBehaviour movement) {
        this.movement = Objects.requireNonNull(movement, "Movement cannot be null");
    }

    @Override
    public String name() {
        return movement.describe();
    }

    @Override
    public void update(Military unit, World world) {
        Optional<Zombie> threat = unit.findThreat(world);
        if (threat.isPresent()) {
            unit.lockTarget(threat.get());
            unit.transitionTo(unit.engageState());
            // Preserve the fork's immediate response on the detection tick.
            unit.engageState().update(unit, world);
            return;
        }
        movement.move(unit, world);
    }
}
