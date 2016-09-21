
public enum Perk {
    
    POWER(1,1.3,false,1),
    MOTIVATION(2,1.3,false,1),
    CARPENTRY(25,1.3,false,1),
    LOOTING(1,1.3,false,1),   
    POWER2(20000,500,true,1000),
    MOTIVATION2(50000,1000,true,1000),
    CARPENTRY2(100000,10000,true,1000),
    LOOTING2(100000,10000,true,1000),
    COORDINATED(150000,1.3,false,1),
    ARTISANISTRY(15,1.3,false,1),
    RESOURCEFUL(50000,1.3,false,1);
  
    public final long baseCost;
    public final double scaleFactor;
    public final boolean additive;
    public final int levelIncrease;
    
    Perk(final long baseCost, final double scaleFactor, final boolean additive, final int levelIncrease){
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.additive = additive;
        this.levelIncrease = levelIncrease;
    }
}
