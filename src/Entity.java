import java.awt.Graphics2D;

public abstract class Entity {

    private double x;
    private double y;
    private boolean active;

    protected Entity(double x, double y) {
        this.x = x;
        this.y = y;
        this.active = true;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    protected void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public double distanceTo(Entity other) {
        double differenceX = other.x - x;
        double differenceY = other.y - y;

        return Math.sqrt(
                differenceX * differenceX
                        + differenceY * differenceY
        );
    }

    public abstract void update(World world);

    public abstract void draw(Graphics2D graphics);
}
