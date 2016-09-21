
public abstract class ZoneSimulation {
    public abstract double getExpectedTime(final double cellDelay,
            final double attackDelay, final double damageFactor,
            final double critChance, final double critDamage,
            final double okFactor, final double corruptMod,
            final int corruptionStart, final int zone);
}
