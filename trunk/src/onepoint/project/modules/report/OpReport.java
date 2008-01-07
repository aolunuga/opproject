/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.project.modules.documents.OpDocument;
import onepoint.project.modules.project.OpProjectNode;

/**
 * Entity representing a report.
 *
 * @author horia.chiorean
 */
public class OpReport extends OpDocument {

   public final static String REPORT = "OpReport";

   /**
    * The type of the report.
    */
   private OpReportType type = null;

   /**
    * The <code>OpProjectNode</code> on which the report is defined.
    */
   private OpProjectNode project = null;

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
}
