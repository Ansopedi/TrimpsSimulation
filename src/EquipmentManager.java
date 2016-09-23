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
    private double metal;
    
    private final int[] SVcurrentPrestiges;
    private final int[] SVmaxPrestiges;
    private final int[] SVcurrentLevels;
    private double SVmetal;

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
        metal = 0;
        

        SVcurrentPrestiges = new int[Equipment.values().length];
        SVmaxPrestiges = new int[Equipment.values().length];
        SVcurrentLevels = new int[Equipment.values().length];
        SVmetal = 0;
    }
    
    public void save() {
    	System.arraycopy(currentPrestiges, 0, SVcurrentPrestiges, 0, currentPrestiges.length);
    	System.arraycopy(maxPrestiges, 0, SVmaxPrestiges, 0, maxPrestiges.length);
    	System.arraycopy(currentLevels, 0, SVcurrentLevels, 0, currentLevels.length);
    	SVmetal = metal;
    }
    
    public void restore() {
    	System.arraycopy(SVcurrentPrestiges, 0, currentPrestiges, 0, currentPrestiges.length);
    	System.arraycopy(SVmaxPrestiges, 0, maxPrestiges, 0, maxPrestiges.length);
    	System.arraycopy(SVcurrentLevels, 0, currentLevels, 0, currentLevels.length);
    	metal = SVmetal;
    }

    public double getTotalDamage() {
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

    public void buyStuff(final double newMetal) {
    	metal += newMetal;
        for (int x = 0; x < 2; x++) {
            for (Equipment e : Equipment.values()) {
                if (e.damage && currentPrestiges[e.ordinal()] < maxPrestiges[e
                        .ordinal()] && getNextEquipmentCost(e) < metal) {
                    metal -= getNextEquipmentCost(e);
                    currentPrestiges[e.ordinal()]++;
                    currentLevels[e.ordinal()] = 1;
                }
            }
        }
    }
    
    // how many maps to drop all available prestiges of equipment index 'e'? *if* there are no currently unbought levels.
    public int mapsForPrestiges(final int zone, final int blacksmitheryZone, final int e) {
    	int maps = 0;
    	// TODO lazy. could be calculated instead of this save/emulate/restore.
    	this.save();
    	//System.out.format("zone %d equip %d maxP %d%n", zone, e, maxPrestiges[e]);
    	while (maxPrestiges[e] < maxPrestigeAvailable(zone, e) && currentPrestiges[e] == maxPrestiges[e]) {
    		dropMap(zone, blacksmitheryZone);
    		maps++;
    	}
    	//if (maps > 1) {
    	//	System.out.println("prestige level " + maxPrestiges[e]);
    	//}
    	this.restore();
    	return maps;
    }

    public void dropMap(final int zone, final int blacksmitheryZone) {
        //if (zone <= blacksmitheryZone) {
        //    return;
        //}
        for (Equipment e : Equipment.values()) {
        	int max = maxPrestigeAvailable(zone, e.ordinal());
            if (max > maxPrestiges[e.ordinal()]) {
                maxPrestiges[e.ordinal()] = Math
                        .min(maxPrestiges[e.ordinal()] + 2, max);
                break;
            }
        }
    }
    
    private int maxPrestigeAvailable(final int zone, final int e) {
    	return 2 * ((zone - Equipment.values()[e].firstDropLevel) / 10) + 2;
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
