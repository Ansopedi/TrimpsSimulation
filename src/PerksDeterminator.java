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
    	int[] perkArray = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        double totalHelium = 24900000000000d;
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
        do {
        	bestHeHr = Math.max(heHr, bestHeHr);
	    	TrimpsSimulation tS = new TrimpsSimulation(perks.getTSFactors(), false, zS);
	    	SimulationResult sR = tS.runSimulation();
	    	heHr = sR.helium / sR.hours;
	    	System.out.format("baseline %5e he/hr with perks: %s%n", heHr, Arrays.toString(perks.getPerkLevels()));
        	if (heHr > bestHeHr) {
        		bestHeHr = heHr;
        		System.arraycopy(perks.getPerkLevels(), 0, bestPerks, 0, perkArray.length);
        	} else { break; }
	    	//long time = System.nanoTime();
	    	double[] rawEffs = calcPerkEfficiencies(perks, zS, sR);
	    	//long time2 = System.nanoTime();
	    	//System.out.println((time2-time)/1000000l + "ms to run sims");
	        perks.permutePerks(rawEffs);
	    	//System.out.println((System.nanoTime() - time2) / 1000000l + "ms to run permute");
        } while (true);
        System.out.format("best he/hr of %5e with perks: %s%n", bestHeHr, Arrays.toString(bestPerks));
        System.out.println((System.nanoTime() - startTime)/1000000l + "ms to determine perks");
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
    private static double[] calcPerkEfficiencies( Perks perks, ZoneSimulation zS, SimulationResult resBase ) {
    	double[] tsFactors = perks.getTSFactors();
    	double[] testFactors = new double[Perks.numTSFactors];
    	double hehrBase = resBase.getHehr();
    	double[] simEffs = new double[Perks.numTSFactors];
    	TrimpsSimulation tS;
    	for (Perks.tsFactor f : Perks.tsFactor.values()) {
    		double multi = 1;
    		testFactors = Arrays.copyOf(tsFactors, Perks.numTSFactors);
    		int i = f.ordinal();
    		switch (f) {
    		case POWER:
    		case MOTIVATION:
    		case LOOTING:
    			multi = 1.2;
    			break;
    		case CARPENTRY:
    			multi = 1.1;
    			break;
    		case COORDINATED:
    			testFactors[i] = Perks.calcCoordFactor(perks.getLevel(Perk.COORDINATED) + 1);
    			break;
    		case ARTISANISTRY:
    			multi = 0.8;
    			break;
    		case RESOURCEFUL:
    			multi = 0.5;
    			break;
    		}
    		testFactors[i] *= multi;
    		tS = new TrimpsSimulation(testFactors, false, zS);
    		SimulationResult resTest = tS.runSimulation();
    		if (multi == 1) {
    			// for coord we just calculate the he/hr gain of 1 additional point
    			simEffs[i] = resTest.getHehr() / hehrBase;
    		} else if (multi > 1) {
    			simEffs[i] = logOfBase( multi, resTest.getHehr() / hehrBase );
    		} else {
    			simEffs[i] = logOfBase( 1/multi, resTest.getHehr() / hehrBase );
    		}
    	}
    	double[] res = new double[Perk.values().length];
    	for ( Perk p : Perk.values()) {
    		res[p.ordinal()] = simEffs[p.tsFactor.ordinal()];
    	}
    	//System.out.format("perks=%s%neff=%s%n", Arrays.toString(perks.getPerkLevels()), Arrays.toString(res));
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
