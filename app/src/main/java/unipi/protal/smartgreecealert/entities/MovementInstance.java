package unipi.protal.smartgreecealert.entities;

import java.time.Instant;

public class MovementInstance {

    private final long instanceTime;
    private final double aX;
    private final double aY;
    private final double aZ;
    private final double accelerationVector;

    public MovementInstance(double aX, double aY, double aZ) {
        this.instanceTime = Instant.now().toEpochMilli();
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
        /* Axis divided by Earth's Standard Gravity on surface 9.80665 m/s^2 */
        this.accelerationVector = Math.sqrt(Math.pow(aX, 2) + Math.pow(aY, 2) + Math.pow(aZ, 2)) / 9.80665;
    }

    public long getInstanceTime() {
        return instanceTime;
    }

    public double getX() {
        return aX;
    }

    public double getY() {
        return aY;
    }

    public double getZ() {
        return aZ;
    }

    public double getAccelerationVector() {
        return accelerationVector;
    }
}
