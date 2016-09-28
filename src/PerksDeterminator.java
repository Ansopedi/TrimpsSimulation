import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerksDeterminator {
    private Perks perks;

    public static void main(String[] args) {
        // TODO fix all 0 bug
        //int[] perkArray = new int[] { 80, 80, 80, 90, 40000, 20000, 9000, 27000,
        //        59, 80, 44 };
    	//int[] perkArray = new int[] { 94,92,93,103,99800,64200,22100,83500,60,89,45 };
    	int[] perkArray = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        double totalHelium = 57300000000000d;
        // TODO check for non-bought ones
        Perks perks = new Perks(perkArray, totalHelium);
        ProbabilisticZoneModel zS =
        		new ProbabilisticZoneModel(
        				TrimpsSimulation.critChance,
        				TrimpsSimulation.critDamage,
        				TrimpsSimulation.okFactor);
        double bestHeHr = 0;
        double heHr = 0;
        int[] bestPerks = Arrays.copyOf(perkArray, perkArray.length);
        long startTime = System.nanoTime();
        boolean fineTune = false;
        int keepTrying = 1;
        do {
        	bestHeHr = Math.max(heHr, bestHeHr);
	    	TrimpsSimulation tS = new TrimpsSimulation(perks.getTSFactors(), false, zS);
	    	SimulationResult sR = tS.runSimulation();
	    	heHr = sR.helium / sR.hours;
	    	System.out.format("baseline %5e he/hr with perks: %s%n", heHr, Arrays.toString(perks.getPerkLevels()));
        	if (heHr > bestHeHr) {
        		bestHeHr = heHr;
        		System.arraycopy(perks.getPerkLevels(), 0, bestPerks, 0, perkArray.length);
        		keepTrying = 1;
        	} else if (!fineTune) {
        		fineTune = true;
        		perks = new Perks(bestPerks, totalHelium);
        	} else if (keepTrying-- <= 0) {
        		break;
        	}
	    	//long time = System.nanoTime();
	    	double[][] rawEffs = calcPerkEfficiencies(perks, zS, sR, 1);
	    	//long time2 = System.nanoTime();
	    	//System.out.println((time2-time)/1000000l + "ms to run sims");
	    	// 
	        perks.permutePerks(rawEffs, fineTune);
	    	//System.out.println((System.nanoTime() - time2) / 1000000l + "ms to run permute");
        } while (true);
        System.out.format("best he/hr of %5e with perks: %s%n", bestHeHr, Arrays.toString(bestPerks));
        System.out.println((System.nanoTime() - startTime)/1000000l + "ms to determine perks");
        //perkArray = new int[] { 80, 80, 85, 90, 40000, 15000, 15000, 40000, 50, 82, 41 };
        //perks = new Perks(perkArray, totalHelium));
//        PerksDeterminator pD = new PerksDeterminator(perks);
//        pD.printPerksToFile();
//        Perks result = pD.determinePerks();
//        for (int x = 0; x < Perk.values().length; x++) {
//            System.out.print(Perk.values()[x].name() + " : "
//                    + result.getLevel(Perk.values()[x]));
//        }
    }

    public PerksDeterminator(final Perks perks) {
        this.perks = perks;
    }
    
    public Perks determinePerks() {
        Perks savedPerks = new Perks(perks);
        ZoneSimulation zS = new ProbabilisticZoneModel(
                TrimpsSimulation.critChance, TrimpsSimulation.critDamage,
                TrimpsSimulation.okFactor);
        double[] tsFactors = perks.getTSFactors();
        TrimpsSimulation tS = new TrimpsSimulation(tsFactors, false, zS);
        SimulationResult prev = tS.runSimulation();
        while (true) {
            long time = System.nanoTime();
            int bestPerk = 0;
            int count = 0;
            double highestRunEfficiency = -10;
            List<SimulationThread> threads = new ArrayList<>();
            for (Perk p : Perk.values()) {
                for (int i : p.levelIncreases) {
                    Perks usePerks = new Perks(savedPerks);
                    if (usePerks.buyPerk(p, i)) {
                        SimulationThread sT = new SimulationThread(usePerks, savedPerks,
                                count,i, zS, prev);
                        threads.add(sT);
                    }
                }
                count++;
            }
            for (SimulationThread sT : threads) {
                sT.start();
            }
            int bestBuyAmount = 0;
            for (int x = 0; x < threads.size(); x++) {
                try {
                    threads.get(x).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                double runEfficiency = threads.get(x).getRelativeEfficiency();
                System.out.println(runEfficiency);
                int perkPosition = threads.get(x).perkPosition;
                if (runEfficiency > highestRunEfficiency) {
                    prev = threads.get(x).sR;
                    bestBuyAmount= threads.get(x).perkIncrease;
                    highestRunEfficiency = runEfficiency;
                    bestPerk = perkPosition;
                }
            }
            System.out.println((System.nanoTime() - time) / 1000000);
            if (highestRunEfficiency > 0) {
                savedPerks.buyPerk(Perk.values()[bestPerk],
                        bestBuyAmount);
                perks = savedPerks;
                printPerksToFile();
            } else {
                break;
            }
        }
        return perks;
    }
    
    // TODO: allow for finer-grain testing with lower multis (perhaps to fine-tune perks at the end)
    // -but do note there can be butterfly effects (related to specific prestige buys late in the run
    //  that may result in insufficient fidelity with small effect sizes
    private static double[][] calcPerkEfficiencies( Perks perks, ZoneSimulation zS,
    		SimulationResult resBase, int debug) {
    	double[] tsFactors = perks.getTSFactors();
    	double[] testFactors = new double[Perks.numTSFactors];
    	double hehrBase = resBase.getHehr();
    	double[][] res = new double[2][Perks.numTSFactors];
    	TrimpsSimulation tS;
    	for ( Perks.tsFactor f : Perks.tsFactor.values() ) {
    		double adjust = f.testEffect;
    		testFactors = Arrays.copyOf(tsFactors, Perks.numTSFactors);
    		SimulationResult plusTest = resBase;
    		SimulationResult minusTest;
    		if (adjust == 1) { // test point by point instead of changing the stat directly
    			boolean soldPoint = false;
    			do {
    				testFactors[f.ordinal()] = Perks.calcCoordFactor(perks.getLevel(f.base) - 1);
    				tS = new TrimpsSimulation(testFactors, false, zS);
    				minusTest = tS.runSimulation(debug-1);
    				if (minusTest.getHehr() == hehrBase) {
        				// loop testing -1 point and selling a point if it had no effect
    					soldPoint = perks.buyPerk(Perk.COORDINATED, -1);
    				} else if (!soldPoint) {
    					// or if -1 point has an effect, test +1 point also
    					testFactors[f.ordinal()] = Perks.calcCoordFactor(perks.getLevel(f.base) + 1);
    					tS = new TrimpsSimulation(testFactors, false, zS);
    					plusTest = tS.runSimulation(debug-1);
    				} else {
    					// if we sold any points, we know the next point has no effect
    					soldPoint = false;
    				}
    			} while (soldPoint);
    			// for point by point perks, just return hehr factors for +1 and -1
    			res[0][f.ordinal()] = plusTest.getHehr() / hehrBase;
    			res[1][f.ordinal()] = hehrBase / minusTest.getHehr();
    		} else {
    			// TODO the perks efficiency calculation should assume no further efficiency change beyond the effect size
    			double plusEff;
    			double minusEff;
    			double logBase;
    			while (true) {
    				testFactors[f.ordinal()] = tsFactors[f.ordinal()] * adjust;
    				tS = new TrimpsSimulation(testFactors, false, zS);
    				plusTest = tS.runSimulation(debug-1);
    				testFactors[f.ordinal()] = tsFactors[f.ordinal()] / adjust;
    				tS = new TrimpsSimulation(testFactors, false, zS);
    				minusTest = tS.runSimulation(debug-1);
    				logBase = Math.abs(Math.log(adjust));
    				plusEff = Math.log(plusTest.getHehr() / hehrBase) / logBase;
    				minusEff = Math.log(hehrBase / minusTest.getHehr()) / logBase;
    				if (plusEff > 0 || minusEff > 0) {
    					// plusEff floor is 0 - possible to have no effect, but assume negative effect is not real (butterfly)
    					plusEff = plusEff < 0 ? 0 : plusEff;
    					// we can't handle minusEff of zero, so if it was swamped by butterfly effect just use plusEff
    					minusEff = minusEff <= 0 ? plusEff : minusEff;
    					break;
    				} else {
    					// if there was no measurable effect, try a bigger effect size
    					adjust *= adjust;
    				}
    			}
    			// interpolated efficiency at baseline
    			res[0][f.ordinal()] = (plusEff + minusEff) / 2d;
    			// slope of efficiency curve between max/min adjusted effects,
    			// 	normalized to the testEffect range (since we may end up clamping to this range)
    			// -> at /adjust, efficiency is (baseEff - slope)
    			// -> at *adjust, efficiency is (baseEff + slope)
    			res[1][f.ordinal()] = (plusEff - minusEff) / 2d / logBase * Math.abs(Math.log(f.testEffect));
    		}	
    	}
    	
    	System.out.format("perks=%s%nbaseEff=%s%nslopeEff=%s%n",
    			Arrays.toString(perks.getPerkLevels()), Arrays.toString(res[0]), Arrays.toString(res[1]));
    	return res;
    }

    private void comparePow() {
        Perks perks = new Perks(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                1000000000000000000d);
        int pow1 = 0;
        int pow2 = 0;
        double comCost = 0;
        while (pow2 <= 100000) {
            double pow1eff = (1 + (pow1 + 1) * 0.05) * (1 + 0.01 * pow2)
                    / (comCost + perks.perkCost(Perk.POWER, 1));
            double pow2eff = (1 + pow1 * 0.05) * (1 + 0.01 * (pow2 + 1))
                    / (comCost + perks.perkCost(Perk.POWER2, 1));
            if (pow1eff > pow2eff) {
                comCost += perks.perkCost(Perk.POWER, 1);
                perks.buyPerk(Perk.POWER, 1);
                pow1++;
                System.out.println(pow2);
            } else {
                comCost += perks.perkCost(Perk.POWER2, 1);
                perks.buyPerk(Perk.POWER2, 1);
                pow2++;
            }
        }
    }

    private void printPerksToFile() {
        PrintWriter writer = null;
        ;
        try {
            writer = new PrintWriter(
                    System.getProperty("user.home") + "/Desktop/" + "perks.txt",
                    "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StringBuilder sB = new StringBuilder();
        sB.append("{");
        for (int x = 0; x < Perk.values().length; x++) {
            writer.println(Perk.values()[x].name() + " : "
                    + perks.getLevel(Perk.values()[x]));
            sB.append(perks.getLevel(Perk.values()[x]));
            sB.append(",");

        }
        sB.deleteCharAt(sB.length() - 1);
        sB.append("}");
        writer.println(sB.toString());
        writer.close();
    }
    
    private static double logOfBase(double base, double num) {
        return Math.log(num) / Math.log(base);
    }

    public class SimulationThread extends Thread {

        private Perks perks;
        private Perks prevPerks;
        private int perkPosition;
        private int perkIncrease;
        private SimulationResult sR;
        private SimulationResult prev;
        private ZoneSimulation zS;

        public SimulationThread(final Perks perks, final Perks prevPerks,
        		final int perkPosition,
                final int perkIncrease, final ZoneSimulation zS,
                final SimulationResult prev) {
            this.perks = perks;
            this.prevPerks = prevPerks;
            this.perkPosition = perkPosition;
            this.perkIncrease = perkIncrease;
            this.zS = zS;
            this.prev = prev;
        }

        public void run() {
            TrimpsSimulation tS = new TrimpsSimulation(perks.getTSFactors(), false, zS);
            sR = tS.runSimulation();
        }

        private double getRelativeEfficiency() {
            return logOfBase(
                    perks.getSpentHelium() / prevPerks.getSpentHelium(),
                    getRunEfficiency(sR) / getRunEfficiency(prev));
        }

        private double getRunEfficiency(final SimulationResult sR) {
            return sR.helium / sR.hours;
        }

    }
}
