/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.schedule;

import onepoint.express.XEventHandler;
import onepoint.persistence.OpObject;

import java.sql.Date;

public class OpSchedule extends OpObject {

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String START = "Start";
   public final static String UNIT = "Unit";
   public final static String INTERVAL = "Interval";
   public final static String MASK = "Mask"; // *** Maybe find a better name?
   public final static String LAST_EXECUTED = "LastExecuted";

   private String name;
   private String description;
   private Date start;
   private int unit;
   private int interval;
   private int mask;
   private Date lastExecuted;
   private XEventHandler handler;

   public void setHandler(XEventHandler handler) {
      this.handler = handler;
   }

   public XEventHandler getHandler() {
      return handler;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setStart(Date time) {
      start = time;
   }

   public Date getStart() {
      return start;
   }

   public void setUnit(int unit) {
      this.unit = unit;
   }

   public int getUnit() {
      return unit;
   }

   public void setInterval(int interval) {
      this.interval = interval;
   }

   public int getInterval() {
      return interval;
   }

   public void setMask(int mask) {
      this.mask = mask;
   }

   public int getMask() {
      return mask;
   }

   public void setLastExecuted(Date last_executed) {
      this.lastExecuted = last_executed;
   }

   public Date getLastExecuted() {
      return lastExecuted;
   }

}
