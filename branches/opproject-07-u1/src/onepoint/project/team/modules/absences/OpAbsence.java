/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.absences;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpResource;

import java.sql.Date;

public class OpAbsence extends OpObject {

   public final static String TYPE = "Type";
   public final static String BEGIN = "Begin";
   public final static String END = "End";
   public final static String RESOURCE = "Resource";

   // Absence types
   public final static byte UNDEFINED = 0;
   public final static byte VACATION = 1;
   public final static byte SICK_LEAVE = 2;

   private byte type;
   private Date begin;
   private Date end;
   private OpResource resource;

   public void setType(byte type) {
      this.type = type;
   }

   public byte getType() {
      return type;
   }

   public void setBegin(Date begin) {
      this.begin = begin;
   }

   public Date getBegin() {
      return begin;
   }

   public void setEnd(Date end) {
      this.end = end;
   }

   public Date getEnd() {
      return end;
   }

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

}