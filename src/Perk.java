public enum Perk {
    
    POWER(1,1.3,false,0.05,false,new int[]{1}),
    MOTIVATION(2,1.3,false,0.05,false,new int[]{1,2,3}),
    CARPENTRY(25,1.3,false,1.1,true,new int[]{1,2,3}),
    LOOTING(1,1.3,false,0.05,false,new int[]{1}),   
    POWER2(20000,500,true,0.01,false,new int[]{100,1000}),
    MOTIVATION2(50000,1000,true,0.01,false,new int[]{100,1000,2000,5000}),
    CARPENTRY2(100000,10000,true,0.0025,false,new int[]{100,1000,2000}),
    LOOTING2(100000,10000,true,0.0025,false,new int[]{100,1000}),
    COORDINATED(150000,1.3,false,0.98,true,new int[]{1}),
    ARTISANISTRY(15,1.3,false,0.95,true,new int[]{1}),
    RESOURCEFUL(50000,1.3,false,0.95,true,new int[]{1,2,3});
  
    public final long baseCost;
    public final double scaleFactor;
    public final boolean additive;
    public final double effect;
    public final boolean compounding;
    public final int[] levelIncreases;
    
    Perk(final long baseCost,
    		final double scaleFactor, final boolean additive, 
    		final double effect, final boolean compounding,
    		final int[] levelIncreases){
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.additive = additive;
        this.effect = effect;
        this.compounding = compounding;
        this.levelIncreases = levelIncreases;
    }
    
    // avoiding circular references between Perk and tsFactor constructors
    public static Perks.tsFactor getTSFactor(Perk p) {
    	switch (p) {
    	case POWER: return Perks.tsFactor.POWER;
    	case POWER2: return Perks.tsFactor.POWER;
    	case MOTIVATION: return Perks.tsFactor.MOTIVATION;
    	case MOTIVATION2: return Perks.tsFactor.MOTIVATION;
    	case CARPENTRY: return Perks.tsFactor.CARPENTRY;
    	case CARPENTRY2: return Perks.tsFactor.CARPENTRY;
    	case LOOTING: return Perks.tsFactor.LOOTING;
    	case LOOTING2: return Perks.tsFactor.LOOTING;
    	case COORDINATED: return Perks.tsFactor.COORDINATED;
    	case ARTISANISTRY: return Perks.tsFactor.ARTISANISTRY;
    	case RESOURCEFUL: return Perks.tsFactor.RESOURCEFUL;
    	default: return null;
    	}
    }
}
