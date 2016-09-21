import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrimpsSimulation {

    private final static int goldenFrequency = 30;
    private final static int blacksmitheryZone = 284;
    private final static double critChance = 0.726;
    private final static double critDamage = 13.7;
    private final static double okFactor = 0.15;
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


    private final static double ehInc = 1.005;
    private static double ehMax = 500 * critDamage;
    private final static int ehLength = getEHidx(ehMax) + 1;
    private static double ehProgress = 0;
    private final static double[] expHits = new double[ehLength];
    private final static double[] expOK = new double[ehLength];
    private final static double[] expOKc = new double[ehLength];
    private final static double[] expPn = new double[ehLength];
    private final static double[] expPc = new double[ehLength];
    
    private final static int dodgeLength = 20;
    private final static double expectedDodges = calculateExpectedDodges(dodgeLength);
    
    // average trimp damage per hit
    private final static double damagePerHit = (critDamage * critChance) + (1 - critChance);
    
    public static void main(String[] args) {
    	buildExpectedHits(ehMax, ehInc);
        long times = System.nanoTime();
        int[] perks = new int[] { 91, 90, 88, 99, 76400, 48500, 13600, 45600,
                59, 88, 47 };
        Perks p = new Perks(perks, 190900000000000d);
        TrimpsSimulation tS = new TrimpsSimulation(p, false);
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
    
    private static int getEHidx (double hp) {
    	return (int) (Math.log(hp*Math.pow(ehInc,.01))/Math.log(ehInc));
    }
    
    private static void weightedAvg(double[] res, double fRes, double[] tmp, double fTmp, int length) {
    	for (int i=0; i < length; i++) {
    		res[i] = res[i] * fRes + tmp[i] * fTmp;
    	}
    }
    
    private static void buildExpectedHits(double max, double inc) {
    	for (double hp = 1; hp < max ; hp *= inc) {
    		int i = getEHidx(hp);
    		double tmp[] = calcEH(hp);
    		expHits[i] = tmp[0];
    		expOK[i] = tmp[1];
    		expOKc[i] = tmp[2];
    		expPn[i] = tmp[3];
    		expPc[i] = tmp[4];
    		ehProgress = hp;
    		//System.out.format("hp=%.3f i=%d eH=%.2f eOK=%.3f eOKc=%.2f ePn=%.3f eCn=%.3f%n", hp, i, tmp[0], tmp[1], tmp[2], tmp[3], tmp[4]);
    	}
    	ehMax = ehProgress;
    }
    

    private static double[] calcEH(double hp) {
    	double[] res = { 0, 0, 0, 0, 0 }; // hits, okdmg on hit, okdmg on crit
    	// if we always kill, actually calculate the results (best accuracy this way compared to building even a very extensive table for values from 0 to 1)
    	if (hp < 1) {
    		res[0] = 1;
    		res[1] = (1.1 - hp) * okFactor;
    		res[2] = (critDamage * 1.1 - hp) * okFactor;
    		res[3] = 1 - critChance;
    		res[4] = critChance;
    		return res;
    	} else if (hp > ehMax) { // beyond the table, expHits can be calculated to high accuracy and other values assumed to be stable
    		res[0] = (hp - ehMax) / damagePerHit + expHits[ehLength - 1];
    		res[1] = expOK[ehLength - 1];
    		res[2] = expOKc[ehLength - 1];
    		res[3] = expPn[ehLength - 1];
    		res[4] = expPc[ehLength - 1];
    		return res;
    	}
    	// already calculated result
    	if (hp < ehProgress) {
    		int i = getEHidx(hp);
    		res[0] = expHits[i];
    		res[1] = expOK[i];
    		res[2] = expOKc[i];
    		res[3] = expPn[i];
    		res[4] = expPc[i];
    		return res;
    	}
    	// calculate new result
    	double[] tmp = { 0, 0, 0, 0, 0 };
    	double tprob = 0;
    	// do crit stuff
    	if (hp < critDamage) {
    		res[0] += 1;
    		res[2] += (critDamage * 1.1 - hp) * okFactor;
    		res[4] += 1;
    	} else if (hp < critDamage * 1.2) {
    		// crit doesn't kill
    		tprob = (hp - critDamage) / (0.2 * critDamage);
    		tmp = calcEH((hp - critDamage) / 2d);
    		tmp[0] += 1;
    		weightedAvg(res, 1, tmp, tprob, 5);
    		// crit does kill
    		res[0] += (1 - tprob);
    		res[2] += (1 - tprob) * (critDamage * 1.2 - hp) / 2d * okFactor;
    		res[4] += (1 - tprob);
    	} else {
    		tmp = calcEH(hp - critDamage * 1.1);
    		tmp[0] += 1;
    		weightedAvg(res, 1, tmp, 1, 5);
    	}
    	
    	// do noncrit stuff - note we already know hp >= 1 since otherwise we directly calculate and return a result
    	double[] ncres = { 0, 0, 0, 0, 0 };
    	if (hp < 1.2) {
    		// hit doesn't kill
    		tprob = (hp - 1) / 0.2;
    		tmp = calcEH((hp - 1) / 2d);
    		tmp[0] += 1;
    		weightedAvg(ncres, 1, tmp, tprob, 5);
    		// hit does kill
    		ncres[0] += (1 - tprob);
    		ncres[1] += (1 - tprob) * (1.2 - hp) / 2d * okFactor;
    		ncres[3] += (1 - tprob);
    	} else {
    		tmp = calcEH(hp - 1.1);
    		tmp[0] += 1;
    		weightedAvg(ncres, 1, tmp, 1, 5);
    	}
    	weightedAvg(res, critChance, ncres, (1 - critChance), 5);
    	return res;
    }
    
    private static double calculateExpectedDodges(final int length) {
    	double res = 0;
    	double cumChance = 0.7;
    	for (int hits = 1; hits < length; hits++) {
    		//System.out.println(res + " expected dodges for hit " + (hits-1));
    		cumChance *= 0.3;
    		res += cumChance;
    	}
    	return res;
    }

    public double runSimulation() {
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
        return highestHeHr;
    }

    public TrimpsSimulation(final Perks perks, final boolean useCache) {
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
                for (int x = 0; x < zoneSimulationRepeatAmount; x++) {
                    res += runZone(damageFactor);
                }
                res /= zoneSimulationRepeatAmount;
                SimulationCache.getInstance().setValue(corrupted, damageFactor,
                        res);
            } else {
                res = cachedValue;
            }
        } else {
            for (int x = 0; x < zoneSimulationRepeatAmount; x++) {
                res += runZone(damageFactor);
            }
            res /= zoneSimulationRepeatAmount;
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
        
        private double probZoneSim(final double damageFactor) {
        	double nCorrupt = (double) getNumCorrupt();
        	double corrRows = Math.min(10, Math.ceil(nCorrupt/6d));
        	double baseHp = 1 / (.508 * damageFactor);
        	double pCorr = nCorrupt / Math.min(corrRows * 10d,99);
        	
        	// expHits, expOK, expOKc, expPn, expPc, freshness (chance of a fresh cell, i.e. first cell of zone or previous cell was OKd)
        	double[] cellStats = { 0, 0, 0, 0, 0, 1 };
        	double[] tmp = { 0, 0, 0, 0, 0, 0 };
        	
        	double zoneTime = 0;
        	
        	for (int cell = 1; cell <= 100; cell++) {
        		double hpC = baseHp * (.5 + .8 * cell/100d);
        		// TODO model normal HP enemies during Corrupted challenge? may also need this in getEnemyHealth?
        		double hpN = hpC * 1.2 / (10 * Math.pow(1.005,(zone - 150) / 6));
        		double pCorrFinal = (cell == 100) ? 1 : (Math.ceil(cell / 10d) <= corrRows) ? pCorr : 0;
        		double freshness = cellStats[5]; // this cell's freshness is used to weight the results between the staleSim and freshSim

        		// stats if cell is stale (including freshness of next cell)
        		if (freshness < 1) {
        			cellStats = staleSim(pCorrFinal, hpC, hpN, cellStats);
        		}
            	
        		// stats if cell is fresh
            	if (freshness > 0) {
            		System.arraycopy(freshSim(pCorrFinal, hpC, hpN), 0, tmp, 0, 5);
            	}
        		tmp[5] = 0; // next cell can't be fresh if this one was fresh (except in the negligible event that we exactly kill the cell with no OK dmg)

        		// avg together stale and fresh stats
        		staleAvg(cellStats, (1 - freshness), tmp, freshness);
        		
        		
        		// if next cell is fresh, this cell adds no time (because it was overkilled)
        		// else add celltime for the first hit, and hittime for each subsequent hit
        		double cellTime = (1 - cellStats[5]) * (cellDelay + (cellStats[0] - 1) * attackDelay);
        		zoneTime += (1 - cellStats[5]) * (cellDelay + (cellStats[0] - 1) * attackDelay);
            	System.out.format("cell %d stats: time=%.2f hpC=%.3f hpN=%.3f eH=%.2f eOK=%.3f pN=%.3f eOKc=%.2f pC=%.2f fresh=%.2f" + "%n", 
            			cell, cellTime, hpC, hpN, cellStats[0], cellStats[1], cellStats[3], cellStats[2], cellStats[4], cellStats[5]);
        	}
        	//System.out.format("est zone %d time: %.3fsec%n", zone, zoneTime);
        	return zoneTime;
        }
        
        // helper for averaging together results for staleSim
        // we only generate OK stats for the next cell when we don't overkill this cell, so ignore results where this cell is overkilled
        // same for expected hits to kill this cell: if it's overkilled we don't care, we add no time for the cell
        private void staleAvg(double[] res, double wRes, double[] tmp, double wTmp) {
        	double freshTmp = res[5] * wRes + tmp[5] * wTmp; // freshness is weighted normally since it applies to all cases

        	// hits are reweighted by the likelihood of NOT overkilling the current cell - if we OK we just count zero time
    		
        	// reweight by freshness
        	wRes *= 1 - res[5];
        	wTmp *= 1 - tmp[5];
        	double total = wRes + wTmp;
        	if (total > 0) {
        		wRes /= total;
        		wTmp /= total;
        	} else if (freshTmp < 1) {
        		System.out.format("fresh=%.3f wRes=%.3f wTmp=%.3f" + "%n", freshTmp, wRes, wTmp);
        		throw new Error("We don't expect staleAvg to produce garbage results unless the next cell is guaranteed to be fresh.");
        	}
        	
    		weightedAvg(res, wRes, tmp, wTmp, 5);
    		res[5] = freshTmp;
        }
        
        // simulate results for a stale cell: apply overkill damage, then generate stats if not dead
        private double[] staleSim(double pCorr, double hpC, double hpN, double[] stats) {
        	double[] res = { 0, 0, 0, 0, 0, 0 };
        	double[] tmp = { 0, 0, 0, 0, 0, 0 };
        	final double[] ok = { 0, 0, 0, 0, 0, 1 }; // constant to use when we OK a cell;
        	
        	// improb
        	if (pCorr == 1d) {
        		// last hit was normal
        		if (hpC * 6 - stats[1] <= 0) {
        			System.arraycopy(ok, 0, res, 0, 6);
        		} else {
        			res[5] = 0;
        			System.arraycopy(calcEH(hpC * 6 - stats[1]), 0, res, 0, 5);
        		}
        		// last hit was crit
        		if (hpC * 6 - stats[3] <= 0) {
        			System.arraycopy(ok, 0, tmp, 0, 6);
        		} else {
        			tmp[5] = 0;
        			System.arraycopy(calcEH(hpC * 6 - stats[3]), 0, tmp, 0, 5);
        		}
        		// avg together results for crit & noncrit
        		staleAvg(res, stats[2], tmp, stats[4]);
        		return res;
        	}
        	
        	// normal hits
        	
        	// tough
        	if (hpC * 5 - stats[1] <= 0) {
        		System.arraycopy(ok, 0, res, 0, 6);
        	} else {
        		res[5] = 0;
        		System.arraycopy(calcEH(hpC * 6 - stats[1]), 0, res, 0, 5);
        	}
        	// nontough corrupt
        	if (hpC - stats[1] <= 0) {
        		System.arraycopy(ok, 0, tmp, 0, 6);
        	} else {
        		tmp[5] = 0;
        		System.arraycopy(calcEH(hpC - stats[1]), 0, tmp, 0, 5);
        		tmp[0] *= 1 + expectedDodges / 5d;
        	}
        	staleAvg(res, 1/6d, tmp, 5/6d);
        	// normal enemy
        	if (hpN - stats[1] <= 0) {
        		System.arraycopy(ok, 0, tmp, 0, 6);
        	} else {
        		tmp[5] = 0;
        		System.arraycopy(calcEH(hpN - stats[1]), 0, tmp, 0, 5);
        	}
        	staleAvg(res, pCorr, tmp, (1 - pCorr));
        	

        	// crits

        	double[] cRes = { 0, 0, 0, 0, 0, 0 };
        	
        	// tough
        	if (hpC * 5 - stats[2] <= 0) {
        		System.arraycopy(ok, 0, cRes, 0, 6);
        	} else {
        		cRes[5] = 0;
        		System.arraycopy(calcEH(hpC * 5 - stats[2]), 0, cRes, 0, 5);
        	}
        	// nontough corrupt
        	if (hpC - stats[2] <= 0) {
        		System.arraycopy(ok, 0, tmp, 0, 6);
        	} else {
        		tmp[5] = 0;
        		System.arraycopy(calcEH(hpC - stats[2]), 0, tmp, 0, 5);
        		tmp[0] *= 1 + expectedDodges / 5d;
        	}
        	staleAvg(cRes, 1/6d, tmp, 5/6d);
        	// normal enemy
        	if (hpN - stats[2] <= 0) {
        		System.arraycopy(ok, 0, tmp, 0, 6);
        	} else {
        		tmp[5] = 0;
        		System.arraycopy(calcEH(hpN - stats[2]), 0, tmp, 0, 5);
        	}
        	staleAvg(cRes, pCorr, tmp, (1 - pCorr));
        	
        	staleAvg(res, stats[3], cRes, stats[4]);
        	
        	return res;
        }
        
        // simulate results for a fresh cell
        private double[] freshSim(double pCorr, double hpC, double hpN) {
        	double[] res = { 0, 0, 0, 0, 0 };
        	double[] tmp = { 0, 0, 0, 0, 0 };
        	if (pCorr == 1d) { //improb
        		res = calcEH(hpC * 6);
        	} else {
        		// tough imp
        		res = calcEH(hpC * 5);
        		// nontough corrupted imp
        		tmp = calcEH(hpC);
        		// add dodges for 1/5 of non-tough corrupted imps
        		tmp[0] *= 1 + expectedDodges / 5d;
        		// average tough with nontough
        		weightedAvg(res, 1/6d, tmp, 5/6d, 5);
        		// average with normal imp
        		tmp = calcEH(hpN);
        		weightedAvg(res, pCorr, tmp, (1 - pCorr), 5);
        	}
        	return res;
        }        

        private int getNumCorrupt() {
        	return Math.min(80, Math.max(0, (int) ((zone - corruptionStart) / 3) + 2));
        }
        
    private double runZone(final double damageFactor) {
        EnemyType[] zoneArray = createZone(Math.min(80,
                Math.max(0, ((int) ((zone - corruptionStart) / 3)) + 2)));
        double res = 0;
        int cell = 1;
        Random random = new Random();
        double hp = getHPModifier(cell, zoneArray[cell - 1])*getHPFactor(zoneArray[cell - 1],random.nextDouble());
        while (cell <= 100) {
            boolean crit = random.nextDouble() < critChance;
            double damage = (crit) ? damageFactor * critDamage : damageFactor;
            damage *= (1 + 0.2 * random.nextDouble());
            boolean dodge = zoneArray[cell - 1] == EnemyType.AGILITY
                    && random.nextDouble() < 0.3;
            if (dodge) {
                res += attackDelay;
                continue;
            }
            if (damage >= hp) {
                cell++;
                damage -= hp;
                double overkillDamage = damage * okFactor;
                if (cell == 101) {
                    res += cellDelay;
                    break;
                } else {
                    hp = getHPModifier(cell, zoneArray[cell - 1])*getHPFactor(zoneArray[cell - 1],random.nextDouble());
                }
                hp -= overkillDamage;
                if (hp <= 0) {
                    cell++;
                    if (cell == 101) {
                        res += cellDelay;
                        break;
                    } else {
                        res += cellDelay;
                        hp = getHPModifier(cell, zoneArray[cell - 1])*getHPFactor(zoneArray[cell - 1],random.nextDouble());
                    }
                } else {
                    res += cellDelay;
                }
            } else {
                res += attackDelay;
                hp -= damage;
            }
        }
        return res;
    }
    //TODO do normal remove better
    private double getHPModifier(final double pCell, final EnemyType enemyType) {
        // TODO properly implement
        if (enemyType == EnemyType.NORMAL) {
            return 1d/corruptMod;
        }
        double cellMod = (0.5 + 0.8 * (pCell / 100)) / 0.508;
        if (pCell < 100) {
            return cellMod * (enemyType == EnemyType.TOUGH ? 5 : 1);
        } else {
            return cellMod * 6;
        }
    }
    //TODO look over and optimize
    private double getHPFactor(final EnemyType enemyType, final double random) {
        if (enemyType == EnemyType.COORUPTED) {
            return 1.0;
        }
        double r = random;
        double mainImpProb = (1 - 0.004666666 - 0.15) / 8;
        if (r < 0.004666666) {
            return 1.6;
        }
        r -= 0.004666666;
        if (r < 0.15) {
            return 1;
        }
        r -= 0.15;
        if (r < mainImpProb * 2) {
            return 0.7;
        }
        r -= mainImpProb * 2;
        if (r < mainImpProb * 2) {
            return 1.3;
        }
        r -= mainImpProb * 2;
        if (r < mainImpProb) {
            return 1;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 0.8;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 1.1;
        }
        r -= mainImpProb;
        if (r < mainImpProb) {
            return 1.5;
        }
        r -= mainImpProb;
        return 1;
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

    private enum EnemyType {
        NORMAL, COORUPTED, TOUGH, AGILITY, IMPROBABILITY;
    }

}
