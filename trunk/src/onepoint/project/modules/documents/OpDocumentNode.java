/**
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

import java.sql.Date;

/**
 * Document node entity.
 *
 * @author mihai.costin
 */
public class OpDocumentNode extends OpObject {

   public final static String DOCUMENT_NODE = "OpDocumentNode";
   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String LINKED = "Linked";
   public final static String LOCATION = "Location";
   public final static String FILE = "File";

   /**
    * The name of the document node.
    */
   private String name = null;

   /**
    * The description of the document node.
    */
   private String description;

   /**
    * Value indicating whether the document node is a web page or a file.
    */
   private boolean linked;

   /**
    * The location of the document node's content.
    */
   private String location;

   /**
    * The content of the document node.
    */
   private OpContent content = null;

   /**
    * The creator of the document node.
    */
   private OpUser creator = null;

   /**
    * The parent folder of the current document node.
    */
   private OpFolder folder;

   /**
    * Gets the name of the document node.
    *
    * @return a <code>String</code> representing the name of the document node.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the name of the document node.
    *
    * @param name a <code>String</code> representing the name of the document node.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the description of the document node.
    *
    * @return a <code>String</code> representing the description of the document node.
    */
   public String getDescription() {
      return description;
   }

   /**
    * Sets the description of the document node.
    *
    * @param description a <code>String</code> representing the description of the document node.
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * Gets the linked property of the document node.
    *
    * @return a <code>boolean</code> representing the linked property of the document node.
    */
   public boolean isLinked() {
      return linked;
   }

   /**
    * Sets the linked property of the document node.
    *
    * @param linked a <code>boolean</code> representing the linked property of the document node.
    */
   public void setLinked(boolean linked) {
      this.linked = linked;
   }

   /**
    * Gets the location of the document node's content.
    *
    * @return a <code>String</code> representing the location of the document node's content.
    */
   public String getLocation() {
      return location;
   }

   /**
    * Sets the location of the document node's content.
    *
    * @param location a <code>String</code> representing the location of the document node's content.
    */
   public void setLocation(String location) {
      this.location = location;
   }

   /**
    * Gets the creation date of the document node.
    *
    * @return a <code>Date</code> representing the creation date of the document node.
    */
   public Date getCreatedOn() {
      return new Date(getCreated().getTime());
   }

   /**
    * Gets the content of the document node.
    *
    * @return a <code>OpContent</code> object representing the content of the document node.
    */
   public OpContent getContent() {
      return content;
   }

   /**
    * Sets the content of the document node.
    *
    * @param content a <code>OpContent</code> object representing the content of the document node.
    */
   public void setContent(OpContent content) {
      this.content = content;
   }

   /**
    * Gets the creator of the document node.
    *
    * @return a <code>OpUser</code> representing the creator of the document node.
    */
   public OpUser getCreator() {
      return creator;
   }

   /**
    * Sets the creator of the document node.
    *
    * @param creator a <code>OpUser</code> representing the creator of the document node.
    */
   public void setCreator(OpUser creator) {
      this.creator = creator;
   }

   /**
    * Gets the parent folder of the document node.
    *
    * @return a <code>OpFolder</code> representing the parent folder of the document node.
    */
   public OpFolder getFolder() {
      return folder;
   }

   /**
    * Sets the parent folder of the document node.
    *
    * @param folder a <code>OpFolder</code> representing the parent folder of the document node.
    */
   public void setFolder(OpFolder folder) {
      this.folder = folder;
   }
}