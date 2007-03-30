/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import onepoint.persistence.OpObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing the type of a report.
 *
 * @author horia.chiorean
 */
public class OpReportType extends OpObject {
   
   public final static String REPORT_TYPE = "OpReportType";

   /**
    * The name of the report.
    */
   private String name = null;

   /**
    * The set of reports this type can have.
    */
   private Set reports = new HashSet();

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
    * Gets the set of reports.
    * @return a <code>Set</code> of <code>OpReport</code> entities.
    */
   public Set getReports() {
      return reports;
   }

   /**
    * Sets the set of reports.
    * @param reports a <code>Set</code> of <code>OpReport</code> entities.
    */
   public void setReports(Set reports) {
      this.reports = reports;
   }
}
