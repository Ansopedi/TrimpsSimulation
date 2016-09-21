
public abstract class ZoneSimulation {
    private final static double cellDelay = 0.4;
    private final static double attackDelay = 0.258;
    
    public abstract double getExpectedTime(final double damageFactor, final double critChance, final double critDamage, final int zone);
    
}
