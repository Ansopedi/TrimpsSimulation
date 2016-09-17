import java.math.BigDecimal;
import java.math.BigInteger;

public class EquipmentManager {
    private final int[] currentPrestiges;
    private final int[] maxPrestiges;
    private final int[] currentLevels;
    private final static int dmgMod = 13;
    private final static int costMod = 53;
    private final static double dmgBase = 1.19;
    private final static double costBase = 1.069;
    private final double artisanistryFactor;

    public EquipmentManager(final int artisanistryLevel) {
        currentPrestiges = new int[Equipment.values().length];
        maxPrestiges = new int[Equipment.values().length];
        currentLevels = new int[Equipment.values().length];
        for (int x = 0; x < Equipment.values().length; x++) {
            currentPrestiges[x] = 2;
            maxPrestiges[x] = 2;
            currentLevels[x] = 1;
        }
        artisanistryFactor = Math.pow(0.95, artisanistryLevel);
    }

    public double gerTotalDamage() {
        double total = 0;
        for (Equipment e : Equipment.values()) {
            if (e.damage) {
                total += getCurrentEquipmentValue(e)
                        * currentLevels[e.ordinal()];
            }
        }
        return total;
    }

    private double getCurrentEquipmentValue(final Equipment equipment) {
        if (!equipment.damage) {
            return -1;
        }
        return equipment.baseEffect * Math.pow(dmgBase,
                ((currentPrestiges[equipment.ordinal()] - 1) * dmgMod + 1));
    }

    private double getNextEquipmentValue(final Equipment equipment) {
        if (!equipment.damage) {
            return -1;
        }
        return equipment.baseEffect * Math.pow(dmgBase,
                ((currentPrestiges[equipment.ordinal()]) * dmgMod + 1));
    }

    private double getCurrentEquipmentCost(final Equipment equipment) {
        if (!equipment.damage) {
            return -1;
        }
        double mod = 0;
        if (currentPrestiges[equipment.ordinal()] >= 3) {
            mod = (currentPrestiges[equipment.ordinal()] - 3) * 0.85 + 2;
        } else {
            mod = currentPrestiges[equipment.ordinal()] - 1;
        }
        return equipment.baseCost * Math.pow(costBase, mod * costMod + 1)
                * artisanistryFactor;
    }

    private double getNextEquipmentCost(final Equipment equipment) {
        if (!equipment.damage) {
            return -1;
        }
        double mod = 0;
        if (currentPrestiges[equipment.ordinal()] >= 3) {
            mod = (currentPrestiges[equipment.ordinal()] - 2) * 0.85 + 2;
        } else {
            mod = currentPrestiges[equipment.ordinal()];
        }
        return equipment.baseCost * Math.pow(costBase, mod * costMod + 1)
                * artisanistryFactor;
    }

    public double buyStuff(final double metal) {
        // TODO
        double res = metal;
        for (int x = 0; x < 2; x++) {
            for (Equipment e : Equipment.values()) {
                if (e.damage && currentPrestiges[e.ordinal()] < maxPrestiges[e
                        .ordinal()] && getNextEquipmentCost(e) < res) {
                    res -= getNextEquipmentCost(e);
                    currentPrestiges[e.ordinal()]++;
                    currentLevels[e.ordinal()] = 1;
                }
            }
        }
        return metal - res;
    }

    public void dropMap(final int zone, final int blacksmitheryZone) {
        if (zone <= blacksmitheryZone) {
            return;
        }
        for (Equipment e : Equipment.values()) {
            int max = (zone - e.firstDropLevel) / 5 + 1;
            if (max > maxPrestiges[e.ordinal()]) {
                maxPrestiges[e.ordinal()] = Math
                        .min(maxPrestiges[e.ordinal()] + 2, max);
                break;
            }
        }
    }

    public void dropAll(final int zone, final int blacksmitheryZone) {
        int relZone = zone % 10;
        for (Equipment e : Equipment.values()) {
            if (e.firstDropLevel == relZone) {
                if ((zone - 1) / 5 + 1 > maxPrestiges[e.ordinal()]) {
                    if (blacksmitheryZone - 5 >= zone) {
                        maxPrestiges[e.ordinal()] += 2;
                    } else {
                        maxPrestiges[e.ordinal()]++;
                    }
                }
            }
        }
    }
}
