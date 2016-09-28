import java.util.Comparator;
import java.util.Arrays;

public class Perks {
    private double totalHelium;
    private double helium;
    private int[] perks;
    private boolean fineTune = false;

    public Perks(final int[] perks, final double helium) {
        totalHelium = helium;
        this.helium = helium;
        this.perks = new int[Perk.values().length];
        for (int x = 0; x < Perk.values().length; x++) {
            this.perks[x] = 0;
            buyPerk(Perk.values()[x],perks[x]);
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
    
    public int getLevel(final Perk perk){
        return perks[perk.ordinal()];
    }
    
    public int[] getPerkLevels(){
        return perks;
    }
    
    public double getSpentHelium(){
        return totalHelium-helium;
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
    	LOOTING(Perk.LOOTING,Perk.LOOTING2,1.2),
    	COORDINATED(Perk.COORDINATED,null,1),
    	ARTISANISTRY(Perk.ARTISANISTRY,null,0.75),
    	RESOURCEFUL(Perk.RESOURCEFUL,null,0.5);
    	
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
    	private double cost;
    	private double sellEfficiency;
    	private final tsFactor statType;
    	private final double baselineEff;	// efficiency at creation level of perk (or +1 for coord)
    	private final double slopeEff;	// slope of efficiency curve between *effect and /effect
    	private final double baseFactor;	// baseline tsFactor
    	private Perk buyPerk;
    	private Perk sellPerk;
    	
    	public BuySellEfficiency( tsFactor tsF, double bE, double sE ) {
    		statType = tsF;
    		// COORDINATED overloads these fields:
    		//	baseFactor = baseline level
    		//	baselineEff = raw helium gain of next point
    		//  slopeEff = raw helium gain of last point
    		baseFactor = tsF == tsFactor.COORDINATED ? getLevel(tsF.base) : getTSFactor(tsF);
    		baselineEff = bE;
    		slopeEff = sE;
    		calculateEfficiencies();
    	}
    	
    	private void calculateEfficiencies() {
        	double[] tmp = getHeliumGainEfficiencies(statType.base, baseFactor, baselineEff, slopeEff);
        	buyEfficiency = tmp[0];
        	cost = tmp[1];
        	sellEfficiency = tmp[2];
        	buyPerk = statType.base;
        	sellPerk = statType.base;
        	if (statType.hasSpirePerk()) {
        		tmp = getHeliumGainEfficiencies(statType.spire, baseFactor, baselineEff, slopeEff);
        		if (tmp[0] > buyEfficiency || !canBuy()) {
        			buyEfficiency = tmp[0];
        			buyPerk = statType.spire;
        			cost = tmp[1];
        		}
        		if (tmp[2] < sellEfficiency) {
        			sellEfficiency = tmp[2];
        			sellPerk = statType.spire;
        		}
        	}
    	}
    	
    	public boolean canBuy() {
    		return helium >= cost;
    	}
    	
    	public tsFactor getTSF() {
    		return statType;
    	}
    	
    	public double getSellEfficiency() {
    		return sellEfficiency;
    	}
    	
    	public double getBuyEfficiency() {
    		return buyEfficiency;
    	}
    	
    	public boolean adjustPerk( int amount ) {
    		if (amount == 1 && buyPerk(buyPerk, amount)) {
    			calculateEfficiencies();
    			return true;
    		} else if (amount == -1 && buyPerk(sellPerk, amount)) {
    			calculateEfficiencies();
    			return true;
    		} else {
    			return false;
    		}
    	}
    }
    
    // sort in DESCENDING order of buy efficiency
    private static Comparator<BuySellEfficiency> BuyComp = new Comparator<BuySellEfficiency>() {
    	@Override
    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
    		return (a.buyEfficiency < b.buyEfficiency) ? 1 :
    			(a.buyEfficiency == b.buyEfficiency) ? 0 : -1;
    	}
    };
    
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
    public boolean permutePerks(double[][] rawEffs, boolean fineTune) {
    	
    	this.fineTune = fineTune;
    	
    	// Use an array for efficient sorting by 2 different comparators (buy and sell efficiency)
    	// -> The list is small, so the sort operation should be fast.
    	//	The presumption is that the overhead of maintaining more complex collections (e.g. TreeSet)
    	//	with different sorting orders, would be worse than just sorting the whole array
    	//  each time we update an element.
    	BuySellEfficiency[] bsEffs = new BuySellEfficiency[tsFactor.values().length];
    	
    	// compile the initial list of buy/sell efficiencies (unsorted)
    	for ( tsFactor t : tsFactor.values() ) {
    		bsEffs[t.ordinal()] = new BuySellEfficiency(t, rawEffs[0][t.ordinal()], rawEffs[1][t.ordinal()]);
//    		System.out.format("tsf %s buyEff=%.4e sellEff=%.4e%n",
//    				t.name(), bsEffs[t.ordinal()].getBuyEfficiency(), bsEffs[t.ordinal()].getSellEfficiency());
    	}
	

		// sell least-efficient perks until we are sure none are over-bought (see method for further description)
    	// -> no need to track whether we did anything, because if we did,
    	//	then buyMostEfficientPerks will do something and return true
		sellLeastEfficientPerks(bsEffs);
		
		System.out.println("after sell:");
    	for ( int i = 0; i < bsEffs.length; i++ ) {
    		System.out.format("tsf %s buyEff=%.4e sellEff=%.4e%n",
    				bsEffs[i].getTSF().name(), bsEffs[i].getBuyEfficiency(), bsEffs[i].getSellEfficiency());
    	}
	
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
    			bse.adjustPerk(-1);
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
    		while (bsEffs[0].getBuyEfficiency() >= nextEfficiency && bsEffs[0].adjustPerk(1)) {
    			res = true;
    		}
//    		if (perks[7] > 46343) {
//    		System.out.format("adjusted TSF %s to lev=%d eff=%.4e%n",
//    				bsEffs[0].buyPerk, perks[bsEffs[0].buyPerk.ordinal()],
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
//    				bsEffs[i].getTSF().name(), bsEffs[i].getBuyEfficiency(), bsEffs[i].getSellEfficiency());
//    	}
    	
    	return res;
    }
    
    // get efficiency of he/hr gain for helium cost gain, normalized to totalHelium
    // -> This way efficiencies are stable with changes in spent helium,
    //	and the "true" efficiency (relative to spent helium) converges to
    //	the calculated efficiency as spent helium converges to total helium.
    private double[] getHeliumGainEfficiencies(Perk perk, double baseFactor, double baselineEff, double slopeEff) {
    	final int amount = 1; // may want to support other amounts eventually
    	int baseLevel = perks[perk.ordinal()];
    	double buyCost = perkCost(perk, amount);
    	double sellCost = perkCost(perk, -amount);
    	double[] res = new double[3];
    	res[1] = buyCost;
    	// transform cost to log on scale of total helium
    	double logBuyCost = Math.log(1 + buyCost / totalHelium);
    	double logSellCost = Math.log(1 + sellCost / totalHelium);
    	double buyFactor;
    	double sellFactor;
    	if (perk == Perk.COORDINATED) {
    		if (baseLevel > baseFactor) {
    			// model exponential decrease in coord eficiency with increasing levels
    			// -> to avoid buying lots of levels as efficiency tanks
    			// fineTune sets a hard-cap of +1 or -1 level for coord
    			buyFactor = fineTune ? 0 : Math.pow(baselineEff,1/(baseLevel - baseFactor));
    			sellFactor = baselineEff;
    		} else if (baseLevel == baseFactor) {
    			buyFactor = baselineEff;
    			sellFactor = slopeEff;
    		} else {
    			// model exponential increase in coord efficiency with decreasing levels
    			// -> to avoid selling lots of levels as efficiency increases
    			buyFactor = Math.pow(slopeEff, baseFactor - baseLevel);
    			sellFactor = fineTune ? Double.POSITIVE_INFINITY : buyFactor * slopeEff;
    		}
    		res[0] = amount * Math.log(buyFactor) / logBuyCost;
    		res[1] = amount * buyCost;
    		res[2] = amount * Math.log(sellFactor) / logSellCost;
    		return res;
    	}
    	// get the raw buy/sell efficiencies using the baseline & slope, clamping to testEffect
    	double tE = Perk.getTSFactor(perk).testEffect;
    	tE = tE > 1 ? tE : 1/tE;
    	double logTE = Math.log(tE);
    	buyFactor = getTSFactor(perk, getLevel(perk) + amount) / baseFactor;
    	buyFactor = Math.max(Math.min(buyFactor, tE),1/tE);
    	buyFactor = baselineEff + slopeEff * Math.log(buyFactor) / logTE;
    	
    	sellFactor = getTSFactor(perk, getLevel(perk) - amount) / baseFactor;
    	sellFactor = Math.max(Math.min(sellFactor, tE),1/tE);
    	sellFactor = baselineEff + slopeEff * Math.log(sellFactor) / logTE;
    	
    	double buyEffect;
    	double sellEffect;
    	if (perk.compounding) {
    		// for compounding perks that provide a discount, the benefit is the reciprocal of the effect
    		buyEffect = perk.effect < 1 ? 1/perk.effect : perk.effect;
    		buyEffect = Math.pow(buyEffect, amount);
    		sellEffect = buyEffect;
    		// for all compounding perks, the benefit is just multiplied by the number of levels
    	} else {
    		// for additive perks, the effect diminishes with increasing levels
    		buyEffect = 1 + amount / (1/perk.effect + baseLevel);
    		sellEffect = 1 + amount / (1/perk.effect + baseLevel - amount);
    	}
		res[0] = buyFactor * Math.log(buyEffect) / logBuyCost;
		res[1] = amount * buyCost;
		res[2] = baseLevel == 0 ? Double.POSITIVE_INFINITY : sellFactor * Math.log(sellEffect) / logSellCost;
    	//	System.out.format("%s lev=%d bE=%3e bF=%3e se=%3e%n", perk.name(), getLevel(perk), res[0], buyFactor, res[2]);
    	if (res[0] < 0 || res[2] < 0) {
    		throw new Error(String.format(
    				"Setting perk %s to negative efficiency! lev=%d bEff=%.2e sEff=%.2e%n"
    				+ "bFac=%.2e sFac=%.2e bEffect=%.2e sEffect=%.2e 1/tE=%.2e%n",
    				perk.name(), getLevel(perk), 
    				res[0], res[2], buyFactor, sellFactor, buyEffect, sellEffect, 1/tE));
    	}
    	return res;
    }
    
    // todo: consider junking this. can just treat base/spire perks separately, using same tsFactor.
    // buy specified levels of spire perk, including buying the base perk
    public boolean buySpirePerk(final Perk perk, int amount) {
    	double aBase = 0;
    	double fBase = 1;
    	double aSpire = 0;
    	Perk perkB;
    	switch (perk) {
    		case POWER2:
    			perkB = Perk.POWER;
    			aBase = 0.05;
    			aSpire = 0.01;
    			break;
    		case MOTIVATION2:
    			perkB = Perk.MOTIVATION;
    			aBase = 0.05;
    			aSpire = 0.01;
    			break;
    		case LOOTING2:
    			perkB = Perk.LOOTING;
    			aBase = 0.05;
    			aSpire = 0.0025;
    			break;
    		case CARPENTRY2:
    			perkB = Perk.CARPENTRY;
    			fBase = 1.1;
    			aSpire = 0.0025;
    			break;
    		default:
    			throw new Error("perk " + perk.name() + " is not a Spire perk!%n");
    	}
    	
    	boolean baseMaxed = false;
    	do {
    		if (baseMaxed) {
    			return buyPerk(perk, amount);
    		}
    		
    		// calculate efficiency (in gain amount per helium) for base perk
    		int lBase = perks[perkB.ordinal()];
    		double cBase = perkCost(perkB, 1);
    		double eBase = (fBase * (1 + 1/(1/aBase + lBase)) - 1)/cBase;
    	
    		// calculate efficiency for spire perk
    		int lSpire = perks[perk.ordinal()];
    		double cSpire = perkCost(perk, 1); 
    		double eSpire = 1/(1/aSpire + lSpire)/cSpire;
    		
    		if (eBase >= eSpire) {
    			if (!buyPerk(perkB, 1)) { baseMaxed = true; }
    		} else {
    			int toBuy = (int) Math.min(amount, Math.max(1, (Math.sqrt(eSpire / eBase) - 1) * lBase));
    			if (!buyPerk(perk, toBuy)) { return false; }
    			amount -= toBuy;
    		}
    	} while (amount > 0);
    	return true;
    }
}
