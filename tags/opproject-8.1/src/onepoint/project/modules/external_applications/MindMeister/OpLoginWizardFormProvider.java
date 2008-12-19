package onepoint.project.modules.external_applications.MindMeister;

import java.util.HashMap;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationInstance;
import onepoint.project.modules.external_applications.OpExternalApplicationUser;
import onepoint.project.modules.external_applications.OpExternalApplicationUserParameter;
import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.server.XSession;

public class OpLoginWizardFormProvider implements XFormProvider {

   public final static String PROJECTID_ID = "ProjectId";
   public final static String MINDMEISTERFROB_ID = "MindMeisterFrob";
   public final static String CALLINGFRAME_ID = "CallingFrame";
   public final static String SUCCESSMETHOD_ID = "SuccessMethod";
   public final static String FAILUREMETHOD_ID = "FailureMethod";
   public final static String CANCELMETHOD_ID = "CancelMethod";
   public final static String AUTHURL_ID = "AuthURL";
   public final static String CONNECTIONFAILED_ID = "ConnectionFailed";
   public final static String DIALOGMAP_ID = "DialogMap";
   public final static String ERRORLABEL_ID = "ErrorLabel";
   public final static String OKBUTTON_ID = "OkButton";
   public final static String CANCELBUTTON_ID = "CancelButton";

   private final static String PROJECT_ID_PARAM = "projectId";
   
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectSession s = (OpProjectSession) session;
      OpBroker broker = s.newBroker();
      
      Map<String, Object> dialogObjects = new HashMap<String, Object>();

      String frob = null;
      
      OpMindMeisterConnection con = OpMindMeisterApplication.newConnection(null);
      try {
         OpExternalApplication mmApp = OpMindMeisterApplication.findMindMeisterInstance(broker);
         long userID = s.getUserID();
         OpExternalApplicationUser link = OpExternalApplicationInstance.findExternalApplicationRelation(
               broker, mmApp, userID);
         OpExternalApplicationUserParameter mmTokenParameter = null;
         if (link != null) {
            mmTokenParameter = link.getParameter(OpMindMeisterApplication.TOKEN_PARAMETER);
            if (mmTokenParameter != null) {
               mmTokenParameter.getUser().removeParameter(mmTokenParameter);
               broker.deleteObject(mmTokenParameter);
            }
         }
         
         frob = con.getFrob();
         form.findComponent(MINDMEISTERFROB_ID).setStringValue(frob);

         // talk to MindMeister for validation of token:
         String authUrl = con.getAuthenticationUrl(frob);
         form.findComponent(AUTHURL_ID).setStringValue(authUrl);
         
      } catch (OpExternalApplicationException e) {
         OpProjectValidator.setError(form, CONNECTIONFAILED_ID);
      }
      finally {
         broker.closeAndEvict();
      }

      if (frob == null) {
         OpProjectValidator.setError(form, CONNECTIONFAILED_ID);
      }
      dialogObjects.put(MINDMEISTERFROB_ID, frob);
      dialogObjects.put(PROJECTID_ID, parameters.get(PROJECT_ID_PARAM));
      dialogObjects.put(CALLINGFRAME_ID, parameters.get(CALLINGFRAME_ID));
      dialogObjects.put(SUCCESSMETHOD_ID, parameters.get(SUCCESSMETHOD_ID));
      dialogObjects.put(FAILUREMETHOD_ID, parameters.get(FAILUREMETHOD_ID));
      dialogObjects.put(CANCELMETHOD_ID, parameters.get(CANCELMETHOD_ID));
      OpProjectValidator.populateDialogFieldsFromMap(form, form.findComponent(DIALOGMAP_ID), dialogObjects);
   }

}
