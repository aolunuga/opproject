package onepoint.project.modules.calendars;


public interface OpHasWorkCalendar {
   
   public String locator();

   public abstract OpWorkCalendar getWorkCalendar();

   public abstract void setWorkCalendar(OpWorkCalendar workCalendar);

}