package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

public abstract class OpWorkBreak extends OpObject {
   
   protected OpActivityIfc activity = null;
   private double start = 0d;
   private double duration = 0d;
   
   protected OpWorkBreak() {
   }
   
   public OpWorkBreak(double start, double duration) {
      setStart(start);
      setDuration(duration);
      setActivity(null);
   }
   
   public double getStart() {
      return start;
   }
   public void setStart(double start) {
      this.start = start;
   }
   public double getDuration() {
      return duration;
   }
   public void setDuration(double duration) {
      this.duration = duration;
   }
   protected OpActivityIfc getActivity() {
      return activity;
   }
   protected void setActivity(OpActivityIfc activity) {
      this.activity = activity;
   }
}
