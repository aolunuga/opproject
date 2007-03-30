/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.resource.XLocalizer;

import java.util.Iterator;
import java.util.Map;

/**
 * Data source for Jasper Reports.
 *
 * @author horia.chiorean
 */
public class OpReportDataSource implements JRDataSource {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReportDataSource.class, true);

   /**
    * The map of [reportFieldName, reportFieldIndex] pairs.
    */
   private Map reportFields = null;

   /**
    * An iterator over the results of a query.
    */
   private Iterator queryIterator = null;

   /**
    * A localizer user for i18n resources.
    */
   private XLocalizer localizer = null;

   /**
    * The current value of the iterator.
    */
   private Object[] currentValue = null;

   /**
    * Creates a new report datasource.
    * 
    * @param reportFields a <code>Map</code> of [reportFieldName, reportFieldIndex] pairs, where the reportFieldName
    * represents the name of the field (in the report) and reportFieldIndex represents the index of the field in the
    * query result.
    *
    * @param queryIterator a <code>Iterator</code> over the results of a query.
    * @param localizer a <code>XLocalizer</code> object user for localizing strings.
    */
   public OpReportDataSource(Map reportFields, Iterator queryIterator, XLocalizer localizer) {
      this.reportFields = reportFields;
      this.queryIterator = queryIterator;
      this.localizer = localizer;
   }

   /**
    * @see net.sf.jasperreports.engine.JRDataSource#next()
    */
   public boolean next()
        throws JRException {
      if (queryIterator.hasNext()) {
         currentValue = (Object[]) queryIterator.next();
         return true;
      }
      return false;
   }

   /**
    * @see JRDataSource#getFieldValue(net.sf.jasperreports.engine.JRField)
    */
   public Object getFieldValue(JRField jrField)
        throws JRException {
      if (currentValue == null) {
         logger.error("There is no report data !");
         return null;
      }
      Integer fieldIndex = ((Integer) reportFields.get(jrField.getName()));
      if (fieldIndex == null) {
         logger.error("Field " + jrField.getName() + " not found among report fields");
         return null;
      }
      if (fieldIndex.intValue() >= currentValue.length) {
         logger.error("Field " + jrField.getName() + " not found in current row");
         return null;
      }
      Object value = currentValue[fieldIndex.intValue()];
      if (value instanceof String && localizer != null) {
         return localizer.localize((String) value);
      }
      return value;
   }
}
