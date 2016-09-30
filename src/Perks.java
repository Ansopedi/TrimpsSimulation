import java.util.Comparator;
import java.util.Arrays;

public class Perks {
    private double totalHelium;
    private double helium;
    private int[] perks = new int[Perk.values().length];
    private boolean fineTune = false;
    private DebugFilter df = new DebugFilter(1000);
    private double buySellInc = 0.03; // percentage of levels to buy or sell at a time (when not fineTuning)

    public Perks(final int[] perks, final double helium) {
        totalHelium = helium;
        this.helium = helium;
        for (int i = 0; i < Perk.values().length; i++) {
            this.perks[i] = 0;
            buyPerk(Perk.values()[i],perks[i]);
        }
    }
    
    public Perks(final int[] perks) {
    	this.helium = 0;
    	this.perks = new int[Perk.values().length];
    	for (int i = 0; i < Perk.values().length; i++) {
    		this.totalHelium += perkCost(Perk.values()[i],perks[i]);
    		this.perks[i] = perks[i];
    	}
    }
    
    public Perks(final Perks perks){
        this.totalHelium = perks.totalHelium;
        this.helium = perks.helium;
        this.perks = new int[Perk.values().length];
        for (int x = 0; x < Perk.values().length; x++) {
            this.perks[x] = perks.perks[x];
        }
    }
    
    private class DebugFilter {
    	private int i;
    	private int n;
    	
    	public DebugFilter( int n ) {
    		this.i = 0;
    		this.n = n;
    	}
    	
    	public boolean filter( boolean inc ) {
    		if (inc) {
    			return i++ % n == 0;
    		} else {
    			return i % n == 0;
    		}
    	}
    }
    
    public int getLevel(final Perk perk){
        return perks[perk.ordinal()];
    }
    
    public int[] getPerkLevels(){
        return perks;
    }
    
    public double getSpentHelium(){
        return totalHelium-helium;
    }
    
    public double getTotalHelium() {
    	return totalHelium;
    }
    
    public double perkCost(final Perk perk, int amount) {
    	int baseLevel;
    	if (amount < 0) {
    		amount = Math.min(-amount, getLevel(perk));
    		baseLevel = perks[perk.ordinal()] - amount;
    	} else {
    		baseLevel = perks[perk.ordinal()];
    	}
        if (perk.additive){
            double base = perk.baseCost+((double)perk.scaleFactor*baseLevel);
            return ((base+(double)(perk.scaleFactor*(amount-1)/2d))*amount);
        } else{
        	double base = perk.baseCost*Math.pow(perk.scaleFactor, baseLevel);
        	// base cost times geometric sum for exponents of 1 -> amount
            return base
            		* (1 - Math.pow(perk.scaleFactor, amount))
            		/ (1 - perk.scaleFactor);
        }
    }
    
    public boolean buyPerk(final Perk perk, int amount){
        double cost = perkCost(perk,amount);
        if (amount < 0) {
        	// can't sell more levels of a perk than we have
        	amount = Math.min(-amount, perks[perk.ordinal()]); 
        	if (cost > 0) {
        		helium += cost;
        		perks[perk.ordinal()] -= amount;
        		return true;
        	} else {
        		return false;
        	}
        } else {
        	if (cost<=helium){
        		helium-=cost;
        		perks[perk.ordinal()]+=amount;
        		return true;
        	}
        	return false;
        }
    }
    
    public enum tsFactor {
    	POWER(Perk.POWER,Perk.POWER2,1.5),
    	MOTIVATION(Perk.MOTIVATION,Perk.MOTIVATION2,1.5),
    	CARPENTRY(Perk.CARPENTRY,Perk.CARPENTRY2,1.2),
    	LOOTING(Perk.LOOTING,Perk.LOOTING2,1.5),
    	COORDINATED(Perk.COORDINATED,null,1),
    	ARTISANISTRY(Perk.ARTISANISTRY,null,0.75),
    	RESOURCEFUL(Perk.RESOURCEFUL,null,0.75);
    	
    	public final Perk base;
    	public final Perk spire;
    	public final double testEffect; // size of effect to test in the determinator
    	
    	tsFactor(Perk base, Perk spire, double testEffect) {
    		this.base = base;
    		this.spire = spire;
    		this.testEffect = testEffect;
    	}
    	
    	public boolean hasSpirePerk() {
    		return spire != null;
    	}
    }
    public static final int numTSFactors = tsFactor.values().length;
    
    public double[] getTSFactors() {
    	double[] res = new double[numTSFactors];
    	for ( tsFactor t : tsFactor.values() ) {
    		res[t.ordinal()] = getTSFactor(t);
    	}
    	return res;
    }
    
    public double getTSFactor(Perk perk, int level) {
    	double res;
    	tsFactor tsf = Perk.getTSFactor(perk);
    	int baseLevel = tsf.base == perk ? level : getLevel(tsf.base);
		if ( tsf == tsFactor.COORDINATED ) {
			res =  calcCoordFactor(baseLevel);
		} else if (tsf.base.compounding) {
			res =  Math.pow(tsf.base.effect, baseLevel);
		} else {
			res =  1 + tsf.base.effect * baseLevel;
		}
		if ( tsf.hasSpirePerk() ) {
	    	int spireLevel = tsf.spire == perk ? level : getLevel(tsf.spire);
			res *= 1 + tsf.spire.effect * spireLevel;
		}
		return res;
    }
    
    public double getTSFactor(tsFactor t) {
    	return getTSFactor(t.base, getLevel(t.base));
    }
    
    public static double calcCoordFactor(int level) {
    	return (1 + 0.25 * Math.pow(Perk.COORDINATED.effect, level));
    }
    
    private class BuySellEfficiency {
    	private double buyEfficiency;
    	private double sellEfficiency;
    	private final double statBase;
    	private final tsFactor statType;
    	private Perk perkToBuy;
    	private Perk perkToSell;
    	private double A;
    	private double B;
    	private double T;
    	// if this perk is related to others, those perks also needs their efficiencies updated
    	private BuySellEfficiency[] duals = new BuySellEfficiency[0];
    	
    	public BuySellEfficiency( tsFactor F, double A, double B, double T ) {
    		this.statType = F;
    		this.A = A;
    		this.B = B;
    		this.T = T;
    		if (F == tsFactor.COORDINATED) {
    			this.statBase = getLevel(Perk.COORDINATED);
    		} else {
    			this.statBase = getTSFactor(statType);
    		}
    		calculateEfficiencies();
    	}
    	
    	public double getCurrentStatFactor() {
    		if (statType == tsFactor.COORDINATED) {
    			// return factor in terms of equivalent population
    			return Math.pow(T, (getLevel(Perk.COORDINATED) - statBase));
    		} else {
    			return getTSFactor(statType) / statBase;
    		}
    	}
    	
    	public double getHeliumGain(double X, double Y) {
    		return getHeliumGain(X, Y, true, true);
    	}
    	
    	public double getT() {
    		return T;
    	}
    	
    	public void setT(double T) {
    		this.T = T;
    	}
    	
    	// due to floating point precision issues, we need to force bound checks to be skipped
    	//	when the inputs are meant to fall inside the bounds
    	public double getHeliumGain(double X, double Y,
    			boolean doUpperBoundCheck, boolean doLowerBoundCheck) {
    		
    		double res;
    		
    		// carpentry is always calculated directly from mot/loot/coord
    		if (statType == tsFactor.CARPENTRY) {
    			BuySellEfficiency mot = duals[0];
    			BuySellEfficiency coord = duals[1];
    			double svT = mot.getT();
    			// carp's T value is just the fraction of metal that comes from motivation
    			mot.setT((svT - 1) * T + 1);
    			res = mot.getHeliumGain(mot.getCurrentStatFactor() * X, Y);
    			mot.setT(svT);
    			res *= coord.getHeliumGain(coord.getCurrentStatFactor() * X, Y);
    		}
    		// coordinated needs special handling when the next point is worthless
    		else if (statType == tsFactor.COORDINATED && A == 0 && X * Y > 1 && doUpperBoundCheck) {
    			if (X >= 1) {
					// if next point of coord is worthless, it gets no benefit above baseline
					res = 1;
				} else {
					// there is some helium gain from X to 1, and none thereafter
					// -> and promise the recursion that we fall inside the upper bound (in case of precision issues)
					res = getHeliumGain(X, 1/X, true, false);
    			}
    		}
    		// if some portion of the range is outside the lower sim bound, apply clamping
    		else if (doLowerBoundCheck && X * T < 1) {
				// entire range is outside the sim bound, calculate result directly
				if (X * Y * T <= 1) {
					res = getClampedHeliumGain(X, Y, true);
				} else {
					//if (df.filter(true)) {
					//	System.out.format("XT-1=%.2e XYT-1=%.2e X=%.2e Y=%.2e T=%.2e%n", X*T-1, X*Y*T-1, X, Y, T);
					//}
					
					// decompose into portions divided by the sim bound
					
					// get the clamped result for the portion below the bound
					res = getClampedHeliumGain(X, 1 / T / X, true);
					// compound with the portion above the lower bound, skipping the lower bound check
					res *= getHeliumGain(1/T, X * Y * T, false, doUpperBoundCheck);
				}
    		}
    		// if some portion of the range is outside the upper sim bound, apply clamping
    		else if (doUpperBoundCheck && X * Y / T > 1) {
    			if (X / T >= 1) {
        			// entire range is outside the sim bound, calculate result directly
    				res = getClampedHeliumGain(X, Y, false);
    			} else {
    				// decompose into portions divided by the sim bound
    				
    				// get the portion below the upper bound, skipping the upper bound check
    				res = getHeliumGain(X, T / X, doLowerBoundCheck, false);
    				// compound with the clamped portion above the upper bound
    				res *= getClampedHeliumGain(T, X * Y / T, false);
    			}
    		} else {
    			// standard model when the entire range is inside the sim bounds
    			res = Math.pow(Y, A + B * Math.log(X * Math.sqrt(Y)) / Math.log(T));
    		}
//    		if (statType == tsFactor.COORDINATED) {
//    		System.out.format("%s He gain X=%.2e Y=%.2e A=%.2e B=%.2e T=%.2e gain=%.2e base=%s spire=%s%n",
//    				statType.name(), X, Y, A, B, T, res - 1,
//    				getLevel(statType.base),statType.hasSpirePerk() ? getLevel(statType.spire) : "none");
//    		}
    		// final clamp at no value, just in case - we can't handle negative helium gains (which are presumed to be fake)
    		return Math.max(1, res);
    	}
    	
    	private double getClampedHeliumGain( double X, double Y, boolean belowLowerBound ) {
    		double res;
    		if (belowLowerBound) {
	    		if (statType == tsFactor.COORDINATED) {
					// exponent gets larger and larger with decreasing X
					res = Math.pow(Y, (A - B) / Math.sqrt(X * Math.sqrt(Y) * T));
				} else {
					// perks other than coord use a fixed clamp outside the sim bounds
					res = Math.pow(Y, A - B);
				}
    		} else {
    			if (statType == tsFactor.COORDINATED) {
					// exponent gets smaller and smaller with increasing X
					res = Math.pow(Y, (A + B) / Math.sqrt(X * Math.sqrt(Y) / T));
				} else {
					// perks other than coord use a fixed clamp outside the sim bound
					res = Math.pow(Y, A + B);
				}
    		}
    		return res;
    	}
    	
    	public void setDuals( BuySellEfficiency[] duals ) {
    		this.duals = duals;
    		// carpentry can't calculate its initial efficiencies until it gets duals
    		if (statType == tsFactor.CARPENTRY) {
    			calculateEfficiencies();
    		}
    	}
    	
    	// followDuals causes us to recalculate efficiencies for the duals as well (when buying or selling a perk)
    	public void calculateEfficiencies() {
    		double X, Y, lastY;
    		Perk base = statType.base;
    		double currentTSF = getTSFactor(statType);
    		if (statType == tsFactor.COORDINATED) {
    			X = getCurrentStatFactor();
    			if (duals.length > 0) {
    				X *= duals[0].getCurrentStatFactor();
    			}
    			Y = T;
    			lastY = Y;
    		} else if (statType == tsFactor.CARPENTRY) {
    			if (duals.length == 0) { return; } // carpentry needs duals to calculate efficiencies
    			X = getCurrentStatFactor();
    			Y = Perk.CARPENTRY.effect;
    			lastY = Y;
    		} else {
    			X = getCurrentStatFactor();
    			for (BuySellEfficiency dual : duals) {
    				X *= dual.getCurrentStatFactor();
    			}
    			if (base.compounding) {
    				Y = base.effect;
    				lastY = Y;
    			} else {
    				Y = getTSFactor(base, getLevel(base) + 1) / currentTSF;
    				lastY = currentTSF / getTSFactor(base, getLevel(base) - 1);
    			}
    		}
    		// helium gain of +1 or -1 point in base perk
			double logBuyEffect = Math.log(getHeliumGain(X,Y));
			double logSellEffect = Math.log(getHeliumGain(X/lastY,lastY));
			double logBuyCost = Math.log(1 + perkCost(base,1) / totalHelium);
			double logSellCost = Math.log(1 + perkCost(base,-1) / totalHelium);
			
//			if (df.filter(false)) {
//			System.out.format("%s logBE=%.2e logBC=%.2e%n", base.name(), logBuyEffect, logBuyCost);
//			}
			
			buyEfficiency = logBuyEffect / logBuyCost;
			if (getLevel(base) == 0) {
				sellEfficiency = Double.POSITIVE_INFINITY;
			} else {
				sellEfficiency = logSellEffect / logSellCost;
			}
			perkToBuy = base;
			perkToSell = base;
			
			// also check spire perk to see if it's best to buy/sell next
			if (statType.hasSpirePerk()) {
				Perk spire = statType.spire;
				Y = getTSFactor(spire, getLevel(spire) + 1) / currentTSF;
				lastY = currentTSF / getTSFactor(spire, getLevel(spire) - 1);
				
				logBuyEffect = Math.log(getHeliumGain(X,Y));
				logSellEffect = Math.log(getHeliumGain(X/lastY,lastY));
				logBuyCost = Math.log(1 + perkCost(spire,1) / totalHelium);
				logSellCost = Math.log(1 + perkCost(spire,-1) / totalHelium);
				
//				if (df.filter(true)) {
//				System.out.format("%s logBE=%.2e logBC=%.2e%n", spire.name(), logBuyEffect, logBuyCost);
//				}
				
				double tmp = logBuyEffect / logBuyCost;
				if (tmp > buyEfficiency) {
					buyEfficiency = tmp;
					perkToBuy = spire;
				}
				if (getLevel(spire) == 0) {
					tmp = Double.POSITIVE_INFINITY;
				} else {
					tmp = logSellEffect / logSellCost;
				}
				if (tmp < sellEfficiency) {
					sellEfficiency = tmp;
					perkToSell = spire;
				}
			}
    	}
    	
    	public boolean canBuy() {
    		return canBuy(1);
    	}
    	
    	public boolean canBuy(int amount) {
    		return helium >= perkCost(perkToBuy, amount);
    	}
    	
    	public tsFactor getStatType() {
    		return statType;
    	}
    	
    	public double getSellEfficiency() {
    		return sellEfficiency;
    	}
    	
    	public double getBuyEfficiency() {
    		return buyEfficiency;
    	}
    	
    	public boolean adjustPerk( int amount ) {
    		boolean res = amount > 0 ? buyPerk(perkToBuy, amount) : buyPerk(perkToSell, amount);
    		calculateEfficiencies();
    		for (BuySellEfficiency dual : duals) {
        		dual.calculateEfficiencies();
    		}
    		return res;
    	}
    }
    
//    // sort in DESCENDING order of buy efficiency
//    private static Comparator<BuySellEfficiency> BuyComp = new Comparator<BuySellEfficiency>() {
//    	@Override
//    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
//    		return (a.buyEfficiency < b.buyEfficiency) ? 1 :
//    			(a.buyEfficiency == b.buyEfficiency) ? 0 : -1;
//    	}
//    };
    
    // sort in DESCENDING order of buy efficiency, ignoring unaffordable perks
    private static Comparator<BuySellEfficiency> BuyCompAffordable = new Comparator<BuySellEfficiency>() {
    	@Override
    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
    		if (a.canBuy() && b.canBuy()) {
    			return (a.buyEfficiency < b.buyEfficiency) ? 1 :
    				(a.buyEfficiency == b.buyEfficiency) ? 0 : -1;
    		} else {
    			return b.canBuy() ? 1 : a.canBuy() ? -1 : 0;
    		}
    	}
    };
    
    // sort in DESCENDING order of sell efficiency
    private static Comparator<BuySellEfficiency> SellComp = new Comparator<BuySellEfficiency>() {
    	@Override
    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
    		return (a.sellEfficiency < b.sellEfficiency) ? 1 :
    			(a.sellEfficiency == b.sellEfficiency) ? 0 : -1;
    	}
    };
   
    // given a set of TrimpsSimulation factor efficiencies, guess a better set of perks
    // return true if perks were changed, else false
    public boolean permutePerks(double[][] effStats, boolean fineTune) {
    	
    	this.fineTune = fineTune;
    	
    	// Use an array for efficient sorting by 2 different comparators (buy and sell efficiency)
    	// -> The list is small, so the sort operation should be fast.
    	//	The presumption is that the overhead of maintaining more complex collections (e.g. TreeSet)
    	//	with different sorting orders, would be worse than just sorting the whole array
    	//  each time we update an element.
    	BuySellEfficiency[] bsEffs = new BuySellEfficiency[tsFactor.values().length];
    	
    	// compile the initial list of buy/sell efficiencies (unsorted)
    	for ( tsFactor f : tsFactor.values() ) {
    		int i = f.ordinal();
    		bsEffs[i] = new BuySellEfficiency(f, effStats[0][i], effStats[1][i], effStats[2][i]);
//    		System.out.format("tsf %s buyEff=%.4e sellEff=%.4e%n",
//    				t.name(), bsEffs[t.ordinal()].getBuyEfficiency(), bsEffs[t.ordinal()].getSellEfficiency());
    	}
    	// set duals for perks that have related efficiencies that need to be co-calculated
    	bsEffs[tsFactor.CARPENTRY.ordinal()].setDuals(new BuySellEfficiency[] {
    			bsEffs[tsFactor.MOTIVATION.ordinal()],
    			bsEffs[tsFactor.COORDINATED.ordinal()]
    	});
    	bsEffs[tsFactor.COORDINATED.ordinal()].setDuals(new BuySellEfficiency[] {
    			bsEffs[tsFactor.CARPENTRY.ordinal()]
    	});
    	bsEffs[tsFactor.MOTIVATION.ordinal()].setDuals(new BuySellEfficiency[] {
    			bsEffs[tsFactor.CARPENTRY.ordinal()]
    	});
    	bsEffs[tsFactor.LOOTING.ordinal()].setDuals(new BuySellEfficiency[] {
    			bsEffs[tsFactor.CARPENTRY.ordinal()]
    	});
	

		// sell least-efficient perks until we are sure none are over-bought (see method for further description)
    	// -> no need to track whether we did anything, because if we did,
    	//	then buyMostEfficientPerks will do something and return true
		sellLeastEfficientPerks(bsEffs);
		
//		System.out.println("after sell:");
//    	for ( int i = 0; i < bsEffs.length; i++ ) {
//    		System.out.format("tsf %s buyEff=%.4e sellEff=%.4e%n",
//    				bsEffs[i].getStatType().name(), bsEffs[i].getBuyEfficiency(), bsEffs[i].getSellEfficiency());
//    	}
	
		System.out.format("perks after sell: %s%n", Arrays.toString(perks));
	
		// then buy most-efficient perks until we run out of helium
		return buyMostEfficientPerks(bsEffs);
    }
    
    private void sellLeastEfficientPerks(BuySellEfficiency[] bsEffs) {
    	// sort the list by sell efficiency
    	Arrays.sort(bsEffs, SellComp);
    	// get the most efficient perk at the beginning (which we know we don't want to sell)
    	double efficiencyToSell = bsEffs[0].getSellEfficiency();
    	// sell all other perks until they are more efficient than the above
    	for ( int i = 1; i < bsEffs.length; i++ ) {
    		BuySellEfficiency bse = bsEffs[i];
    		while (bse.getSellEfficiency() < efficiencyToSell) {
    			//System.out.format("sell perk %s to eff=%.4e: curEff=%.4e%n",
    			//		bse.perk.name(), efficiencyToSell, bse.getSellEfficiency());
    			// TODO? sell more than 1 level at once
    			int levelsToSell = (int) Math.ceil(getLevel(bse.perkToSell) * buySellInc);
    			levelsToSell = levelsToSell > getLevel(bse.perkToSell) ? 1 : levelsToSell;
    			levelsToSell = Math.max(1, levelsToSell);
    			bse.adjustPerk(-levelsToSell);
    		}
    	}
    }
    
    private boolean buyMostEfficientPerks(BuySellEfficiency[] bsEffs) {
    	boolean res = false;
    	
    	// sort the list by buy efficiency
    	Arrays.sort(bsEffs, BuyCompAffordable);
    	
    	// loop until no perk is affordable
    	while (bsEffs[0].canBuy()) {
    		double nextEfficiency = bsEffs[1].canBuy() ? bsEffs[1].getBuyEfficiency() : 0;
    		// loop buying this perk until it's no longer the most efficient
    		Perk p = bsEffs[0].perkToBuy;
    		int level = getLevel(p);
    		int levelsToBuy = fineTune ? 1 : (int) Math.max(1, Math.ceil(level * buySellInc));
    		levelsToBuy = bsEffs[0].canBuy(levelsToBuy) ? levelsToBuy : 1;
    		while (bsEffs[0].getBuyEfficiency() >= nextEfficiency && bsEffs[0].adjustPerk(levelsToBuy)) {
    			res = true;
    		}
//    		if (perks[7] > 46343) {
//    		System.out.format("adjusted TSF %s to lev=%d eff=%.4e%n",
//    				bsEffs[0].perkToBuy, perks[bsEffs[0].perkToBuy.ordinal()],
//    				bsEffs[0].getBuyEfficiency());
//    		}
    		// re-sort this perk
    		// TODO? we know the rest of the array is sorted, so we can do an O(n) sort if needed
    		// -> but note that O(n * log(#perks)) is already good, so you'd better do a really good job!
    		Arrays.sort(bsEffs, BuyCompAffordable);
    	}
    	
//		System.out.println("after buy:");
//    	for ( int i = 0; i < bsEffs.length; i++ ) {
//    		System.out.format("tsf %s buyEff=%.4e sellEff=%.4e%n",
//    				bsEffs[i].getStatType().name(), bsEffs[i].getBuyEfficiency(), bsEffs[i].getSellEfficiency());
//    	}
    	
    	return res;
    }
}
