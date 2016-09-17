
public enum Equipment {
    Shield(40,false,4,1),
    Dagger(40,true,2,1),
    Boots(55,false,6,1),
    Mace(80,true,3,2),
    Helmet(100,false,10,2),
    Polearm(140,true,4,3),
    Pants(160,false,14,3),
    Battleaxe(230,true,7,4),
    Shoulderguards(275,false,23,4),
    Greatsword(375,true,9,5),
    Breastplate(415,false,35,5),
    Arbalest(450,true,15,5),
    Gamebeson(500,false,60,5);
    
    public final int baseCost;
    public final boolean damage;
    public final int baseEffect;
    public final int firstDropLevel;
    
    Equipment(final int baseCost, final boolean damage, final int baseEffect, final int firstDropLevel){
        this.baseCost = baseCost;
        this.damage = damage;
        this.baseEffect = baseEffect;
        this.firstDropLevel = firstDropLevel;
    }
    
}
