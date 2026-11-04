import java.util.Objects;
import java.util.Optional;

/** Pursues civilian threats, fires in range and reloads before returning to idle movement. */
public final class EngageState implements MilitaryState {
    private final MovementBehaviour movement;

    public EngageState(MovementBehaviour movement) {
        this.movement = Objects.requireNonNull(movement, "Movement cannot be null");
    }

    @Override
    public String name() {
        return "Engaging";
    }

    @Override
    public boolean isEngaging() {
        return true;
    }

    @Override
    public void update(Military unit, World world) {
        Optional<Zombie> threat = unit.findThreat(world);
        if (!threat.isPresent()) {
            unit.clearTarget();
            if (unit.needsReload()) {
                unit.transitionTo(new ReloadingState(unit.getWeapon().reloadTicks()));
            } else {
                unit.transitionTo(unit.patrolState());
                unit.patrolState().update(unit, world);
            }
            return;
        }
        Zombie target = threat.get();
        unit.lockTarget(target);
        if (unit.getAmmo() == 0) {
            unit.transitionTo(new ReloadingState(unit.getWeapon().reloadTicks()));
            return;
        }
        if (unit.distanceTo(target) > unit.getWeapon().range()) {
            movement.move(unit, world);
        }
        // Moving into range can fire on this tick, matching the fork's response.
        if (unit.distanceTo(target) <= unit.getWeapon().range()) {
            try {
                unit.shoot();
            } catch (NoTargetException noTarget) {
                // An invalidated target is selected again on the next tick.
            }
        }
    }
}
