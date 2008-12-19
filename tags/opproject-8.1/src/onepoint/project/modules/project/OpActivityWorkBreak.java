package onepoint.project.modules.project;


public class OpActivityWorkBreak extends OpWorkBreak {

   private OpProjectPlan projectPlan = null;

   public OpActivityWorkBreak() {
   }
   
   public OpActivityWorkBreak(double start, double duration) {
      super(start, duration);
      setProjectPlan(projectPlan);
   }
   
   public OpActivity getActivity() {
      return (OpActivity) super.getActivity();
   }
   public void setActivity(OpActivity activity) {
      super.setActivity(activity);
   }
   protected OpProjectPlan getProjectPlan() {
      return projectPlan;
   }
   protected void setProjectPlan(OpProjectPlan projectPlan) {
      this.projectPlan = projectPlan;
   }
}
