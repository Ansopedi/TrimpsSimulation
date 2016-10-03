
public class SimulationResult {
     public final double helium;
     public final double hours;
     public final int zone;
     public final double motiFraction;
     
     public double getHehr() {
    	 return helium / hours;
     }
     
     public double getMetric(boolean deepRun) {
    	 return deepRun ? zone : getHehr();
     }

     public SimulationResult(final double helium, final double hours, final int zone, 
    		 final double motiFraction){
         this.helium = helium;
         this.hours = hours;
         this.zone = zone;
         this.motiFraction = motiFraction;
     }
}
