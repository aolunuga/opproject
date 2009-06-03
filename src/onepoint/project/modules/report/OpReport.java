/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import java.util.HashSet;
import java.util.Set;

import onepoint.project.modules.documents.OpDocument;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

/**
 * Entity representing a report.
 *
 * @author horia.chiorean
 */
public class OpReport extends OpDocument implements OpPermissionable {

   public final static String REPORT = "OpReport";

   /**
    * The type of the report.
    */
   private OpReportType type = null;

   /**
    * The <code>OpProjectNode</code> on which the report is defined.
    */
   private OpProjectNode project = null;

   private Set<OpPermission> permissions;

   private String[] KNOWN_EXTENSIONS = {"pdf", "xls", "html", "csv", "xml", "rtf", "txt"};
   /**
    * Gets the type of the report.
    *
    * @return a <code>OpReportType</code> object.
    */
   public OpReportType getType() {
      return type;
   }

   /**
    * Sets the type of the report.
    *
    * @param type a <code>OpReportType</code> object.
    */
   public void setType(OpReportType type) {
      this.type = type;
   }

   /**
    * Gets the <code>OpProjectNode</code> for which on which the report is defined.
    *
    * @return a <code>OpProjectNode</code> object.
    */
   public OpProjectNode getProject() {
      return project;
   }

   /**
    * Sets the <code>OpProjectNode</code> on which the report is defined.
    *
    * @param project a <code>OpProjectNode</code> object.
    */
   public void setProject(OpProjectNode project) {
      this.project = project;
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

   public String getNameWithoutExtension() {
      String name = getName();
      if (name == null) {
         return null;
      }
      int suffixPos = name.lastIndexOf('.');
      if (suffixPos < 0) {
         return name;
      }
      String extension = name.substring(suffixPos+1);
      if (isKnownExtension(extension)) {
         return name.substring(0, suffixPos);
      }
      return name;
   }

   private boolean isKnownExtension(String extension) {
      extension = extension.toLowerCase();
      for (int pos = 0; pos < KNOWN_EXTENSIONS .length; pos++) {
         if (KNOWN_EXTENSIONS[pos].equals(extension)) {
            return true;
         }
      }
      return false;
   }

   public String getNameExtension() {
      String name = getName();
      if (name == null) {
         return null;
      }
      int suffixPos = name.lastIndexOf('.');
      if (suffixPos < 0) {
         return name;
      }
      String extension = name.substring(suffixPos+1);
      if (isKnownExtension(extension)) {
         return extension;
      }

      return null;
   }
}
