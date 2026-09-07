/** Strategy for moving a military unit during one simulation tick. */
public interface MovementBehaviour {
    void move(Military unit, World world);

    String describe();
}
