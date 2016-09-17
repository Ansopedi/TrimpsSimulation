
public enum Perk {
    
    POWER(1,1.3,false),
    MOTIVATION(2,1.3,false),
    CARPENTRY(25,1.3,false),
    LOOTING(1,1.3,false),   
    POWER2(20000,500,true),
    MOTIVATION2(50000,1000,true),
    CARPENTRY2(100000,10000,true),
    LOOTING2(100000,10000,true),
    COORDINATED(150000,1.3,false),
    ARTISANISTRY(15,1.3,false),
    RESOURCEFUL(50000,1.3,false);
  
    public final long baseCost;
    public final double scaleFactor;
    public final boolean additive;
    
    Perk(final long baseCost, final double scaleFactor, final boolean additive){
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.additive = additive;
    }
}
