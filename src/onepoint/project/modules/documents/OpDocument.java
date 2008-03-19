/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
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
   public final static String NAME = "Name";

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
    * Gets the name of the document.
    *
    * @return a <code>String</code> representing the name of the document.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the name of the document.
    *
    * @param name a <code>String</code> representing the name of the document.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the content of the document.
    *
    * @return a <code>OpContent</code> object representing the content of the document.
    */
   public OpContent getContent() {
      return content;
   }

   /**
    * Sets the content of the document.
    *
    * @param content a <code>OpContent</code> object representing the content of the document.
    */
   public void setContent(OpContent content) {
      this.content = content;
   }

   /**
    * Gets the creator of the document.
    *
    * @return a <code>OpUser</code> representing the creator of the document.
    */
   public OpUser getCreator() {
      return creator;
   }

   /**
    * Sets the creator of the document.
    *
    * @param creator a <code>OpUser</code> representing the creator of the document.
    */
   public void setCreator(OpUser creator) {
      this.creator = creator;
   }
}