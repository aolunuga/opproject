/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project;

import java.lang.reflect.Method;
import java.util.Map;

import onepoint.error.XLocalizableException;
import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.forms.OpProjectFormsError;
import onepoint.project.forms.OpProjectFormsErrorMap;
import onepoint.project.forms.OpProjectInputFormatException;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.XError;
import onepoint.service.XMessage;
import onepoint.service.server.XService;
import onepoint.service.server.XSession;

public class OpProjectService extends XService {

   private static final XLog logger = XLogFactory.getLogger(OpProjectService.class);
   public static final OpProjectFormsErrorMap FORMS_ERROR_MAP = new OpProjectFormsErrorMap();

   /**
    * Rollbacks the current<code>transaction</code> and releases the <code>broker</code>. This should be extracted
    * in a helper class.
    *
    * @param broker      a <code>OpBroker</code> representing the broker
    * @param transaction a <code>OpTransaction</code> representing the current transaction
    */
   protected void finalizeSession(OpTransaction transaction, OpBroker broker) {
      logger.info("Finalizing session...");
      if (transaction != null) {
         transaction.rollbackIfNecessary();
      }
      if (broker != null) {// && broker.isOpen()) {
         broker.close();
      }
   }


   /**
    * @see onepoint.service.server.XService#findInstanceMethod(String)
    */
   protected Method findInstanceMethod(String methodName)
        throws NoSuchMethodException {
      try {
         Class clazz = this.getClass();
         return clazz.getMethod(methodName, new Class[]{OpProjectSession.class, XMessage.class});
      }
      catch (NoSuchMethodException e) {
         return super.findInstanceMethod(methodName);
      }
   }


   protected XMessage callJavaMethod(XSession session, XMessage request, Method javaMethod)
        throws Throwable {
      try {
         return super.callJavaMethod(session, request, javaMethod);
     } catch (OpProjectInputFormatException e) {
        XMessage response = new XMessage();
        makeDialogError((OpProjectSession)session, response, e);
        return response;
     }
      catch (XLocalizableException e) {
         if (e.getErrorMap() != null) {
            XMessage response = new XMessage();
            XError error = ((OpProjectSession) session).newError(e.getErrorMap(), e.getCode(), e.getParameters());
            response.setError(error);
            return response;
         }
         throw e;
      }
   }

   /**
    * @param session
    * @param reply
    * @param e
    */
   private void makeDialogError(OpProjectSession session, XMessage reply, OpProjectInputFormatException e) {
      reply.setArgument(OpProjectValidator.INVALID_FIELD_ID, e.getFieldId());
      reply.setArgument(OpProjectValidator.INVALID_FIELD_LABEL_ID, e.getFieldLabelId());
      reply.setArgument(OpProjectValidator.INVALID_TABLE_COLUMN, e.getTableColumnIndex());
      reply.setArgument(OpProjectValidator.INVALID_VALUE, e.getInvalidValue());
      if (e.getInvalidValue() == null || e.getInvalidValue().length() == 0) {
         reply.setError(session.newError(FORMS_ERROR_MAP, OpProjectFormsError.MANDATORY_VALUE));
      }
      else {
         reply.setError(session.newError(FORMS_ERROR_MAP, OpProjectFormsError.INVALID_FORMAT));
      }
   }
   
   public static void readDialogFieldsIntoMap(XComponent dialogFieldsMapDataSet, Map/*<String, Object>*/ valueMap,
         Map/*<String, Object>*/ objects) throws OpProjectInputFormatException {
      OpProjectValidator.FormatError error = OpProjectValidator.readDialogFieldsIntoMap(dialogFieldsMapDataSet, valueMap, objects);
      if (error != null) {
         throw new OpProjectInputFormatException(error);
      }
   }
   
   public static void readDialogFieldsIntoMap(String[][] dialogFieldsMap, Map/*<String, Object>*/ valueMap,
            Map/*<String, Object>*/ objects) throws OpProjectInputFormatException {
      OpProjectValidator.FormatError error = OpProjectValidator.readDialogFieldsIntoMap(dialogFieldsMap, valueMap, objects);
      if (error != null) {
         throw new OpProjectInputFormatException(error);
      }
   }
   
   public static void readDataRowIntoMap(XComponent row, XComponent dataRowMapDataSet, 
         Map<String, Object> objects) throws OpProjectInputFormatException {
      OpProjectValidator.FormatError error = OpProjectValidator.readDataRowIntoMap(row, dataRowMapDataSet, objects);
      if (error != null) {
         throw new OpProjectInputFormatException(error);
      }
   }
   
   public static void readDataRowIntoMap(XComponent row, String[][] dataRowMap, 
         Map<String, Object> objects) throws OpProjectInputFormatException {
      OpProjectValidator.FormatError error = OpProjectValidator.readDataRowIntoMap(row, dataRowMap, objects);
      if (error != null) {
         throw new OpProjectInputFormatException(error);
      }
   }
}
