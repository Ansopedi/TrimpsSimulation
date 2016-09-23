
public class PopulationManager {
    private final double buildingDiscount;
    private double population;
    private double armyPopulation;
    private int unboughtCoordinations;
    private final double coordFactor;
    private final double popMod;
    private int gigaStations;
    private int warpStations;
    private double damageFactor;
    private double metal;
    
    private double SVpopulation;
    private double SVarmyPopulation;
    private int SVunboughtCoordinations;
    private int SVgigaStations;
    private int SVwarpStations;
    private double SVdamageFactor;
    private double SVmetal;

    public PopulationManager(
    		final double popMod,
    		final double buildingDiscount,
    		final double coordFactor) {
        this.buildingDiscount = buildingDiscount;
        this.coordFactor = coordFactor;
        this.popMod = popMod;
        population = 100000 * popMod;
        unboughtCoordinations = 0;
        armyPopulation = 1;
        gigaStations = -1;
        warpStations = 0;
        damageFactor = 1;
        metal = 0;
    }
    
    public void save() {
        SVpopulation = population;
        SVarmyPopulation = armyPopulation;
        SVunboughtCoordinations = unboughtCoordinations;
        SVgigaStations = gigaStations;
        SVwarpStations = warpStations;
        SVdamageFactor = damageFactor;
        SVmetal = metal;
    }    
    
    public void restore() {
        population = SVpopulation;
        armyPopulation = SVarmyPopulation;
        unboughtCoordinations = SVunboughtCoordinations;
        gigaStations = SVgigaStations;
        warpStations = SVwarpStations;
        damageFactor = SVdamageFactor;
        metal = SVmetal;
    }

    public void buyCoordinations() {
        while (buyCoordination()) {

        }
    }

    private boolean buyCoordination() {
        if (unboughtCoordinations == 0 || !canBuyCoordination()) {
            return false;
        }
        armyPopulation *= coordFactor;
        armyPopulation = Math.ceil(armyPopulation);
        damageFactor *= 1.25;
        damageFactor = Math.ceil(damageFactor);
        unboughtCoordinations--;
        return true;
    }

    private boolean canBuyCoordination() {
        return armyPopulation * 3 <= population;
    }

    private double getWarpStationCost() {
        return 1000000000000000l * Math.pow(1.75, gigaStations)
                * Math.pow(1.4, warpStations) * buildingDiscount;
    }

    private double getWarpStationEffect() {
        return 10000 * Math.pow(1.2, gigaStations) * popMod;
    }

    private void buyWarpStation() {
        warpStations++;
        population += getWarpStationEffect();
    }

    public void applyTauntImps(final int amount) {
        population *= Math.pow(1.003, amount);
    }

    public void newGigaStation() {
        if (gigaStations == -1) {
            warpStations = 0;
        } else {
            warpStations = 1;
        }
        gigaStations++;
    }

    public void newCoordination() {
        unboughtCoordinations++;
    }

    public double getDamageFactor() {
        return damageFactor;
    }

    public double getPopulation() {
        return population;
    }

    // TODO optimize gigastation buys (will matter for <5B He or so)
    // -> lost tauntimp population from not buying a gigastation is about .2% per zone,
    // so buy one if you can't get that much pop gain from warpstations this zone
    public void buyStuff(final double newMetal) {
    	metal += newMetal;
        if (gigaStations >=0) {
        	while (metal > getWarpStationCost()) {
        		metal -= getWarpStationCost();
        		buyWarpStation();
        	}
        }
    }
}
