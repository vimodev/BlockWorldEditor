/**
 * Timer for checking time intervals
 */
public class Timer {

    private double alpha = 0.2;
    private double previousTime;
    private double frequency = 0;

    public Timer() {
        init();
    }

    /**
     * Initializes the timer by looking at time of calling
     */
    public void init() {
        previousTime = ((double) System.nanoTime()) / (double) 1000000000L;
        frequency = 0;
    }

    /**
     * Gets the delta time in seconds since last call or init() if no last call exists
     * @return delta time
     */
    public double dt() {
        double currentTime = ((double) System.nanoTime()) / (double) 1000000000L;
        double dt = currentTime - previousTime;
        previousTime = currentTime;
        frequency = (1 - alpha) * frequency + alpha / dt;
        return dt;
    }

    /**
     * Get time since last non-read dt. useful for intermittent checking
     * @return
     */
    public double readDt() {
        double currentTime = ((double) System.nanoTime()) / (double) 1000000000L;
        double dt = currentTime - previousTime;
        return dt;
    }

    public double getFrequency() {
        return frequency;
    }

}