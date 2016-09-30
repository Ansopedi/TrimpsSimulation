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
    	//int[] perkArray = new int[] {96,94,93,105,131600,86400,27400,103100,61,92,43};
    	int[] perkArray = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        double totalHelium = 78099000000000d;
        // TODO check for non-bought ones
        Perks perks = new Perks(perkArray, totalHelium);
        PerksDeterminator pD = new PerksDeterminator(perks);
        pD.printPerksToFile();
        Perks result = pD.determinePerksPermute();
        for (int x = 0; x < Perk.values().length; x++) {
            System.out.print(Perk.values()[x].name() + " : "
                    + result.getLevel(Perk.values()[x]) + " ");
        }
    }

    public PerksDeterminator(final Perks perks) {
        this.perks = perks;
    }
    
    public Perks determinePerksPermute() {
    	ProbabilisticZoneModel zS =
        		new ProbabilisticZoneModel(
        				TrimpsSimulation.critChance,
        				TrimpsSimulation.critDamage,
        				TrimpsSimulation.okFactor);
        double bestHeHr = 0;
        double heHr = 0;
        Perks bestPerks = new Perks(perks);
        Perks dpPerks = new Perks(perks);
        long startTime = System.nanoTime();
        boolean fineTune = false;
        int keepTrying = 3;
        do {
        	bestHeHr = Math.max(heHr, bestHeHr);
	    	TrimpsSimulation tS = new TrimpsSimulation(dpPerks.getTSFactors(), false, zS);
	    	SimulationResult sR = tS.runSimulation();
	    	heHr = sR.helium / sR.hours;
	    	System.out.format("baseline %5e he/hr with perks: %s%n", heHr, Arrays.toString(dpPerks.getPerkLevels()));
        	if (heHr > bestHeHr) {
        		bestHeHr = heHr;
        		bestPerks = new Perks(dpPerks);
        		keepTrying = 3;
        	} else if (!fineTune) {
        		fineTune = true;
        		dpPerks = new Perks(bestPerks);
        	} else if (keepTrying-- <= 0) {
        		break;
        	}
	    	//long time = System.nanoTime();
	    	double[][] rawEffs = calcPerkEfficiencies(dpPerks, zS, sR, fineTune, 1);
	    	//long time2 = System.nanoTime();
	    	//System.out.println((time2-time)/1000000l + "ms to run sims");
	    	// 
	        dpPerks.permutePerks(rawEffs, fineTune);
	    	//System.out.println((System.nanoTime() - time2) / 1000000l + "ms to run permute");
        } while (true);
        System.out.format("best he/hr of %5e with perks: %s%n", bestHeHr, Arrays.toString(bestPerks.getPerkLevels()));
        System.out.println((System.nanoTime() - startTime)/1000000l + "ms to determine perks");
        return new Perks(bestPerks);
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
    		SimulationResult resBase, boolean fineTune, int debug) {
    	double[] tsFactors = perks.getTSFactors();
    	double[] testFactors = Arrays.copyOf(tsFactors, Perks.numTSFactors);
    	double hehrBase = resBase.getHehr();
    	double[][] res = new double[3][Perks.numTSFactors];
    	TrimpsSimulation tS;
    	
		SimulationResult nextRes = resBase;
		SimulationResult curRes = resBase;
		SimulationResult lastRes = resBase;
		// loop selling coord until both the last point and the next point have value
		while (nextRes.getHehr() <= curRes.getHehr() || curRes.getHehr() <= lastRes.getHehr()) {
			
			nextRes = curRes;
			curRes = lastRes;
			
			testFactors[Perks.tsFactor.COORDINATED.ordinal()]
					= Perks.calcCoordFactor(perks.getLevel(Perk.COORDINATED) - 1);
			tS = new TrimpsSimulation(testFactors, false, zS);
			lastRes = tS.runSimulation(debug-1);
			
			if (lastRes.getHehr() < curRes.getHehr()) {
				if (nextRes == resBase) {
					// still need to test +1 point if -1 point had an effect on the first try
					testFactors[Perks.tsFactor.COORDINATED.ordinal()]
							= Perks.calcCoordFactor(perks.getLevel(Perk.COORDINATED) + 1);
					tS = new TrimpsSimulation(testFactors, false, zS);
					nextRes = tS.runSimulation(debug-1);
					if (nextRes.getHehr() <= curRes.getHehr()) {
						// next point has no effect, sell one and try again
						perks.buyPerk(Perk.COORDINATED, -1);
					} else {
						// we're done, will exit the loop (lastres<curres, and nextres>curRes)
					}
				} else if (nextRes.getHehr() <= curRes.getHehr()) {
					perks.buyPerk(Perk.COORDINATED, -1);
				} else {
					// we're done if both points had effect
				}
			} else {
				// last point had no effect, sell one and try again
				perks.buyPerk(Perk.COORDINATED, -1);
			}
		}
		resBase = curRes;
		hehrBase = resBase.getHehr();
		tsFactors = perks.getTSFactors();
		
		// now we are at a coord level where the last point and next point have value
		
		double coordNextGain = nextRes.getHehr() / hehrBase;
		double coordLastGain = hehrBase / lastRes.getHehr();
		
		// TODO: trying to eliminate the next-point-of-coord-is-worthless stuff
		// these may be used in cases where coordNextGain == 1 so that coord needs a special derivation
		double carpNextGain = 0;
		double carpLastGain = 0;
		double Tcarp = 0;
		
		// For any given tsFactor ("stat"), we simulate *testEffect and /testEffect. (Call testEffect "T".)
		// -> Really we may use powers of T if necessary to notice an effect, and pass the final adjustment as a result.
		// Then we use these simulation results to get parameters for the following model:
		//
		// Let H(X,Y) = <hehr at stat X*Y> / <hehr at stat X>,
		// 	where X is the stat gain factor relative to baseline perks.
		// Now for an infinitesimal gain 'e' (i.e. limit as e approaches 1 from above), we have:
		// 	H(X,e) = e^G(X) for some function G, which we could call the "local helium gain exponent".
		// We model G(X) as linear in the log of X:
		//	G(X) = A + B * log(X,T)
		// Skipping over the long derivation, we can calculate total helium gain as follows:
		// 	H(X,Y) = Y^(A + B * log(X,T) + (B/2) * log(Y,T))
		// We aim to calculate A and B for each perk, knowing the values of:
		// H(1/T,T) = hehrBase / lastRes.getHehr();
		// H(1,T) = nextRes.getHehr() / hehrBase;
		// With these values A and B, we can calculate H(X,Y) for buying or selling any amount of any perk,
		// 	where X accounts for stat gain redundancies between different perks.
		// e.g. "X" for motivation is really the motivation factor (over baseline) TIMES the carpentry factor.
		
    	for ( Perks.tsFactor f : Perks.tsFactor.values() ) {
    		
    		// coord gain is already calculated 
    		if (f == Perks.tsFactor.COORDINATED) { continue; }
    		
    		double T = f.testEffect;
    		testFactors = Arrays.copyOf(tsFactors, Perks.numTSFactors);
    		double nextGain = 0;
    		double lastGain = 0;
			while (true) {
				// bail out with an error if we haven't found an effect for T = max double
				// -> something is very wrong if we gain no hehr with zillions of times more of a stat
				// -> this could happen for coord but this loop doesn't do coord
				if (T >= Double.MAX_VALUE/f.testEffect/2d) {
					throw new Error("We expect all non-coord perks to have some effect!%n");
				}
				
				testFactors[f.ordinal()] = tsFactors[f.ordinal()] * T;
				tS = new TrimpsSimulation(testFactors, false, zS);
				nextRes = tS.runSimulation(debug-1);
				testFactors[f.ordinal()] = tsFactors[f.ordinal()] / T;
				tS = new TrimpsSimulation(testFactors, false, zS);
				lastRes = tS.runSimulation(debug-1);

				nextGain = nextRes.getHehr() / hehrBase;
				lastGain = hehrBase / lastRes.getHehr();
				
				// for looting we always get a gain of 1 from the helium alone,
				//	so try a bigger adjust if that's all we got
				double gainComp = f == Perks.tsFactor.LOOTING ? 1 : 0;
				if (nextGain > gainComp && lastGain > gainComp) {
					break;
				} else {
					// if there was no measurable effect, try a bigger effect size
					T *= T;
				}
			}
			
			// the coord derivations need to know about carp gain stats
			if (f == Perks.tsFactor.CARPENTRY) {
				carpNextGain = nextGain;
				carpLastGain = lastGain;
				Tcarp = T;
				// and if the next point of coord is worthless, carp has a totally different derivation
				//	so skip the rest of the normal derivation
				if (coordNextGain == 1) { continue; }
			}
			
			// Skipping derivations again:
			// A = log(sqrt(nextGain*lastGain),T)
			// B = 2 * (A - log(lastGain,T))
			double A = Math.log(Math.sqrt(nextGain * lastGain)) / Math.log(T);
			double B = 2 * ( A - Math.log(lastGain) / Math.log(T) );
			res[0][f.ordinal()] = A;
			res[1][f.ordinal()] = B;
			res[2][f.ordinal()] = T;
			
			// by the model, we should also have B<adjust> = 2 * (log(nextGain,adjust) - A),
			//	so print out the results to check how close the model approaches reality
			double Bprime = 2 * ( Math.log(nextGain) / Math.log(T) - A );
			if (Math.abs(B/Bprime - 1d ) > .1) { 
				System.out.println("WARNING: large divergence between B and B', model may not be so good :(");
				System.out.format("For %s, B=%.2e B'=%.2e%n", f.name(), B, Bprime); 
			}
    	}
    	
    	// once we have all the basic perks covered, we need to derive A, B, and population-equivalent T for coord
    	// -> and possibly parameters for carp if the next point of coord is worthless
    	
    	// the normal case, where coord's next point has value
    	
		double Amot = res[0][Perks.tsFactor.MOTIVATION.ordinal()];
		double Bmot = res[1][Perks.tsFactor.MOTIVATION.ordinal()];
		// scale from motivation factor to the equivalent population factor (some resources come from loot instead)
		double logTmot = Math.log((res[2][Perks.tsFactor.MOTIVATION.ordinal()] - 1) * nextRes.motiFraction + 1);
		double logTcarp = Math.log(Tcarp);
    	if (coordNextGain > 1) {
    		// We know carpentry is simply the aggregate of gains from mot, loot (excluding helium), and coord:
    		// 	A<carp> =
    		//		A<mot>*log(T<carp>,T<mot>)
    		//		+ (A<loot> - 1)*log(T<carp>,T<mot>)
    		//		+ A<coord>*log(T<carp>,T<coord>)
    		// And as with other perks, for coord we have:
    		// 	T^A = sqrt(nextGain * lastGain)
    		// These two equations allow us to eventually derive:
    		// 	A<coord>^2 =
    		//		(A<carp> - A<mot>*log(T<carp>,T<mot> - (A<loot> - 1)*log(T<carp>,T<mot>))
    		//		* log(sqrt(nextGain * lastGain),T<carp>)
    		//	T<coord> = (sqrt(nextGain * lastGain)^(1/A<coord>)
    		double Acarp = res[0][Perks.tsFactor.CARPENTRY.ordinal()];
    		double coordGainMean = Math.sqrt(coordNextGain * coordLastGain);
    		double A = Acarp / logTcarp - Amot / logTmot;
    		A *= Math.log(coordGainMean);
    		A = Math.sqrt(A);
    		double T = Math.pow(coordGainMean, 1/A);
    		
    		// Once we have A and T, B is calculated as for other perks:
			double B = 2 * ( A - Math.log(coordLastGain) / Math.log(T) );

//			System.out.format("Acarp=%.2e logTcarp=%.2f Amot=%.2e logTmot=%.2f Aloot=%.2e logTloot=%.2f%n",
//					Acarp, logTcarp, Amot, logTmot, Aloot, logTloot);
			
			res[0][Perks.tsFactor.COORDINATED.ordinal()] = A;
			res[1][Perks.tsFactor.COORDINATED.ordinal()] = B;
			res[2][Perks.tsFactor.COORDINATED.ordinal()] = T;
			
			// carpentry is always calculated from coord and mot, but needs to know the fraction of 
			// resources that come from motivation
			res[2][Perks.tsFactor.CARPENTRY.ordinal()] = nextRes.motiFraction;
			
			// as with other perks, check that the model gives similar results for the two derivations of B:
			double Bprime = 2 * ( Math.log(coordNextGain) / Math.log(T) - A );
			if (Math.abs(B/Bprime - 1d ) > .1) { 
				System.out.println("WARNING: large divergence between B and B', model may not be so good :("); 
				System.out.format("For %s, B=%.2e B'=%.2e%n", "COORDINATED", B, Bprime);
			}
    	} else {
    		
    		throw new Error ("Next point of coord should always have value in the current paradigm!%n");
//    		
//    		// Next point of coord has no value, so use alternate model for H<carp> and H<coord>
//    		//	Above baseline, more coord does nothing.
//    		//	Below baseline, we use a calculated slope B.
//    		//	For carp, we calculate H directly from the mot/loot/coord effects,
//    		//		using the A/B/T values for those perks (and 0 coord effect above baseline).
//    		
//    		// local helium gain exponent at baseline (aka "A") is 0: more coord does nothing.
//    		res[0][Perks.tsFactor.COORDINATED.ordinal()] = 0;
//    		
//    		// carpLastGain
//    		//	= H<carp>(1/Tcarp,Tcarp)
//    		//	= H<mot>(1/Tcarp,Tcarp) * H<loot>(1/Tcarp,Tcarp) * H<coord>(1/Tcarp,Tcarp)
//    		// (with H<loot> excluding direct helium gain, since we subtracted 1 from Aloot)
//    		// Solve for H<coord>(1/Tcarp,Tcarp):
//    		double Hmot = Math.pow(Tcarp, Amot - Bmot / 2d * logTcarp / logTmot);
//    		double Hcoord = carpLastGain / Hmot;
//    		
//    		
//    		// H<coord>(1/Tcarp,Tcarp)
//    		//	= Tcarp^(A + B*log(1/Tcarp,Tcoord) + (B/2)*log(Tcarp,Tcoord))
//    		// 		note A = 0 and log(1/Tcarp) = -log(Tcarp)
//    		//	= Tcarp^((-B/2)*log(Tcarp,Tcoord))
//    		// And coordLastGain
//    		//		= H<coord>(1/Tcoord,Tcoord)
//    		//		= Tcoord^(-B/2)
//    		// Solving for T<coord>:
//    		//	Tcoord = Tcarp ^ sqrt(log(coordLastGain,Hcoord))
//    		double T = Math.log(coordLastGain) / Math.log(Hcoord);
//    		T = Math.pow(Tcarp, Math.sqrt(T));
//    		double B = -2 * (Math.log(coordLastGain) / Math.log(T));
//
//    		res[1][Perks.tsFactor.COORDINATED.ordinal()] = B;
//    		res[2][Perks.tsFactor.COORDINATED.ordinal()] = T;
//    		
//    		// for carp, it's not that A/B/T are truly 0, but this tells H<carp> to use the alternate model.
//    		res[0][Perks.tsFactor.CARPENTRY.ordinal()] = 0;
//    		res[1][Perks.tsFactor.CARPENTRY.ordinal()] = 0;
//    		res[2][Perks.tsFactor.CARPENTRY.ordinal()] = 0;
    		
    	}
    	
//    	System.out.format("perks=%s%nA=%s%nB=%s%nT=%s%n",
//    			Arrays.toString(perks.getPerkLevels()),
//    			Arrays.toString(res[0]),
//    			Arrays.toString(res[1]),
//    			Arrays.toString(res[2]));
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
