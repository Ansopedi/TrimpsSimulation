
public class PopulationManager {
    private double resourcefulFactor;
    private double population;
    private double armyPopulation;
    private int unboughtCoordinations;
    private double coordinationFactor;
    private double carpentryFactor;
    private int gigaStations;
    private int warpStations;
    private double damageFactor;
    private double metal;
    
    private double SVresourcefulFactor;
    private double SVpopulation;
    private double SVarmyPopulation;
    private int SVunboughtCoordinations;
    private double SVcoordinationFactor;
    private double SVcarpentryFactor;
    private int SVgigaStations;
    private int SVwarpStations;
    private double SVdamageFactor;
    private double SVmetal;

    public PopulationManager(final int carpentryLevel,
            final int carpentry2Level, final int resourcefulLevel,
            final int coordinatedLevel) {
        resourcefulFactor = Math.pow(0.95, resourcefulLevel);
        coordinationFactor = 1 + 0.25 * Math.pow(0.98, coordinatedLevel);
        carpentryFactor = Math.pow(1.1, carpentryLevel)
                * (1 + 0.0025 * carpentry2Level);
        population = 100000 * carpentryFactor;
        unboughtCoordinations = 0;
        armyPopulation = 1;
        gigaStations = -1;
        warpStations = 0;
        damageFactor = 1;
        metal = 0;
    }
    
    public void save() {
        SVresourcefulFactor = resourcefulFactor;
        SVpopulation = population;
        SVarmyPopulation = armyPopulation;
        SVunboughtCoordinations = unboughtCoordinations;
        SVcoordinationFactor = coordinationFactor;
        SVcarpentryFactor = carpentryFactor;
        SVgigaStations = gigaStations;
        SVwarpStations = warpStations;
        SVdamageFactor = damageFactor;
        SVmetal = metal;
    }    
    
    public void restore() {
        resourcefulFactor = SVresourcefulFactor;
        population = SVpopulation;
        armyPopulation = SVarmyPopulation;
        unboughtCoordinations = SVunboughtCoordinations;
        coordinationFactor = SVcoordinationFactor;
        carpentryFactor = SVcarpentryFactor;
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
        armyPopulation *= coordinationFactor;
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
                * Math.pow(1.4, warpStations) * resourcefulFactor;
    }

    private double getWarpStationEffect() {
        return 10000 * Math.pow(1.2, gigaStations) * carpentryFactor;
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
