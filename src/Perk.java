
public enum Perk {
    
    POWER(1,1.3,false,new int[]{1}),
    MOTIVATION(2,1.3,false,new int[]{1,2,3}),
    CARPENTRY(25,1.3,false,new int[]{1,2,3}),
    LOOTING(1,1.3,false,new int[]{1}),   
    POWER2(20000,500,true,new int[]{100,1000}),
    MOTIVATION2(50000,1000,true,new int[]{100,1000,2000,5000}),
    CARPENTRY2(100000,10000,true,new int[]{100,1000,2000}),
    LOOTING2(100000,10000,true,new int[]{100,1000}),
    COORDINATED(150000,1.3,false,new int[]{1}),
    ARTISANISTRY(15,1.3,false,new int[]{1}),
    RESOURCEFUL(50000,1.3,false,new int[]{1,2,3});
  
    public final long baseCost;
    public final double scaleFactor;
    public final boolean additive;
    public final int[] levelIncreases;
    
    Perk(final long baseCost, final double scaleFactor, final boolean additive, final int[] levelIncreases){
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.additive = additive;
        this.levelIncreases = levelIncreases;
    }
}
