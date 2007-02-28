/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

/**
 * Document entity.
 *
 * @author horia.chiorean
 */
public class OpDocument extends OpObject {
   
   public final static String DOCUMENT = "OpDocument";

   /**
    * The name of the report.
    */
   private String name = null;

   /**
    * The content of the report.
    */
   private OpContent content = null;

   /**
    * The creator of the report.
    */
   private OpUser creator = null;

   /**
    * Gets the name of the report.
    * @return a <code>String</code> representing the name of the report.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the name of the report.
    * @param name a <code>String</code> representing the name of the report.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the content of the report.
    * @return a <code>OpContent</code> object representing the content of the report.
    */
   public OpContent getContent() {
      return content;
   }

   /**
    * Sets the content of the report.
    * @param content a <code>OpContent</code> object representing the content of the report.
    */
   public void setContent(OpContent content) {
      this.content = content;
   }

   /**
    * Gets the creator of the report.
    * @return a <code>OpUser</code> representing the creator of the report.
    */
   public OpUser getCreator() {
      return creator;
   }

   /**
    * Sets the creator of the report.
    * @param creator a <code>OpUser</code> representing the creator of the report.
    */
   public void setCreator(OpUser creator) {
      this.creator = creator;
   }
}
