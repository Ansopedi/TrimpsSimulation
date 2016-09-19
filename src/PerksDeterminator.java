import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PerksDeterminator {
    private Perks perks;

    public static void main(String[] args) {
        int[] perkArray = new int[] { 91, 87, 86, 98, 73400, 40600, 10800,
                39200, 58, 83, 45 };
        double totalHelium = 17100000000000d;
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
        while (true) {
            System.out.println("iteration");
            Perks savedPerks = new Perks(perks);
            int bestPerk = 0;
            int count = 0;
            double highestHeHrIncreasePerHelium = 0;
            List<SimulationThread> threads = new ArrayList<>();
            for (Perk p : Perk.values()) {
                Perks usePerks = new Perks(savedPerks);
                if (usePerks.buyPerk(p, p.levelIncrease)) {
                    SimulationThread sT = new SimulationThread(usePerks, count);
                    threads.add(sT);
                }
                count++;
            }
            for (SimulationThread sT : threads) {
                sT.run();
            }
            for (int x = 0; x < threads.size(); x++) {
                try {
                    threads.get(x).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                double heHrIncreasePerHelium = threads.get(x).heHr;
                heHrIncreasePerHelium /= savedPerks.perkCost(Perk.values()[x],
                        Perk.values()[x].levelIncrease);
                int perkPosition = threads.get(x).perkPosition;
                if (heHrIncreasePerHelium > highestHeHrIncreasePerHelium) {
                    highestHeHrIncreasePerHelium = heHrIncreasePerHelium;
                    bestPerk = perkPosition;
                }
            }
            if (highestHeHrIncreasePerHelium > 0) {
                savedPerks.buyPerk(Perk.values()[bestPerk],
                        Perk.values()[bestPerk].levelIncrease);
                perks = savedPerks;
                System.out.println("write");
                printPerksToFile();
            } else {
                break;
            }
        }
        return perks;
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

    public class SimulationThread extends Thread {

        private Perks perks;
        private int perkPosition;
        private double heHr;

        public SimulationThread(final Perks perks, final int perkPosition) {
            this.perks = perks;
            this.perkPosition = perkPosition;
            heHr = 0;
        }

        public void run() {
            TrimpsSimulation tS = new TrimpsSimulation(perks);
            heHr = tS.runSimulation();
        }
    }
}
