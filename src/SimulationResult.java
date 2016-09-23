
public class SimulationResult {
     public final double heHrPercentage;
     public final double time;
     public final Perks perks;
     public final int zone;
     
     public SimulationResult(final double heHrPercentage, final double time, final Perks perks, final int zone){
         this.heHrPercentage = heHrPercentage;
         this.time = time;
         this.perks = perks;
         this.zone = zone;
     }
}
