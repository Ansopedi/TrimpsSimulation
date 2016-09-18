import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class PerksDeterminator {
    private Perks perks;
    
    public static void main(String[] args){
        int[] perkArray = new int[]{80,80,80,80,30000,20000,9000,30000,55,75,30};
        double totalHelium = 14600000000000d;
        Perks perks = new Perks(perkArray,totalHelium);
        PerksDeterminator pD = new PerksDeterminator(perks);
        pD.printPerksToFile();
        Perks result = pD.determinePerks();
        for (int x = 0; x<Perk.values().length;x++){
            System.out.print(Perk.values()[x].name()+" : "+result.getLevel(Perk.values()[x]));
        }
    }
    
    public PerksDeterminator(final Perks perks){
        this.perks = perks;
    }
    
    public Perks determinePerks(){
        while(true){
            System.out.println("iteration");
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
                System.out.println("write");
                printPerksToFile();
            }
            else{
                break;
            }
        }
        return perks;
    }
    
    private void printPerksToFile(){
        PrintWriter writer = null;;
        try {
            writer = new PrintWriter(System.getProperty("user.home") + "/Desktop/"+"perks.txt", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int x = 0; x<Perk.values().length;x++){
            writer.println(Perk.values()[x].name()+" : "+perks.getLevel(Perk.values()[x]));
        }
        writer.close();
    }
}
