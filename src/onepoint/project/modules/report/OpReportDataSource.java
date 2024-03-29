/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.report;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.resource.XLocalizer;

/**
 * Data source for Jasper Reports.
 *
 * @author horia.chiorean
 */
public class OpReportDataSource implements JRDataSource {

   /**
    * This class's logger.
    */
   private static final XLog logger = XLogFactory.getLogger(OpReportDataSource.class);

   /**
    * The map of [reportFieldName, reportFieldIndex] pairs.
    */
   protected Map<String, Integer> reportFields = null;

   /**
    * An iterator over the results of a query.
    */
   protected Iterator queryIterator = null;

   /**
    * A localizer user for i18n resources.
    */
   private XLocalizer localizer = null;

   /**
    * The current value of the iterator.
    */
   protected List currentValue = null;


   protected Map parameters;
   protected OpBroker broker;
   protected OpProjectSession session;


   public OpReportDataSource() {
   }

   public void setReportFields(Map reportFields) {
      this.reportFields = reportFields;
   }

   public void setQueryIterator(Iterator queryIterator) {
      this.queryIterator = queryIterator;
   }

   public Iterator getQueryIterator() {
      return queryIterator;
   }
   
   public void setLocalizer(XLocalizer localizer) {
      this.localizer = localizer;
   }


   /**
    * Creates a new report datasource.
    *
    * @param reportFields  a <code>Map</code> of [reportFieldName, reportFieldIndex] pairs, where the reportFieldName
    *                      represents the name of the field (in the report) and reportFieldIndex represents the index of the field in the
    *                      query result.
    * @param queryIterator a <code>Iterator</code> over the results of a query.
    * @param localizer     a <code>XLocalizer</code> object user for localizing strings.
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
         currentValue = Arrays.asList(((Object[]) queryIterator.next()));
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
      if (fieldIndex >= currentValue.size()) {
         logger.error("Field " + jrField.getName() + " not found in current row");
         return null;
      }
      Object value = currentValue.get(fieldIndex);
      if (value instanceof String && localizer != null) {
         return localizer.localize((String) value);
      }
      return value;
   }

   public void setParameters(Map param) {
      this.parameters = param;
   }

   public Map getParameters() {
      return parameters;
   }
   
   public void setBrokerAndSession(OpBroker broker, OpProjectSession session) {
      this.broker = broker;
      this.session = session;
   }

   public void init() {
   }

   public Map getReportParameters() {
      return null;
   }
}
