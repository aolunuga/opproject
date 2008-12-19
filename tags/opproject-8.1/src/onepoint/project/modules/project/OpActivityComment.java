/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

public class OpActivityComment extends OpObject {

   public final static String ACTIVITY_COMMENT = "OpActivityComment";

   public final static String NAME = "Name";
   public final static String TEXT = "Text";
   public final static String SEQUENCE = "Sequence";
   public final static String ACTIVITY = "Activity";
   public final static String CREATOR = "Creator";

   private String name;
   private String text;
   private int sequence;
   private OpActivity activity;
   private OpUser creator;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getText() {
      return text;
   }
   
   public void setSequence(int sequence) {
      this.sequence = sequence;
   }
   
   public int getSequence() {
      return sequence;
   }

   public void setActivity(OpActivity activity) {
      this.activity = activity;
   }

   public OpActivity getActivity() {
      return activity;
   }

   public void setCreator(OpUser creator) {
      this.creator = creator;
   }

   public OpUser getCreator() {
      return creator;
   }

}
