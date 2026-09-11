/**
 * Charges straight at the unit's current target zombie so that it can be
 * brought within firing range. This is the {@link Military} unit's
 * behaviour whenever a zombie has been detected.
 */
public class HuntBehaviour implements MovementBehaviour {

    @Override
    public void move(Military unit, World world) {
        Zombie target = unit.getTarget();
        if (target == null || !target.isActive()) {
            return;
        }
        unit.moveTowards(target.getX(), target.getY(), world);
    }

    @Override
    public String describe() {
        return "Engaging";
    }
}
