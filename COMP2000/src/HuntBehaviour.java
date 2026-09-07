/** Moves a military unit towards its currently detected zombie. */
public class HuntBehaviour implements MovementBehaviour {
    @Override
    public void move(Military unit, World world) {
        Zombie target = unit.getTarget();
        if (target != null && target.isActive()) {
            unit.moveTowards(target.getX(), target.getY(), world);
        }
    }

    @Override
    public String describe() {
        return "Engaging";
    }
}
