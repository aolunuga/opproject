package onepoint.project.modules.resource;

import java.sql.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import onepoint.persistence.OpObject;
import onepoint.util.XCalendar;

public class OpAbsence extends OpObject {
   
   private OpResource resource = null;
   private Date start = null;
   private Date finish = null;
   
   private OpAbsenceReason reason = null;
   private String comment = null;
   
   public OpResource getResource() {
      return resource;
   }
   public void setResource(OpResource resource) {
      this.resource = resource;
   }
   public Date getStart() {
      return start;
   }
   public void setStart(Date start) {
      this.start = start;
   }
   public Date getFinish() {
      return finish;
   }
   public void setFinish(Date finish) {
      this.finish = finish;
   }
   public OpAbsenceReason getReason() {
      return reason;
   }
   public void setReason(OpAbsenceReason reason) {
      this.reason = reason;
   }
   public String getComment() {
      return comment;
   }
   public void setComment(String comment) {
      this.comment = comment;
   }
   
   public SortedSet<Date> getDays() {
      SortedSet<Date> dates = new TreeSet<Date>();
      Date d = getStart();
      while (d.before(getFinish())) {
         dates.add(d);
         d = new Date(d.getTime() + XCalendar.MILLIS_PER_DAY);
      }
      dates.add(d); // adds finish (if finish after start, otherwise we are still stuck at "start"
      return dates;
   }
}
