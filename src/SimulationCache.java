import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//TODO make sure the cache does not negatively impact results
public class SimulationCache {
    private final int cacheAccuracyDigits = 4;
    private final double cacheMax = 120;
    private double[][] cache = new double[81][(int) ((Math.pow(10,
            cacheAccuracyDigits)) * cacheMax + 1)];;
    private final int maxIndex = (int) ((Math.pow(10, cacheAccuracyDigits))
            * cacheMax);
    private static final SimulationCache sC = new SimulationCache();
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);;

    public static SimulationCache getInstance() {
        return sC;
    }

    public void setValue(final int corrupted, final double factor,
            final double value) {
        int fac = 0;
        if (factor >= cacheMax) {
            fac = maxIndex;

        } else {
            fac = (int) (factor * Math.pow(10, cacheAccuracyDigits));
        }
        readWriteLock.writeLock().lock();
        cache[corrupted][fac] = value;
        readWriteLock.writeLock().unlock();
    }

    public double getValue(final int corrupted, final double factor) {
        int fac = 0;
        if (factor >= cacheMax) {
            fac = maxIndex;
            
        } else {
            fac = (int) (factor
                    * Math.pow(10, cacheAccuracyDigits));
        }
        readWriteLock.readLock().lock();
        double val =  cache[corrupted][fac];
        readWriteLock.readLock().unlock();
        return val;
    }
}
