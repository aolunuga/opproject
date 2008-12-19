package onepoint.project.modules.project;

public class OpActivityVersionWorkBreak extends OpWorkBreak {

   private OpProjectPlanVersion planVersion = null;
   
   private OpActivityVersionWorkBreak() {
      // hibernate
   }
   
   public OpActivityVersionWorkBreak(double start, double duration) {
      super(start, duration);
      setPlanVersion(planVersion);
   }
   
   public OpActivityVersion getActivity() {
      return (OpActivityVersion) super.getActivity();
   }
   public void setActivity(OpActivityVersion activity) {
      super.setActivity(activity);
   }
   public OpProjectPlanVersion getPlanVersion() {
      return planVersion;
   }
   public void setPlanVersion(OpProjectPlanVersion planVersion) {
      this.planVersion = planVersion;
   }
}
