
public class PerksDeterminator {
    private Perks perks;
    
    public static void main(String[] args){
        int[] perkArray = new int[11];
        double totalHelium = 140d;
        Perks perks = new Perks(perkArray,totalHelium);
        PerksDeterminator pD = new PerksDeterminator(perks);
        Perks result = pD.determinePerks();
        for (int x = 0; x<Perk.values().length;x++){
            System.out.println(Perk.values()[x].name()+" : "+result.getLevel(Perk.values()[x]));
        }
    }
    
    public PerksDeterminator(final Perks perks){
        this.perks = perks;
    }
    
    public Perks determinePerks(){
        while(true){
            Perks savedPerks = new Perks(perks);
            int bestPerk = 0;
            int count = 0;
            double highestHeHrPercentage = 0;
            for (Perk p: Perk.values()){
                Perks usePerks = new Perks(savedPerks);
                if(usePerks.buyPerk(p, p.levelIncrease)){
                    TrimpsSimulation tS = new TrimpsSimulation(usePerks);
                    double heHrPercentage = tS.runSimulation();
                    if (heHrPercentage>highestHeHrPercentage){
                        bestPerk = count;
                        highestHeHrPercentage = heHrPercentage;
                    }
                }
                count++;
            }
            if (highestHeHrPercentage>0){
                savedPerks.buyPerk(Perk.values()[bestPerk], Perk.values()[bestPerk].levelIncrease);
                perks = savedPerks;
            }
            else{
                break;
            }
        }
        return perks;
    }
}
