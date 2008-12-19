package onepoint.project.modules.external_applications.MindMeister;

import java.io.IOException;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationInstance;
import onepoint.project.modules.external_applications.OpExternalApplicationUser;
import onepoint.project.modules.external_applications.OpExternalApplicationUserParameter;
import onepoint.project.modules.external_applications.MindMeister.generated.Rsp;
import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.project.modules.project.OpProjectAdministrationService;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project_planning.OpProjectPlanningError;
import onepoint.project.modules.project_planning.OpProjectPlanningException;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.modules.user.OpUser;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.XError;
import onepoint.service.XMessage;

public class OpMindMeisterService extends OpProjectPlanningService {

   private OpMindMeisterErrorMap ERROR_MAP = new OpMindMeisterErrorMap();
   
   public XMessage createApplicationToken(OpProjectSession session, XMessage request) {
      Map<String, Object> dialogContent = (Map<String, Object>) request.getArgument("dialogContent");
      XMessage reply = new XMessage();
      
      OpBroker broker = session.newBroker();
      OpTransaction tx = null;
      try {
         tx = broker.newTransaction();
         String frob = OpProjectValidator.getDialogString(dialogContent, OpLoginWizardFormProvider.MINDMEISTERFROB_ID);
         
         OpMindMeisterConnection con = OpMindMeisterApplication.newConnection(null);
         String token = con.getToken(frob);
         
         OpExternalApplication mmApp = OpMindMeisterApplication.findMindMeisterInstance(broker);
         long userID = session.getUserID();
         OpExternalApplicationUser link = OpExternalApplicationInstance.findExternalApplicationRelation(
               broker, mmApp, userID);
         OpExternalApplicationUserParameter mmTokenParameter = null;
         if (link != null) {
            mmTokenParameter = link.getParameter(OpMindMeisterApplication.TOKEN_PARAMETER);
         }
         else {
            link = new OpExternalApplicationUser();
            mmApp.addUser(link);
            OpUser u = session.user(broker);
            u.addExternalApplication(link);
            broker.makePersistent(link);
         }
         if (mmTokenParameter == null) {
            mmTokenParameter = new OpExternalApplicationUserParameter(OpMindMeisterApplication.TOKEN_PARAMETER, token);
            link.addParameter(mmTokenParameter);
            broker.makePersistent(mmTokenParameter);
         }
         
         reply.setArgument("token", token);
         tx.commit();
      } catch (OpExternalApplicationException e) {
         XError error = session.newError(ERROR_MAP, OpMindMeisterError.LOGIN_ERROR);
         reply.setError(error);
         return reply;
      }
      finally {
         if (tx != null) {
            tx.rollbackIfNecessary();
         }
         broker.closeAndEvict();
      }
      
      return reply;
   }

   public XMessage importMap(OpProjectSession session, XMessage request) {
      Map<String, Object> dialogContent = (Map<String, Object>) request.getArgument("dialogContent");

      String projectId = OpProjectValidator.getDialogString(dialogContent, OpImportPlanFormProvider.PROJECTID_ID);
      boolean editMode = OpProjectValidator.getDialogBoolean(dialogContent, OpImportPlanFormProvider.EDITMODE_ID).booleanValue();
      String token = OpProjectValidator.getDialogString(dialogContent, OpImportPlanFormProvider.MINDMEISTERTOKEN_ID);
      String mmMapId = OpProjectValidator.getDialogObject(dialogContent, OpImportPlanFormProvider.MINDMEISTERMAPID_ID).toString();  

      OpMindMeisterConnection con = OpMindMeisterApplication.newConnection(token);
      con.setToken(token);

      XMessage reply = new XMessage();
      OpBroker broker = session.newBroker();
      OpTransaction tx = null;
      XComponent dataSet = null;
      try {
         tx = broker.newTransaction();
         OpProjectNode project = (OpProjectNode) (broker.getObject(projectId));
         if (project.getType() != OpProjectNode.PROJECT) {
            reply.setError(session.newError(OpProjectPlanningService.PLANNING_ERROR_MAP, OpProjectPlanningError.INVALID_PROJECT_NODE_TYPE_FOR_IMPORT));
            return reply;
         }
         OpProjectPlan projectPlan = project.getPlan();

         if (OpProjectAdministrationService.hasWorkRecords(project, broker)) {
            throw new OpProjectPlanningException(session.newError(OpProjectPlanningService.PLANNING_ERROR_MAP, OpProjectPlanningError.IMPORT_ERROR_WORK_RECORDS_EXIST));
         }

         Rsp.Ideas ideas = null;
         try {
            ideas = con.getMap(mmMapId);
         } catch (OpExternalApplicationException e1) {
            reply.setError(session.newError(ERROR_MAP, OpMindMeisterError.GET_MAP_ERROR));
            return reply;
         }
         
         try {
            dataSet = OpMindMeisterIdeaConverter.importActivities(session, broker, ideas, projectPlan, session.getLocale(), con);
         }
         catch (OpActivityLoopException loopExc) {
            reply.setError(session.newError(OpProjectPlanningService.PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR));
            return reply;
         }
         catch (IOException e) {
            reply.setError(session.newError(OpProjectPlanningService.PLANNING_ERROR_MAP, OpProjectPlanningError.MSPROJECT_FILE_READ_ERROR));
            return reply;
         }
         // commit before other brokers come along...
         tx.commit();
      }
      finally {
         if (tx != null) {
            tx.rollbackIfNecessary();
         }
         broker.close();
      }
         
      //edit if !edit_mode
      if (!editMode) {
         XMessage editRequest = new XMessage();
         editRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         reply = internalEditActivities(session, editRequest);
         if (reply.getError() != null) {
            return reply;
         }
      }

      //save
      XMessage saveRequest = new XMessage();
      saveRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
      saveRequest.setArgument(OpProjectPlanningService.ACTIVITY_SET, dataSet);
      saveRequest.setArgument(OpProjectPlanningService.SOURCE_PLAN_VERSION_ID, null);
      reply = saveActivities(session, saveRequest);
      if (reply != null && reply.getError() != null) {
         return reply;
      }

      //check in if !edit_mode
      if (!editMode) {
         XMessage checkInRequest = new XMessage();
         checkInRequest.setArgument(OpProjectPlanningService.PROJECT_ID, projectId);
         checkInRequest.setArgument(OpProjectPlanningService.ACTIVITY_SET, dataSet);
         reply = internalCheckInActivities(session, checkInRequest);
         if (reply != null && reply.getError() != null) {
            return reply;
         }
      }
      return reply;
   }
}
