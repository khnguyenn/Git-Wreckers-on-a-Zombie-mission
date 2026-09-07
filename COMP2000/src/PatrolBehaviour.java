import java.awt.geom.Point2D;
import java.util.List;

/** Moves a military unit around a fixed loop of waypoints. */
public class PatrolBehaviour implements MovementBehaviour {
    private static final double ARRIVAL_DISTANCE = 8.0;

    private final List<Point2D> waypoints;
    private int waypointIndex;

    public PatrolBehaviour(List<Point2D> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "Patrol route must contain at least one waypoint"
            );
        }
        this.waypoints = List.copyOf(waypoints);
    }

    @Override
    public void move(Military unit, World world) {
        Point2D waypoint = waypoints.get(waypointIndex);
        unit.moveTowards(waypoint.getX(), waypoint.getY(), world);

        double distance = Math.hypot(
                waypoint.getX() - unit.getX(),
                waypoint.getY() - unit.getY()
        );
        if (distance <= ARRIVAL_DISTANCE) {
            waypointIndex = (waypointIndex + 1) % waypoints.size();
        }
    }

    @Override
    public String describe() {
        return "Patrolling";
    }
}
