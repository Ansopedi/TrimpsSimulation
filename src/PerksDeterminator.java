import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PerksDeterminator {
    private Perks perks;

    public static void main(String[] args) {
        // TODO fix all 0 bug
        int[] perkArray = new int[] { 80, 80, 80, 90, 40000, 20000, 9000, 27000,
                59, 80, 44 };
        double totalHelium = 24900000000000d;
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

    public static double[] tsFactorsFromPerks(Perks perks) {
    	double[] res = new double[7];
    	int[] perkLevels = perks.getPerkLevels();
        res[0] = (1 + 0.05 * perkLevels[Perk.POWER.ordinal()])
				* (1 + 0.01 * perkLevels[Perk.POWER2.ordinal()]);
        res[1] = (1 + 0.05 * perkLevels[Perk.MOTIVATION.ordinal()])
				* (1 + 0.01 * perkLevels[Perk.MOTIVATION2.ordinal()]);	
        res[2] = Math.pow(1.1, perkLevels[Perk.CARPENTRY.ordinal()])
				* (1 + 0.0025 * perkLevels[Perk.CARPENTRY2.ordinal()]);
        res[3] = (1 + 0.05 * perkLevels[Perk.LOOTING.ordinal()])
				* (1 + 0.0025 * perkLevels[Perk.LOOTING2.ordinal()]);
        res[4] = (1 + 0.25 * Math.pow(.98, perkLevels[Perk.COORDINATED.ordinal()]));
        res[5] = Math.pow(0.95, perkLevels[Perk.ARTISANISTRY.ordinal()]);
        res[6] = Math.pow(0.95, perkLevels[Perk.RESOURCEFUL.ordinal()]);
        return res;
    }
    
    public Perks determinePerks() {
        Perks savedPerks = new Perks(perks);
        ZoneSimulation zS = new ProbabilisticZoneModel(
                TrimpsSimulation.critChance, TrimpsSimulation.critDamage,
                TrimpsSimulation.okFactor);
        double[] tsFactors = tsFactorsFromPerks(perks);
        TrimpsSimulation tS = new TrimpsSimulation(tsFactors, false, zS);
        SimulationResult prev = tS.runSimulation();
        while (true) {
            long time = System.nanoTime();
            int bestPerk = 0;
            int count = 0;
            double highestRunEfficiency = -10;
            List<SimulationThread> threads = new ArrayList<>();
            for (Perk p : Perk.values()) {
                for (int i : p.levelIncreases) {
                    Perks usePerks = new Perks(savedPerks);
                    if (usePerks.buyPerk(p, i)) {
                        SimulationThread sT = new SimulationThread(usePerks, savedPerks,
                                count,i, zS, prev);
                        threads.add(sT);
                    }
                }
                count++;
            }
            for (SimulationThread sT : threads) {
                sT.start();
            }
            int bestBuyAmount = 0;
            for (int x = 0; x < threads.size(); x++) {
                try {
                    threads.get(x).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                double runEfficiency = threads.get(x).getRelativeEfficiency();
                System.out.println(runEfficiency);
                int perkPosition = threads.get(x).perkPosition;
                if (runEfficiency > highestRunEfficiency) {
                    prev = threads.get(x).sR;
                    bestBuyAmount= threads.get(x).perkIncrease;
                    highestRunEfficiency = runEfficiency;
                    bestPerk = perkPosition;
                }
            }
            System.out.println((System.nanoTime() - time) / 1000000);
            if (highestRunEfficiency > 0) {
                savedPerks.buyPerk(Perk.values()[bestPerk],
                        bestBuyAmount);
                perks = savedPerks;
                printPerksToFile();
            } else {
                break;
            }
        }
        return perks;
    }

    private void comparePow() {
        Perks perks = new Perks(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                1000000000000000000d);
        int pow1 = 0;
        int pow2 = 0;
        double comCost = 0;
        while (pow2 <= 100000) {
            double pow1eff = (1 + (pow1 + 1) * 0.05) * (1 + 0.01 * pow2)
                    / (comCost + perks.perkCost(Perk.POWER, 1));
            double pow2eff = (1 + pow1 * 0.05) * (1 + 0.01 * (pow2 + 1))
                    / (comCost + perks.perkCost(Perk.POWER2, 1));
            if (pow1eff > pow2eff) {
                comCost += perks.perkCost(Perk.POWER, 1);
                perks.buyPerk(Perk.POWER, 1);
                pow1++;
                System.out.println(pow2);
            } else {
                comCost += perks.perkCost(Perk.POWER2, 1);
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

    public class SimulationThread extends Thread {

        private Perks perks;
        private Perks prevPerks;
        private int perkPosition;
        private int perkIncrease;
        private SimulationResult sR;
        private SimulationResult prev;
        private ZoneSimulation zS;

        public SimulationThread(final Perks perks, final Perks prevPerks,
        		final int perkPosition,
                final int perkIncrease, final ZoneSimulation zS,
                final SimulationResult prev) {
            this.perks = perks;
            this.prevPerks = prevPerks;
            this.perkPosition = perkPosition;
            this.perkIncrease = perkIncrease;
            this.zS = zS;
            this.prev = prev;
        }

        public void run() {
            TrimpsSimulation tS = new TrimpsSimulation(tsFactorsFromPerks(perks), false, zS);
            sR = tS.runSimulation();
        }

        private double getRelativeEfficiency() {
            return logOfBase(
                    perks.getSpentHelium() / prevPerks.getSpentHelium(),
                    getRunEfficiency(sR) / getRunEfficiency(prev));
        }

        private double getRunEfficiency(final SimulationResult sR) {
            return sR.helium / sR.hours;
        }

        private double logOfBase(double base, double num) {
            return Math.log(num) / Math.log(base);
        }

    }
}
