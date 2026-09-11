import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;

public class Building extends Entity {
    private final Image image;
    private final int width;
    private final int height;
    private final int capacity;
    private int occupantCount;

    public Building(double x, double y, int width, int height) {
        this(x, y, width, height, 10);
    }

    public Building(double x, double y, int width, int height, int capacity) {
        super(x, y);
        if (width > 0 && height > 0) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Building capacity must be positive");
            } else {
                this.width = width;
                this.height = height;
                this.capacity = capacity;
                this.occupantCount = 0;

                Image loadedImage;
                try {
                    URL imageURL = Building.class.getResource("/assets/Building.png");
                    if (imageURL == null) {
                        throw new IOException("Building.png could not be found");
                    }

                    loadedImage = ImageIO.read(imageURL);
                    if (loadedImage == null) {
                        throw new IOException("Building.png is corrupted or unsupported");
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to load the building image", exception);
                }

                this.image = loadedImage;
            }
        } else {
            throw new IllegalArgumentException("Building dimensions must be positive");
        }
    }

    public boolean isFull() {
        return this.occupantCount >= this.capacity;
    }

    public boolean canEnter() {
        return !this.isFull();
    }

    public boolean enter() {
        if (this.isFull()) {
            return false;
        } else {
            ++this.occupantCount;
            return true;
        }
    }

    public boolean leave() {
        if (this.occupantCount == 0) {
            return false;
        } else {
            --this.occupantCount;
            return true;
        }
    }

    public boolean containsPoint(double pointX, double pointY) {
        return pointX >= this.getX() && pointX < this.getX() + (double)this.width && pointY >= this.getY() && pointY < this.getY() + (double)this.height;
    }

    public double getEntranceX() {
        return this.getX() + (double)this.width / (double)2.0F;
    }

    public double getEntranceY() {
        return this.getY() + (double)this.height;
    }

    public int getOccupantCount() {
        return this.occupantCount;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void update(World world) {
    }

    public void draw(Graphics2D graphics) {
        graphics.drawImage(this.image, (int)this.getX(), (int)this.getY(), this.width, this.height, (ImageObserver)null);
    }
}
