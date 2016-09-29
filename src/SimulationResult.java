
public class SimulationResult {
     public final double helium;
     public final double hours;
     public final int zone;
     
     public double getHehr() {
    	 return helium / hours;
     }

     public SimulationResult(final double helium, final double hours, final int zone){
         this.helium = helium;
         this.hours = hours;
         this.zone = zone;
     }
}
