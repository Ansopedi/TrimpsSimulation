import java.util.Comparator;
import java.util.Arrays;

public class Perks {
    private double totalHelium;
    private double helium;
    private int[] perks;

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
        }
        else{
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
    	POWER, MOTIVATION, CARPENTRY, LOOTING, COORDINATED, ARTISANISTRY, RESOURCEFUL;
    }
    public static final int numTSFactors = tsFactor.values().length;
    
    public double[] getTSFactors() {
    	double[] res = new double[numTSFactors];
        res[tsFactor.POWER.ordinal()] = (1 + Perk.POWER.effect * perks[Perk.POWER.ordinal()])
				* (1 + Perk.POWER2.effect * perks[Perk.POWER2.ordinal()]);
        res[tsFactor.MOTIVATION.ordinal()] = (1 + Perk.MOTIVATION.effect * perks[Perk.MOTIVATION.ordinal()])
				* (1 + Perk.MOTIVATION2.effect * perks[Perk.MOTIVATION2.ordinal()]);	
        res[tsFactor.CARPENTRY.ordinal()] = Math.pow(Perk.CARPENTRY.effect, perks[Perk.CARPENTRY.ordinal()])
				* (1 + Perk.CARPENTRY2.effect * perks[Perk.CARPENTRY2.ordinal()]);
        res[tsFactor.LOOTING.ordinal()] = (1 + Perk.LOOTING.effect * perks[Perk.LOOTING.ordinal()])
				* (1 + Perk.LOOTING2.effect * perks[Perk.LOOTING2.ordinal()]);
        res[tsFactor.COORDINATED.ordinal()] = (1 + 0.25 * Math.pow(Perk.COORDINATED.effect, perks[Perk.COORDINATED.ordinal()]));
        res[tsFactor.ARTISANISTRY.ordinal()] = Math.pow(Perk.ARTISANISTRY.effect, perks[Perk.ARTISANISTRY.ordinal()]);
        res[tsFactor.RESOURCEFUL.ordinal()] = Math.pow(Perk.RESOURCEFUL.effect, perks[Perk.RESOURCEFUL.ordinal()]);
        return res;
    }
    
    public static double calcCoordFactor(int level) {
    	return (1 + 0.25 * Math.pow(.98, level));
    }
    
    private class BuySellEfficiency {
    	private double buyEfficiency;
    	private double sellEfficiency;
    	private Perk perk;
    	private double rawEff;
    	
    	public BuySellEfficiency( Perk p, double rE ) {
    		perk = p;
    		rawEff = rE;
    		calculateEfficiencies();
    	}
    	
    	private void calculateEfficiencies() {
    		buyEfficiency = getHeliumGainEfficiency(perk, rawEff, 1);
    		sellEfficiency = getHeliumGainEfficiency(perk, rawEff, -1);
    	}
    	
    	public Perk getPerk() {
    		return perk;
    	}
    	
    	public double getSellEfficiency() {
    		return sellEfficiency;
    	}
    	
    	public double getBuyEfficiency() {
    		return buyEfficiency;
    	}
    	
    	public boolean adjustPerk( int amount ) {
    		if (buyPerk(perk, amount)) {
    			calculateEfficiencies();
    			return true;
    		} else {
    			calculateEfficiencies();
    			return false;
    		}
    	}
    }
    
    // sort in DESCENDING order of sell efficiency
    private static Comparator<BuySellEfficiency> BuyComp = new Comparator<BuySellEfficiency>() {
    	@Override
    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
    		return (a.buyEfficiency < b.buyEfficiency) ? 1 :
    			(a.buyEfficiency == b.buyEfficiency) ? 0 : -1;
    	}
    };
    
    // sort in DESCENDING order of buy efficiency
    private static Comparator<BuySellEfficiency> SellComp = new Comparator<BuySellEfficiency>() {
    	@Override
    	public int compare(BuySellEfficiency a, BuySellEfficiency b) {
    		return (a.sellEfficiency < b.sellEfficiency) ? 1 :
    			(a.sellEfficiency == b.sellEfficiency) ? 0 : -1;
    	}
    };
   
    // given a set of TrimpsSimulation factor efficiencies, guess a better set of perks
    // return true if perks were changed, else false
    public boolean permutePerks(double[] rawEffs) {
    	
    	// Use an array for efficient sorting by 2 different comparators (buy and sell efficiency)
    	// -> The list is small, so the sort operation should be fast.
    	//	The presumption is that the overhead of maintaining more complex collections (e.g. TreeSet)
    	//	with different sorting orders, would be worse than just sorting the whole array
    	//  each time we update an element.
    	BuySellEfficiency[] bsEffs = new BuySellEfficiency[Perk.values().length];
    	
    	// compile the initial list of buy/sell efficiencies (unsorted)
    	for ( Perk p : Perk.values() ) {
    		bsEffs[p.ordinal()] = new BuySellEfficiency(p, rawEffs[p.ordinal()]);
    	}
	

		// sell least-efficient perks until we are sure none are over-bought (see method for further description)
    	// -> no need to track whether we did anything, because if we did,
    	//	then buyMostEfficientPerks will do something and return true
		sellLeastEfficientPerks(bsEffs);
	
		//System.out.format("perks after sell: %s%n", Arrays.toString(perks));
	
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
    	Arrays.sort(bsEffs, BuyComp);
    	
    	// loop until no perk is affordable
    	while (bsEffs[0].getBuyEfficiency() > 0) {
    		double nextEfficiency = bsEffs[1].getBuyEfficiency();
    		// loop buying this perk until it's no longer the most efficient
    		while (bsEffs[0].getBuyEfficiency() >= nextEfficiency && bsEffs[0].adjustPerk(1)) {
    			res = true;
    		}
//    		System.out.format("adjusted perk %s to lev=%d eff=%.4e%n",
//    				bsEffs[0].getPerk(), perks[bsEffs[0].getPerk().ordinal()],
//    				bsEffs[0].getBuyEfficiency());
    		// re-sort this perk
    		// TODO? we know the rest of the array is sorted, so we can do an O(n) sort if needed
    		// -> but note that O(n * log(#perks)) is already good, so you'd better do a really good job!
    		Arrays.sort(bsEffs, BuyComp);
    	}
    	
    	return res;
    }
    
    // get efficiency of he/hr gain for helium cost gain, normalized to totalHelium
    // -> This way efficiencies are stable with changes in spent helium,
    //	and the "true" efficiency (relative to spent helium) converges to
    //	the calculated efficiency as spent helium converges to total helium.
    private double getHeliumGainEfficiency(Perk perk, double rawEff, int amount) {
    	int baseLevel = perks[perk.ordinal()];
    	double cost = perkCost(perk, amount);
    	if (amount < 0) {
    		if (baseLevel == 0) {
    			// return infinite efficiency if we can't sell the requested perk
    			return Double.POSITIVE_INFINITY;
    		}
    		// set sell amount to actual number of levels that can be sold
    		amount = Math.min(-amount, baseLevel);
    		baseLevel -= amount;
    	} else if (amount == 0 || cost > helium) {
    		// return 0 efficiency if we can't afford the requested amount
    		return 0;
    	}
    	// transform cost to log on scale of total helium
    	cost = Math.log(1 + cost / totalHelium);
    	if (perk == Perk.COORDINATED) {
    		// the rawEff for coordinated is the hehr gain from 1 more point
    		return amount * Math.log(rawEff) / cost;
    	} else if (perk.compounding) {
    		// for compounding perks that provide a discount, the benefit is the reciprocal of the effect
    		double effect = perk.effect < 1 ? 1/perk.effect : perk.effect;
    		// for all compounding perks, the benefit is just multiplied by the number of levels
    		return rawEff * amount * Math.log(effect) / cost;
    	} else {
    		// for additive perks, the effect diminishes with increasing levels
    		double effect = 1 + amount / (1/perk.effect + baseLevel);
    		return rawEff * Math.log(effect) / cost;
    	}
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
