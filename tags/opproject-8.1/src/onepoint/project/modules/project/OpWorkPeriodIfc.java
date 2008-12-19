package onepoint.project.modules.project;

import java.sql.Date;

public interface OpWorkPeriodIfc {

   public abstract Date getStart();

   public abstract long getWorkingDays();

   public abstract double getBaseEffort();

   public OpActivityIfc getActivity();

}