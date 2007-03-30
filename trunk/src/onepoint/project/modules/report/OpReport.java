/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.project.modules.documents.OpDocument;

/**
 * Entity representing the a report.
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
    * Gets the type of the report.
    * @return a <code>OpReportType</code> object.
    */
   public OpReportType getType() {
      return type;
   }

   /**
    * Sets the type of the report.
    * @param type a <code>OpReportType</code> object.
    */
   public void setType(OpReportType type) {
      this.type = type;
   }

}
