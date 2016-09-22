import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PerksDeterminator {
    private Perks perks;

    public static void main(String[] args) {
        //TODO fix all 0 bug
        int[] perkArray = new int[] 
                {80,80,80,90,40000,20000,9000,27000,59,80,44};
        double totalHelium = 22200000000000d;
        // TODO check for non-bought ones
        Perks perks = new Perks(perkArray, totalHelium);
        PerksDeterminator pD = new PerksDeterminator(perks);
        pD.printPerksToFile();
        Perks result = pD.determinePerks();
        for (int x = 0; x < Perk.values().length; x++) {
            System.out.print(Perk.values()[x].name() + " : "
                    + result.getLevel(Perk.values()[x]));
        }
    }

    public PerksDeterminator(final Perks perks) {
        this.perks = perks;
    }

    public Perks determinePerks() {
        Perks savedPerks = new Perks(perks);
        //TrimpsSimulation tS = new TrimpsSimulation(savedPerks,false, new AveragedZoneSimulation());
        ZoneSimulation zS = new ProbabilisticZoneModel(
				TrimpsSimulation.critChance, 
				TrimpsSimulation.critDamage, 
				TrimpsSimulation.okFactor);
        TrimpsSimulation tS = new TrimpsSimulation(savedPerks,false,zS);
         SimulationResult sR = tS.runSimulation();
         double beforeBuyHeHrOverTime = getHeHrOverTime(tS.runSimulation());
        while (true) {
            long time = System.nanoTime();
            int bestPerk = 0;
            int count = 0;
            double highestHeHrIncreasePerHelium = 0;
            List<SimulationThread> threads = new ArrayList<>();
            for (Perk p : Perk.values()) {
                Perks usePerks = new Perks(savedPerks);
                if (usePerks.buyPerk(p, p.levelIncrease)) {
                    SimulationThread sT = new SimulationThread(usePerks, count,
                            beforeBuyHeHrOverTime,
                            savedPerks.perkCost(p, p.levelIncrease)
                            ,zS
                            );
                    threads.add(sT);
                }
                count++;
            }
            for (SimulationThread sT : threads) {
                sT.start();
            }
            for (int x = 0; x < threads.size(); x++) {
                try {
                    threads.get(x).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                double heHrIncreasePerHelium = threads.get(x)
                        .getHeHrOverTimePerHeliumSpent();
                System.out.println(heHrIncreasePerHelium);
                int perkPosition = threads.get(x).perkPosition;
                if (heHrIncreasePerHelium > highestHeHrIncreasePerHelium) {
                    highestHeHrIncreasePerHelium = heHrIncreasePerHelium;
                    beforeBuyHeHrOverTime = getHeHrOverTime(threads.get(x).sR);
                    bestPerk = perkPosition;
                }
            }
            System.out.println((System.nanoTime() - time) / 1000000);
            if (highestHeHrIncreasePerHelium > 0) {
                savedPerks.buyPerk(Perk.values()[bestPerk],
                        Perk.values()[bestPerk].levelIncrease);
                perks = savedPerks;
                printPerksToFile();
            } else {
                break;
            }
        }
        return perks;
    }
    
    private void comparePow(){
        Perks perks = new Perks(new int[]{0,0,0,0,0,0,0,0,0,0,0},1000000000000000000d);
        int pow1 = 0;
        int pow2 = 0;
        double comCost = 0;
        while (pow2<=100000){
            double pow1eff = (1+(pow1+1)*0.05)*(1+0.01*pow2)/(comCost+perks.perkCost(Perk.POWER, 1));
            double pow2eff = (1+pow1*0.05)*(1+0.01*(pow2+1))/(comCost+perks.perkCost(Perk.POWER2, 1));
            if (pow1eff>pow2eff){
                comCost+=perks.perkCost(Perk.POWER, 1);
                perks.buyPerk(Perk.POWER, 1);
                pow1++;
                System.out.println(pow2);
            }
            else{
                comCost+=perks.perkCost(Perk.POWER2, 1);
                perks.buyPerk(Perk.POWER2, 1);
                pow2++;
            }
        }
    }

    private void printPerksToFile() {
        PrintWriter writer = null;
        ;
        try {
            writer = new PrintWriter(
                    System.getProperty("user.home") + "/Desktop/" + "perks.txt",
                    "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        StringBuilder sB = new StringBuilder();
        sB.append("{");
        for (int x = 0; x < Perk.values().length; x++) {
            writer.println(Perk.values()[x].name() + " : "
                    + perks.getLevel(Perk.values()[x]));
            sB.append(perks.getLevel(Perk.values()[x]));
            sB.append(",");

        }
        sB.deleteCharAt(sB.length() - 1);
        sB.append("}");
        writer.println(sB.toString());
        writer.close();
    }
    
    
    private double getHeHrOverTime(final SimulationResult sR){
        return Math.pow((1+sR.heHr*sR.time),(24/sR.time));
    }

    public class SimulationThread extends Thread {

        private Perks perks;
        private int perkPosition;
        private double heHrOverTimeBeforeBuy;
        private double buyCost;
        ZoneSimulation zS;
        SimulationResult sR;

        public SimulationThread(final Perks perks, final int perkPosition,
                final double heHrOverTimeBeforeBuy, final double buyCost
                ,final ZoneSimulation zS
                ) {
            this.perks = perks;
            this.perkPosition = perkPosition;
            this.heHrOverTimeBeforeBuy = heHrOverTimeBeforeBuy;
            this.buyCost = buyCost;
            this.zS = zS;
        }

        public void run() {
            TrimpsSimulation tS = new TrimpsSimulation(perks,false, zS);
            sR = tS.runSimulation();
        }

        private double getHeHrOverTimePerHeliumSpent() {
            return (getHeHrOverTime(sR) / heHrOverTimeBeforeBuy) / (perks.getSpentHelium()
                    / (perks.getSpentHelium() - buyCost));
        }
    }
}
