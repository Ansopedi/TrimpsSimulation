import java.util.Random;

public class TrimpsSimulation {

    private final static int goldenFrequency = 30;
    private final static int blacksmitheryZone = 284;
    private final static double critChance = 0.726;
    private final static double critDamage = 13.7;
    private final static double achievementDamage = 13.577;
    private final static double heirloomDamage = 5.7;
    private final static double robotrimpDamage = 7;
    private final static double heirloomMetalDrop = 6.04;
    private final static double heirloomMinerEff = 6.12;
    private final static double cellDelay = 0.4;
    private final static double attackDelay = 0.258;
    private final static int corruptionStart = 151;
    private final static double[] mapOffsets = new double[] { 100, 0.75, 0.5,
            0.2, 0.13, 0.08, 0.05, 0.036, 0.03, 0.0275 };
    private final static int zoneSimulationRepeatAmount = 1000;
    private Perks perks;
    private double goldenHeliumMod;
    private double goldenHeliumBought;
    private double damageMod;
    private double dropMod;
    private double metalMod;
    private double jCMod;
    private double lootingMod;
    private double helium;
    private int mapsRunZone;
    private double time;
    private int zone;
    private double metal;
    EquipmentManager eM;
    PopulationManager pM;

    public static void main(String[] args) {
        int[] perks = new int[]{90,87,88,97,60000,37000,14000,39299,58,85,40};
        Perks p = new Perks(perks,14600000000000d);
        TrimpsSimulation tS = new TrimpsSimulation(p);
        double highestHeHrPercentage = 0;
        while (true) {
            tS.startZone();
            tS.pM.buyCoordinations();
            tS.doMapsAndBuyStuff();
            tS.pM.buyCoordinations();
            tS.doZone();
            tS.endZone();
            double newHeHrPercentage = tS.getHeHrPercentage();
            if (newHeHrPercentage<highestHeHrPercentage){
                break;
            }
            highestHeHrPercentage=newHeHrPercentage;
        }
        System.out.println(highestHeHrPercentage);
        System.out.println(tS.time/3600);
        System.out.println(tS.zone);
        System.out.println(tS.helium);
    }
    
    public double runSimulation(){
        double highestHeHrPercentage = 0;
        while (true) {
            startZone();
            pM.buyCoordinations();
            doMapsAndBuyStuff();
            pM.buyCoordinations();
            doZone();
            endZone();
            double newHeHrPercentage = getHeHrPercentage();
            if (newHeHrPercentage<highestHeHrPercentage){
                break;
            }
            highestHeHrPercentage = newHeHrPercentage;
        }
        return highestHeHrPercentage;
    }

    public TrimpsSimulation(final Perks perks) {
        this.perks = perks;
        int[] perkLevels = perks.getPerkLevels();
        eM = new EquipmentManager(perkLevels[Perk.ARTISANISTRY.ordinal()]);
        pM = new PopulationManager(perkLevels[Perk .CARPENTRY.ordinal()],
                perkLevels[Perk.CARPENTRY2.ordinal()],
                perkLevels[Perk.RESOURCEFUL.ordinal()],
                perkLevels[Perk.COORDINATED.ordinal()]);
        damageMod = achievementDamage
                * (1 + 0.05 * perkLevels[Perk.POWER.ordinal()])
                * (1 + 0.01 * perkLevels[Perk.POWER2.ordinal()]) * 7 * 4
                * robotrimpDamage * heirloomDamage;
        metalMod = 0.25 * heirloomMinerEff * 1.75
                * (1 + 0.05 * perkLevels[Perk.MOTIVATION.ordinal()])
                * (1 + 0.01 * perkLevels[Perk.MOTIVATION2.ordinal()]);
        dropMod = 0.16 * heirloomMetalDrop * 1.249 * 1.8 * 2
                * (1 + 0.05 * perkLevels[Perk.LOOTING.ordinal()])
                * (1 + 0.0025 * perkLevels[Perk.LOOTING2.ordinal()]);
        jCMod = 0.25 * heirloomMinerEff * 1.75
                * (1 + 0.05 * perkLevels[Perk.MOTIVATION.ordinal()])
                * (1 + 0.01 * perkLevels[Perk.MOTIVATION2.ordinal()])
                * (1 + 0.05 * perkLevels[Perk.LOOTING.ordinal()])
                * (1 + 0.0025 * perkLevels[Perk.LOOTING2.ordinal()]);
        lootingMod = (1 + 0.05 * perkLevels[Perk.LOOTING.ordinal()])
                * (1 + 0.0025 * perkLevels[Perk.LOOTING2.ordinal()]);
        helium = 0;
        time = 0;
        zone = 0;
        mapsRunZone = 0;
        metal = 0;
        goldenHeliumMod = 1;
        goldenHeliumBought = 0;
    }
    
    private double getHeHrPercentage(){
        return (helium/time*3600)/perks.getSpentHelium();
    }

    private void startZone() {
        zone++;
        mapsRunZone = 0;
        if (zone % goldenFrequency == 0) {
            goldenHeliumBought++;
            goldenHeliumMod += goldenHeliumBought / 100d;

        }
        if (zone <= blacksmitheryZone) {
            eM.dropAll(zone, blacksmitheryZone);
        }
    }

    private void doMapsAndBuyStuff() {
        double damage = damageMod * eM.gerTotalDamage() * pM.getDamageFactor();
        double hp = enemyHealth();
        double damageFactor = damage / hp;
        int mapsToRun = mapsToRun(damageFactor);
        if (mapsToRun > 0) {
            for (int x = 0; x < mapsToRun; x++) {
                eM.dropMap(zone, blacksmitheryZone);
            }
            mapsRunZone = mapsToRun;
            time += cellDelay * 13 * mapsToRun;
            metal += jCMod * 5 * (26 / 33.33 * mapsToRun) * 1.5
                    * pM.getPopulation()
                    + jCMod * 45 / 6 * (26 / 33.33 * mapsToRun) * 1.5
                            * pM.getPopulation();
        }
        metal -= pM.buyStuff(metal / 50);
        metal -= eM.buyStuff(metal);
    }

    private void doZone() {
        double damage = damageMod * eM.gerTotalDamage() * pM.getDamageFactor()
                * (1d + 0.2d * mapsRunZone);
        double hp = enemyHealth();
        double damageFactor = damage / hp;
        ZoneSimulation zS = new ZoneSimulation();
        double res = 0;
        for (int x = 0; x < zoneSimulationRepeatAmount; x++) {
            res += zS.runZone(damageFactor);
        }
        res /= zoneSimulationRepeatAmount;
        time += res;
        metal += metalMod * res * pM.getPopulation();
        metal += dropMod * 17 * pM.getPopulation();
    }

    private void endZone() {
        if (zone == 15) {
            metalMod *= 2;
            jCMod *= 2;
            dropMod *= 2;
        }
        if (zone <= 59) {
            metalMod *= 1.25;
            jCMod *= 1.25;
            dropMod *= 1.25;
        } else {
            metalMod *= 1.6;
            jCMod *= 1.6;
            dropMod *= 1.6;
        }
        metalMod *= Math.pow(1.003, 3);
        jCMod *= Math.pow(1.003, 6);
        dropMod *= Math.pow(1.003, 6);
        pM.applyTauntImps(3);
        pM.newCoordination();
        if (zone >= 60 && zone <= 70
                || zone >= 70 && zone <= 78 && zone % 2 == 0
                || zone >= 81 && zone <= 90 && zone % 3 == 0
                || zone >= 95 && zone <= 170 && zone % 5 == 0
                || zone >= 180 && zone % 10 == 0) {
            pM.newGigaStation();
        }
        if (zone >= 20) {
            addHelium();
        }
    }

    private void addHelium() {
        //TODO fix
    	double heliumMod = 1;
        if (zone >= 59) {
            heliumMod *= 5;
        }
        if (zone >= corruptionStart) {
            heliumMod *= 2;
        }
        double a = 1.35 * (zone - 19);
        heliumMod = Math.round( heliumMod * Math.pow(1.23, Math.sqrt(a))) + Math.round( heliumMod * a );
        if (zone >= 201) {
            heliumMod *= 1.2;
        }
        heliumMod *= Math.pow(1.005, zone);
        heliumMod *= lootingMod;
        heliumMod *= goldenHeliumMod;
        helium += heliumMod * (1 + 0.15*Math.min(80,Math.max(0, ((int) ((zone - corruptionStart) * 3)) + 2)));
    }

    private double enemyHealth() {
        double res = 0;
        res += 130 * Math.sqrt(zone) * Math.pow(3.265, zone / 2d);
        res -= 110;
        res *= 0.508;
        
        if (zone >= 60) {
            res *= Math.pow(1.1, zone - 59);
        }
        if (zone >= 151) {
            res *= 10 * Math.pow(1.05, Math.floor((zone - 150) / 6));
        }
        return Math.floor(res);
    }

    private int mapsToRun(final double damageFactor) {
        for (int x = 0; x < mapOffsets.length; x++) {
            if (damageFactor > mapOffsets[x]) {
                return x;
            }
        }
        return mapOffsets.length;
    }

    private class ZoneSimulation {

        private double runZone(final double damageFactor) {
            EnemyType[] zoneArray = createZone(Math.min(80,
                    Math.max(0, ((int) ((zone - corruptionStart) / 3)) + 2)));
            double res = 0;
            int cell = 1;
            double hp = getHPModifier(cell, zoneArray[cell - 1]);
            while (cell <= 100) {
                boolean crit = Math.random() < critChance;
                double damage = (crit) ? damageFactor * critDamage
                        : damageFactor;
                damage *= (1 + 0.2 * Math.random());
                boolean dodge = zoneArray[cell - 1] == EnemyType.AGILITY
                        && Math.random() < 0.3;
                if (dodge) {
                    res += attackDelay;
                    continue;
                }
                if (damage >= hp) {
                    cell++;
                    damage -= hp;
                    double overkillDamage = damage * 0.15;
                    if (cell == 101) {
                        res += cellDelay;
                        break;
                    } else {
                        hp = getHPModifier(cell, zoneArray[cell - 1]);
                    }
                    hp -= overkillDamage;
                    if (hp <= 0) {
                        cell++;
                        if (cell == 101) {
                            res += cellDelay;
                            break;
                        } else {
                            res += cellDelay;
                            hp = getHPModifier(cell, zoneArray[cell - 1]);
                        }
                    } else {
                        res += attackDelay;
                    }
                } else {
                    res += attackDelay;
                    hp -= damage;
                }
            }
            return res;
        }

        private double getHPModifier(final int pCell,
                final EnemyType enemyType) {
            //TODO properly implement
            if (enemyType == EnemyType.NORMAL){
                return 0.01;
            }
            double cellMod = (0.5 + 0.8 * (pCell / 100)) / 0.508;
            if (pCell < 100) {
                return cellMod * (enemyType == EnemyType.TOUGH ? 5 : 1);
            } else {
                return cellMod * 6;
            }
        }

        private EnemyType[] createZone(int numberCorrupted) {
            int numberNormal = 99 - numberCorrupted;
            EnemyType[] result = new EnemyType[100];
            Random random = new Random();
            result[99] = EnemyType.IMPROBABILITY;
            for (int x = 0; x < 99; x++) {
                int r = random.nextInt(numberNormal + numberCorrupted);
                if (r < numberNormal) {
                    result[x] = EnemyType.NORMAL;
                    numberNormal--;
                } else {
                    r = random.nextInt(6);
                    if (r == 0) {
                        result[x] = EnemyType.TOUGH;
                    } else if (r == 1) {
                        result[x] = EnemyType.AGILITY;
                    } else {
                        result[x] = EnemyType.COORUPTED;
                    }
                    numberCorrupted--;
                }
            }
            return result;
        }
    }

    private enum EnemyType {
        NORMAL, COORUPTED, TOUGH, AGILITY, IMPROBABILITY;
    }

}
