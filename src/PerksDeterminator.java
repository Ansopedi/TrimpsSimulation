import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PerksDeterminator {
    private Perks perks;

    public static void main(String[] args) {
        int[] perkArray = new int[] {91,88,87,98,73200,42300,11800,40700,59,84,46};
        double totalHelium = 15400000000000d;
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
            double highestHeHrPercentage = 0;
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
                double heHrPercent = threads.get(x).heHrPercent;
                int perkPosition = threads.get(x).perkPosition;
                if (heHrPercent > highestHeHrPercentage) {
                    highestHeHrPercentage = heHrPercent;
                    bestPerk = perkPosition;
                }
            }
            if (highestHeHrPercentage > 0) {
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
        private double heHrPercent;

        public SimulationThread(final Perks perks, final int perkPosition) {
            this.perks = perks;
            this.perkPosition = perkPosition;
            heHrPercent = 0;
        }

        public void run() {
            TrimpsSimulation tS = new TrimpsSimulation(perks);
            heHrPercent = tS.runSimulation();
        }
    }
}
