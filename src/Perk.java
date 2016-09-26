
public enum Perk {
    
    POWER(1,1.3,false,0.05,false,new int[]{1},Perks.tsFactor.POWER),
    MOTIVATION(2,1.3,false,0.05,false,new int[]{1,2,3},Perks.tsFactor.MOTIVATION),
    CARPENTRY(25,1.3,false,1.1,true,new int[]{1,2,3},Perks.tsFactor.CARPENTRY),
    LOOTING(1,1.3,false,0.05,false,new int[]{1},Perks.tsFactor.LOOTING),   
    POWER2(20000,500,true,0.01,false,new int[]{100,1000},Perks.tsFactor.POWER),
    MOTIVATION2(50000,1000,true,0.01,false,new int[]{100,1000,2000,5000},Perks.tsFactor.MOTIVATION),
    CARPENTRY2(100000,10000,true,0.0025,false,new int[]{100,1000,2000},Perks.tsFactor.CARPENTRY),
    LOOTING2(100000,10000,true,0.0025,false,new int[]{100,1000},Perks.tsFactor.LOOTING),
    COORDINATED(150000,1.3,false,0.98,true,new int[]{1},Perks.tsFactor.COORDINATED),
    ARTISANISTRY(15,1.3,false,0.95,true,new int[]{1},Perks.tsFactor.ARTISANISTRY),
    RESOURCEFUL(50000,1.3,false,0.95,true,new int[]{1,2,3},Perks.tsFactor.RESOURCEFUL);
  
    public final long baseCost;
    public final double scaleFactor;
    public final boolean additive;
    public final double effect;
    public final boolean compounding;
    public final int[] levelIncreases;
    public final Perks.tsFactor tsFactor;
    
    Perk(final long baseCost,
    		final double scaleFactor, final boolean additive, 
    		final double effect, final boolean compounding,
    		final int[] levelIncreases, final Perks.tsFactor tsFactor){
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.additive = additive;
        this.effect = effect;
        this.compounding = compounding;
        this.levelIncreases = levelIncreases;
        this.tsFactor = tsFactor;
    }
}
