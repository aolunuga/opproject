/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.persistence.OpTypeManager;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.work.OpCostRecord;

public class OpAttachment extends OpObject {
   
   public final static String ATTACHMENT = "OpAttachment";

   public final static String NAME = "Name";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String CONTENT = "Content";

   private String name;
   private boolean linked;
   private String location;
   private OpContent content;
   private OpActivity activity;
   private OpCostRecord costRecord;
   private OpProjectNode projectNode;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setLinked(boolean linked) {
      this.linked = linked;
   }

   public boolean getLinked() {
      return linked;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public String getLocation() {
      return location;
   }

   public void setContent(OpContent content) {
      this.content = content;
   }

   public OpContent getContent() {
      return content;
   }

   /**
    * Returns the <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    *    an <code>OpCostRecord</code> object)to which the <code>OpAttachment</code> is associated.
    *
    * @return the <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    *         an <code>OpCostRecord</code> object)to which the <code>OpAttachment</code> is associated.
    */

   public OpObject getObject() {
      if (getActivity() != null) {
         return getActivity();
      }
      return getCostRecord();
   }

   /**
    * Sets an <code>OpObject</code> (which is either an <code>OpActivity</code>, a <code>OpProjectNode</code> or
    * a <code>OpCostRecord</code>) on the attachment.
    *
    * @param object - the <code>OpActivity</code>, <code>OpProjectNode</code> or <code>OpCostRecord</code> object
    *               which will be set on the attachment.
    */
   public void setObject(OpObject object) {
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpActivity.ACTIVITY)) {
         setActivity((OpActivity) object);
      }
      if (OpTypeManager.getPrototypeForObject(object).getName().equals(OpCostRecord.COST_RECORD)) {
         setCostRecord((OpCostRecord) object);
      }
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivity getActivity() {
      return activity;
   }

   public OpCostRecord getCostRecord() {
      return costRecord;
   }

   public void setCostRecord(OpCostRecord costRecord) {
      this.costRecord = costRecord;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }
}
