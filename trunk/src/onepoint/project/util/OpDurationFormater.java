package onepoint.project.util;

public class OpDurationFormater {
   public static final long MILISECONDS_PER_SECOND = 1000;
   public static final long SECONDS_PER_MINUTE = 60;
   public static final long MINUTES_PER_HOUR = 60;
   public static final long HOURS_PER_DAY = 24;

   public static final int MILISECOND  = 0;
   public static final int SECOND      = 1;
   public static final int MINUTE      = 2;
   public static final int HOUR        = 3;
   public static final int DAY         = 4;


   public static final String PATTERNS[] = {
       "@ms", "@s", "@m", "@h", "@d"
   };
   private static final long[] AMOUNTS = {
       MILISECONDS_PER_SECOND,
       SECONDS_PER_MINUTE,
       MINUTES_PER_HOUR,
       HOURS_PER_DAY
   };
   private static long[] times = new long[5];
   private long time;
   private String pattern;
   private boolean detail = false;

   public OpDurationFormater() {
   }

   public OpDurationFormater(long time, String pattern, boolean detail) {
      this.time = time;
      this.pattern = pattern;
      this.detail = detail;
      update();
  }

   public OpDurationFormater(long time, String pattern) {
      this(time, pattern, false);
   }

   public OpDurationFormater(long time) {
      this(time, null, false);
  }

   public OpDurationFormater(long time, boolean detail) {
      this(time, null, detail);
   }

   private void update() {
       long remain = time;
       for (int i = 0; i < AMOUNTS.length; i++) {
           times[i] = remain % AMOUNTS[i];
           remain = remain / AMOUNTS[i];
       }
       times[DAY] = (int) remain;
   }

   /*  @h
    *  @M  --> Month
    *  @m  --> minute
    *  @ms --> milisecond
    *  @s  --> second
    */
   public void setPattern(String pattern) {
       this.pattern = pattern;
   }

   public long getTime() {
       return time;
   }

   public void setTime(long duration) {
       time = duration;
       update();
   }

   public long getMiliseconds() {
       return times[MILISECOND];
   }

   public long getSeconds() {
       return times[SECOND];
   }

   public long getMinutes() {
       return times[MINUTE];
   }

   public long getHours() {
       return times[HOUR];
   }

   public long getDays() {
       return times[DAY];
   }

   public void setDetail(boolean detail) {
       this.detail = detail;
   }

   private String getString() {
       StringBuffer buffer = new StringBuffer(1024);
       buffer.append(pattern);
       for (int i = 0; i < PATTERNS.length; i++) {
           int start = -1;
           int end = -1;
           while ((start = buffer.toString().indexOf(PATTERNS[i])) > -1) {
               end = start + PATTERNS[i].length();
               buffer.replace(start, end, String.valueOf(times[i]));
           }
       }
       return buffer.toString();
   }

   public String toString() {
       if (pattern != null) {
           return getString();
       }

       StringBuffer desc = new StringBuffer(256);
       if (times[DAY] > 0) {
           desc.append(checkPlural(times[DAY], "day"));
       }
       if (times[HOUR] > 0) {
           desc.append(checkPlural(times[HOUR], "hour"));
       }
       if ((times[MINUTE] > 0) || (times[DAY] == 0 && times[MINUTE] == 0)) {
           desc.append(checkPlural(times[MINUTE], "minute"));
       }
       if (detail) {
           desc.append(checkPlural(times[SECOND], "second"));
           desc.append(checkPlural(times[MILISECOND], "milisecond"));
       }
       return desc.toString();
   }

   private static String checkPlural(long amount, String unit) {
       StringBuffer desc = new StringBuffer(20);
      desc.append(amount).append(" ").append(unit);
       if (amount > 1) {
           desc.append("s");
       }
       return desc.append(" ").toString();
   }
}
