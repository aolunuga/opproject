/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.modules.user.OpUser;

/**
 * Folder entity.
 *
 * @author mihai.costin
 */
public class OpFolder extends OpObject implements OpPermissionable {

   public final static String FOLDER = "OpFolder";
   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";

   // Root folder
   public final static String ROOT_FOLDER_NAME = "${RootFolderName}";
   public final static String ROOT_FOLDER_DESCRIPTION = "${RootFolderDescription}";

   /**
    * The name of the folder.
    */
   private String name;

   /**
    * The description of the folder.
    */
   private String description;

   /**
    * The creation time of the folder.
    */
   private Date createdOn;

   /**
    * The creator of the folder.
    */
   private OpUser creator;

   /**
    * The documents the folder contains.
    */
   private Set<OpDocumentNode> documentNodes;

   /**
    * The parent folder of the current folder.
    */
   private OpFolder superFolder;

   /**
    * The subfolders of the current folder.
    */
   private Set<OpFolder> subFolders;

   /**
    * The project node to which the folder belongs.
    */
   private OpProjectNode projectNode;
   private Set<OpPermission> permissions;

   /**
    * Gets the name of the folder.
    * @return a <code>String</code> representing the name of the folder.
    */
   public String getName() {
      return name;
   }

   /**
    * Sets the name of the folder.
    * @param name a <code>String</code> representing the name of the folder.
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Gets the description of the folder.
    * @return a <code>String</code> representing the description of the folder.
    */
   public String getDescription() {
      return description;
   }

   /**
    * Sets the description of the folder.
    * @param description a <code>String</code> representing the description of the folder.
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * Gets the creation date of the folder.
    * @return a <code>Date</code> representing the creation date of the folder.
    */
   public Date getCreatedOn() {
      return createdOn;
   }

   /**
    * Sets the creation date of the folder.
    * @param createdOn a <code>Date</code> representing the creation date of the folder.
    */
   public void setCreatedOn(Date createdOn) {
      this.createdOn = createdOn;
   }

   /**
    * Gets the creator of the folder.
    * @return a <code>OpUser</code> representing the creator of the folder.
    */
   public OpUser getCreator() {
      return creator;
   }

   /**
    * Sets the creator of the folder.
    * @param creator a <code>OpUser</code> representing the creator of the folder.
    */
   public void setCreator(OpUser creator) {
      this.creator = creator;
   }

   /**
    * Gets the document nodes belonging to the current folder.
    * @return a <code>Set<OpDocumentNode></code> representing the document nodes belonging to the current folder.
    */
   public Set<OpDocumentNode> getDocumentNodes() {
      return documentNodes;
   }

   /**
    * Sets the document nodes belonging to the current folder.
    * @param documentNodes a <code>Set<OpDocumentNode></code> representing the document nodes belonging to the current
    *    folder.
    */
   public void setDocumentNodes(Set<OpDocumentNode> documentNodes) {
      this.documentNodes = documentNodes;
   }

   /**
    * Gets the subfolders belonging to the current folder.
    * @return a <code>Set<OpFolder></code> representing the subfolders belonging to the current folder.
    */
   public Set<OpFolder> getSubFolders() {
      return subFolders;
   }

   /**
    * Sets the subfolders belonging to the current folder.
    * @param subFolders a <code>Set<OpFolder></code> representing the subfolders belonging to the current folder.
    */
   public void setSubFolders(Set<OpFolder> subFolders) {
      this.subFolders = subFolders;
   }

   /**
    * Gets the parent folder of the current folder.
    * @return a <code>OpFolder</code> representing the parent folder of the current folder.
    */
   public OpFolder getSuperFolder() {
      return superFolder;
   }

   /**
    * Sets the parent folder to which the folder belongs.
    * @param superFolder a <code>OpFolder</code> representing the parent folder to which the folder belongs.
    */
   public void setSuperFolder(OpFolder superFolder) {
      this.superFolder = superFolder;
   }

    /**
    * Gets the projet node to which the folder belongs.
    * @return a <code>OpProjectNode</code> representing the projet node to which the folder belongs.
    */
   public OpProjectNode getProjectNode() {
      return projectNode;
   }

   /**
    * Sets the project node to which the folder belongs.
    * @param projectNode a <code>OpProjectNode</code> representing the projet node to which the folder belongs.
    */
   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }
   
   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }

   public void addPermission(OpPermission permission) {
      Set<OpPermission> perm = getPermissions();
      if (perm == null) {
         perm = new HashSet<OpPermission>();
         setPermissions(perm);
      }
      perm.add(permission);
      permission.setObject(this);
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
         perm.remove(opPermission);
      }
      opPermission.setObject(null);
   }

}