package onepoint.project.modules.project;

import java.util.Date;

public interface OpWorkPeriodIfc {

   public abstract java.sql.Date getStart();

   public abstract long getWorkingDays();

   public abstract double getBaseEffort();

   public OpActivityIfc getActivity();

   public int countWorkDays();
   
   public int countWorkDaysInPeriod(Date start, Date finish);
}