
public class ProbabilisticZoneSimulation extends ZoneSimulation {
	
	private static double critChance;
	private static double critDamage;
	private static double okFactor;
	
	// Expected Hits table - holds static stats for the expected result of a cell with a given relative HP (compared to trimp attack)
	
	// geometric increment for expHits indices
    private final static double ehInc = 1.005;
    // top end of expHits table - should be high enough that oscillations in Pn and Pc have damped out
    private final static double ehMaxFactor = 200;
    private static double ehMax;
    // length of expHits table - just the number of indices required to get to ehMax
    private static int ehLength;
    // progress in building the expHits table - for smaller values we pull from the existing table instead of recursing
    private static double ehProgress;
    // expHits, expOK, expOKc, expPn, expPc
    // expected hits to kill
    // expected OK damage to apply to next cell if finishing with a noncrit
    // expected OK damage to apply to next cell if finishing with a crit
    // chance last hit to kill is a noncrit
    // chance last hit to kill is a crit
    private static double[] expHits;
    private static double[] expOK;
    private static double[] expOKc;
    private static double[] expPn;
    private static double[] expPc;
    
    // expected dodges per hit for dodge imps
    private final static int dodgeLength = 20;
    private static double expectedDodges;
    
    // average trimp damage per hit
    private static double damagePerHit;
    
    // crit chance & damage;
    
    public ProbabilisticZoneSimulation(final double pCrit, final double dCrit, final double okF) {
    	critChance = pCrit;
    	critDamage = dCrit;
    	okFactor = okF;
    	
    	damagePerHit = (critDamage * critChance) + (1 - critChance);
    	expectedDodges = calculateExpectedDodges(dodgeLength);
    	ehMax = ehMaxFactor * critDamage;
    	ehLength = getEHidx(ehMax) + 1;
    	ehProgress = 0;
    	
    	expHits = new double[ehLength];
    	expOK = new double[ehLength];
    	expOKc = new double[ehLength];
    	expPn = new double[ehLength];
    	expPc = new double[ehLength];
    	
        buildExpectedHits(ehMax, ehInc);
    }
    
    // get the expHits table index corresponding to a given relative imp HP
    private static int getEHidx(double hp) {
        return (int) (Math.log(hp * Math.pow(ehInc, .01)) / Math.log(ehInc));
    }

    // utility method for compiling a weighted average of two arrays
    private static void weightedAvg(double[] res, double fRes, double[] tmp,
            double fTmp, int length) {
        for (int i = 0; i < length; i++) {
            res[i] = res[i] * fRes + tmp[i] * fTmp;
        }
    }
    
    // calculate the expected # of dodges per hit against a dodge imp
    private static double calculateExpectedDodges(final int length) {
        double res = 0;
        double cumChance = 0.7;
        for (int hits = 1; hits < length; hits++) {
            // System.out.println(res + " expected dodges for hit " + (hits-1));
            cumChance *= 0.3;
            res += cumChance;
        }
        return res;
    }

    // build the expHits table that holds stats for quickly getting calcEH values during zone sims
    private static void buildExpectedHits(double max, double inc) {
        for (double hp = 1; hp < max; hp *= inc) {
            int i = getEHidx(hp);
            double tmp[] = calcEH(hp);
            expHits[i] = tmp[0];
            expOK[i] = tmp[1];
            expOKc[i] = tmp[2];
            expPn[i] = tmp[3];
            expPc[i] = tmp[4];
            ehProgress = hp;
            // System.out.format("hp=%.3f i=%d eH=%.2f eOK=%.3f eOKc=%.2f
            // ePn=%.3f eCn=%.3f%n", hp, i, tmp[0], tmp[1], tmp[2], tmp[3],
            // tmp[4]);
        }
        ehMax = ehProgress;
    }
    
    private static double[] calcEH(double hp) {
        double[] res = { 0, 0, 0, 0, 0 }; // hits, okdmg on hit, okdmg on crit
        // if we always kill, actually calculate the results (best accuracy this
        // way compared to building even a very extensive table for values from
        // 0 to 1)
        if (hp < 1) {
            res[0] = 1;
            res[1] = (1.1 - hp) * okFactor;
            res[2] = (critDamage * 1.1 - hp) * okFactor;
            res[3] = 1 - critChance;
            res[4] = critChance;
            return res;
        } else if (hp > ehMax) { // beyond the table, expHits can be calculated
                                 // to high accuracy and other values assumed to
                                 // be stable
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

        // do noncrit stuff - note we already know hp >= 1 since otherwise we
        // directly calculate and return a result
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

    
	@Override
	public double getExpectedTime(double cellDelay, double attackDelay, double damageFactor, double critChance,
			double critDamage, double okFactor, double corruptMod, int corruptionStart, int zone) {
        double nCorrupt = (double) getNumCorrupt(zone, corruptionStart);
        double corrRows = Math.min(10, Math.ceil(nCorrupt / 6d));
        double baseHp = 1 / (.508 * damageFactor);
        double pCorr = nCorrupt / Math.min(corrRows * 10d, 99);

        // expHits, expOK, expOKc, expPn, expPc, freshness (chance of a fresh
        // cell, i.e. first cell of zone or previous cell was OKd)
        double[] cellStats = { 0, 0, 0, 0, 0, 1 };
        double[] tmp = { 0, 0, 0, 0, 0, 0 };

        double zoneTime = 0;

        for (int cell = 1; cell <= 100; cell++) {
            double hpC = baseHp * (.5 + .8 * cell / 100d);
            // TODO model normal HP enemies during Corrupted challenge? may also
            // need this in getEnemyHealth?
            double hpN = hpC * 1.2 / corruptMod;
            double pCorrFinal = (cell == 100) ? 1
                    : (Math.ceil(cell / 10d) <= corrRows) ? pCorr : 0;
            double freshness = cellStats[5]; // this cell's freshness is used to
                                             // weight the results between the
                                             // staleSim and freshSim

            //System.out.format("zone %d, cell %d%n", zone, cell);
            
            // stats if cell is stale (including freshness of next cell)
            if (freshness < 1) {
                cellStats = staleSim(pCorrFinal, hpC, hpN, cellStats);
            }

            // stats if cell is fresh
            if (freshness > 0) {
                System.arraycopy(freshSim(pCorrFinal, hpC, hpN), 0, tmp, 0, 5);
            }
            tmp[5] = 0; // next cell can't be fresh if this one was fresh
                        // (except in the negligible event that we exactly kill
                        // the cell with no OK dmg)

            // avg together stale and fresh stats
            staleAvg(cellStats, (1 - freshness), tmp, freshness);

            // if next cell is fresh, this cell adds no time (because it was
            // overkilled)
            // else add celltime for the first hit, and hittime for each
            // subsequent hit
            double cellTime = (1 - cellStats[5])
                    * (cellDelay + (cellStats[0] - 1) * attackDelay);
            zoneTime += cellTime;
            // System.out.format("cell %d stats: time=%.2f hpC=%.3f hpN=%.3f
            // eH=%.2f eOK=%.3f pN=%.3f eOKc=%.2f pC=%.2f fresh=%.2f" + "%n",
            // cell, cellTime, hpC, hpN, cellStats[0], cellStats[1],
            // cellStats[3], cellStats[2], cellStats[4], cellStats[5]);
        }
        // System.out.format("est zone %d time: %.3fsec%n", zone, zoneTime);
        return zoneTime;
	}
	
    // helper for averaging together results for staleSim
    // we only generate OK stats for the next cell when we don't overkill this
    // cell, so ignore results where this cell is overkilled
    // same for expected hits to kill this cell: if it's overkilled we don't
    // care, we add no time for the cell
    private void staleAvg(double[] res, double wRes, double[] tmp,
            double wTmp) {
        double freshTmp = res[5] * wRes + tmp[5] * wTmp; // freshness is
                                                         // weighted normally
                                                         // since it applies to
                                                         // all cases

        //System.out.format("res5=%.3f wRes=%.3f tmp5=%.3f wTmp=%.3f%n", res[5], wRes, tmp[5], wTmp);

        // reweight by freshness
        wRes *= 1 - res[5];
        wTmp *= 1 - tmp[5];
        double total = wRes + wTmp;
        if (total > 0) {
            wRes /= total;
            wTmp /= total;
        } else if (freshTmp < 0.999999) {
            System.out.format("fresh=%.3f wRes=%.3f wTmp=%.3f total=%.3f" + "%n", freshTmp,
                    wRes, wTmp, total);
            throw new Error(
                    "We don't expect staleAvg to produce garbage results unless the next cell is guaranteed to be fresh.");
        }

        weightedAvg(res, wRes, tmp, wTmp, 5);
        res[5] = freshTmp;
    }
	
    // simulate results for a stale cell: apply overkill damage, then generate
    // stats if not dead
    private double[] staleSim(double pCorr, double hpC, double hpN,
            double[] stats) {
        double[] res = { 0, 0, 0, 0, 0, 0 };
        double[] tmp = { 0, 0, 0, 0, 0, 0 };
        final double[] ok = { 0, 0, 0, 0, 0, 1 }; // constant to use when we OK
                                                  // a cell;

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
        staleAvg(res, 1 / 6d, tmp, 5 / 6d);
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
        staleAvg(cRes, 1 / 6d, tmp, 5 / 6d);
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
        if (pCorr == 1d) { // improb
            res = calcEH(hpC * 6);
        } else {
            // tough imp
            res = calcEH(hpC * 5);
            // nontough corrupted imp
            tmp = calcEH(hpC);
            // add dodges for 1/5 of non-tough corrupted imps
            tmp[0] *= 1 + expectedDodges / 5d;
            // average tough with nontough
            weightedAvg(res, 1 / 6d, tmp, 5 / 6d, 5);
            // average with normal imp
            tmp = calcEH(hpN);
            weightedAvg(res, pCorr, tmp, (1 - pCorr), 5);
        }
        return res;
    }

    private int getNumCorrupt(int zone, int corruptionStart) {
        return Math.min(80,
                Math.max(0, (int) ((zone - corruptionStart) / 3) + 2));
    }

}
