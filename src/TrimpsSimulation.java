import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrimpsSimulation {

    public final static int goldenFrequency = 30;
    public final static int blacksmitheryZone = 284;
    public final static double critChance = 0.726;
    public final static double critDamage = 13.7;
    public final static double cellDelay = 0.4;
    public final static double attackDelay = 0.258;
    public final static double okFactor = 0.15;
    public final static double achievementDamage = 13.577;
    public final static double heirloomDamage = 5.7;
    public final static double robotrimpDamage = 7;
    public final static double heirloomMetalDrop = 6.04;
    public final static double heirloomMinerEff = 6.12;
    public final static int corruptionStart = 151;
    private final static double[] mapOffsets = new double[] { 100, 0.75, 0.5,
            0.2, 0.13, 0.08, 0.05, 0.036, 0.03, 0.0275 };
    private final boolean useCache;
    private Perks perks;
    private double goldenHeliumMod;
    private double goldenBattleMod;
    private double goldenBought;
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
    private double corruptMod;
    private EquipmentManager eM;
    private PopulationManager pM;
    private ZoneSimulation zoneSimulation;



    // private double[][] zoneStats = new double[100][10];

    public static void main(String[] args) {
        long times = System.nanoTime();
        int[] perks = new int[] {93,87,88,101,67300,47600,16800,56700,59,86,45};
        Perks p = new Perks(perks, 24900000000000d);
        TrimpsSimulation tS = new TrimpsSimulation(p, false,
                //new AveragedZoneSimulation());
        		new ProbabilisticZoneModel(critChance, critDamage, okFactor));
        double highestHeHr = 0;
        while (true) {
            tS.startZone();
            tS.pM.buyCoordinations();
            tS.doMapsAndBuyStuff();
            tS.pM.buyCoordinations();
            tS.doZone();
            tS.endZone();
            double newHeHr = tS.getHeHr();
            if (newHeHr < highestHeHr) {
                break;
            }
            highestHeHr = newHeHr;
        }
        System.out.println(highestHeHr);
        System.out.println(tS.time / 3600);
        System.out.println(tS.zone);
        System.out.println(tS.helium);
        System.out.println((System.nanoTime() - times) / 1000000);
    }



    public SimulationResult runSimulation() {
        double highestHeHr = 0;
        while (true) {
            startZone();
            pM.buyCoordinations();
            doMapsAndBuyStuff();
            pM.buyCoordinations();
            doZone();
            endZone();
            double newHeHr = getHeHr();
            if (newHeHr < highestHeHr) {
                break;
            }
            highestHeHr = newHeHr;
        }
        return new SimulationResult(helium,time/3600,perks);
    }

    public TrimpsSimulation(final Perks perks, final boolean useCache,
            final ZoneSimulation zoneSimulation) {
        this.useCache = useCache;
        this.perks = perks;
        int[] perkLevels = perks.getPerkLevels();
        eM = new EquipmentManager(perkLevels[Perk.ARTISANISTRY.ordinal()]);
        pM = new PopulationManager(perkLevels[Perk.CARPENTRY.ordinal()],
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
        goldenBattleMod = 1;
        goldenBought = 0;
        corruptMod = 1;
        this.zoneSimulation = zoneSimulation;
    }

    private double getHeHr() {
        return (helium / time * 3600);
    }

    private void startZone() {
        zone++;
        mapsRunZone = 0;
        if (zone % goldenFrequency == 0) {
            // TODO code properly
            if (zone <= 530) {
                goldenBought++;
                goldenHeliumMod += goldenBought / 100d;
            } else {
                goldenBought++;
                goldenBattleMod += goldenBought * 3d / 100d;
            }

        }
        if (zone <= blacksmitheryZone) {
            eM.dropAll(zone, blacksmitheryZone);
        }
    }

    private void doMapsAndBuyStuff() {
        double damage = damageMod * goldenBattleMod * eM.gerTotalDamage()
                * pM.getDamageFactor();
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
        double damage = damageMod * goldenBattleMod * eM.gerTotalDamage()
                * pM.getDamageFactor() * (1d + 0.2d * mapsRunZone);
        double hp = enemyHealth();
        double damageFactor = damage / hp;
        double res = 0;
        if (useCache) {
            int corrupted = Math.min(80,
                    Math.max(0, ((int) ((zone - corruptionStart) / 3)) + 2));
            double cachedValue = SimulationCache.getInstance()
                    .getValue(corrupted, damageFactor);
            if (cachedValue == 0) {
                res = zoneSimulation.getExpectedTime(cellDelay, attackDelay,
                        damageFactor, critChance, critDamage, okFactor,
                        corruptMod, corruptionStart, zone);
                SimulationCache.getInstance().setValue(corrupted, damageFactor,
                        res);
            } else {
                res = cachedValue;
            }
        } else {
            // zoneStats = new double[100][10];
            res = zoneSimulation.getExpectedTime(cellDelay, attackDelay,
                    damageFactor, critChance, critDamage, okFactor, corruptMod,
                    corruptionStart, zone);
            if (damageFactor < 10) {
                //double estimate = probZoneSim(damageFactor);
                //for (int i = 0; i < 100; i++) {
                    // System.out.format("sim cell %d stats: eH=%.2f eOK=%.3f
                    // pN=%.3f eOKc=%.2f pC=%.2f fresh=%.2f%n",
                    // i+1, zoneStats[i][0]/zoneStats[i][1],
                    // zoneStats[i][2]/zoneStats[i][3],
                    // zoneStats[i][3]/zoneSimulationRepeatAmount,
                    // zoneStats[i][4]/zoneStats[i][5],
                    // zoneStats[i][5]/zoneSimulationRepeatAmount,
                    // zoneStats[i][6]/zoneSimulationRepeatAmount);
                //}
            }
        }
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
        // TODO fix

        double heliumMod = 1;
        if (zone >= 59) {
            heliumMod *= 5;
        }
        if (zone >= corruptionStart) {
            heliumMod *= 2;
        }
        double a = 1.35 * (zone - 19);
        heliumMod = Math.round(heliumMod * Math.pow(1.23, Math.sqrt(a)))
                + Math.round(heliumMod * a);
        if (zone >= 201) {
            heliumMod *= 1.2;
        }
        heliumMod *= Math.pow(1.005, zone);
        heliumMod *= lootingMod;
        heliumMod *= goldenHeliumMod;
        helium += heliumMod * (1 + 0.15 * Math.min(80,
                Math.max(0, ((int) ((zone - corruptionStart) * 3)) + 2)));
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
            corruptMod = 10 * Math.pow(1.05, Math.floor((zone - 150) / 6));
            res *= corruptMod;
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


}
