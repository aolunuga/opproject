package onepoint.project.modules.external_applications.MindMeister;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationInstance;
import onepoint.project.modules.external_applications.OpExternalApplicationUser;
import onepoint.project.modules.external_applications.OpExternalApplicationUserParameter;
import onepoint.project.modules.external_applications.MindMeister.generated.Rsp;
import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.project.modules.user.OpUser;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.server.XSession;

public class OpImportPlanFormProvider implements XFormProvider {

   public final static String PROJECTID_ID = "ProjectId";
   public final static String EDITMODE_ID = "EditMode";
   public final static String MINDMEISTERTOKEN_ID = "MindMeisterToken";
   public final static String MINDMEISTERMAPID_ID = "MindMeisterMapId";
   public final static String MINDMEISTERERROR_ID = "MindMeisterError";
   public final static String DIALOGMAP_ID = "DialogMap";
   public final static String ERRORLABEL_ID = "ErrorLabel";
   public final static String MAPSROWMAP_ID = "MapsRowMap";
   public final static String MAPSDATASET_ID = "MapsDataSet";
   public final static String PROJECTSTABLE_ID = "ProjectsTable";
   public final static String OKBUTTON_ID = "OkButton";
   public final static String CANCELBUTTON_ID = "CancelButton";

   private final static String PROJECT_ID_PARAM = "ProjectId";
   private final static String EDIT_MODE_PARAM = "EditMode";
   private final static String MINDMEISTER_TOKEN_PARAM = "MindMeisterToken";
   private final static String MINDMEISTER_FROB_PARAM = "MindMeisterFrob";
   
   public void prepareForm(XSession session, XComponent form, HashMap parameters) {
      OpProjectSession s = (OpProjectSession) session;
      OpBroker broker = s.newBroker();
      
      Map<String, Object> dialogObjects = new HashMap<String, Object>();
      String frob = (String) parameters.get(MINDMEISTER_FROB_PARAM);
      String token = (String) parameters.get(MINDMEISTER_TOKEN_PARAM);;
      
      OpMindMeisterConnection con = OpMindMeisterApplication.newConnection(token);

      try {
         OpExternalApplication mmApp = OpMindMeisterApplication.findMindMeisterInstance(broker);
         long userID = s.getUserID();
         OpExternalApplicationUser link = OpExternalApplicationInstance.findExternalApplicationRelation(
               broker, mmApp, userID);
         OpExternalApplicationUserParameter mmTokenParameter = null;
         if (link != null) {
            mmTokenParameter = link.getParameter(OpMindMeisterApplication.TOKEN_PARAMETER);
         }
         try {
            if (frob != null) {
               token = con.getToken(frob);
               
               if (mmTokenParameter == null) {
                  mmTokenParameter = new OpExternalApplicationUserParameter(OpMindMeisterApplication.TOKEN_PARAMETER, token);
                  if (link == null) {
                     link = new OpExternalApplicationUser();
                     OpUser user = broker.getObject(OpUser.class, s.getUserID());
                     user.addExternalApplication(link);
                     mmApp.addUser(link);
                     broker.makePersistent(link);
                  }
                  link.addParameter(mmTokenParameter);
                  broker.makePersistent(mmTokenParameter);
               }
            }
            else {
               if (mmTokenParameter != null) {
                  con.checkToken(mmTokenParameter.getValue());
                  token = mmTokenParameter.getValue();
               }
            }
         } catch (OpExternalApplicationException e) {
            switch (e.getCode()) {
            case OpExternalApplicationException.APPLICATION_ERROR_EXCEPTION:
               if (mmTokenParameter != null) {
                  link.removeParameter(mmTokenParameter);
                  broker.deleteObject(mmTokenParameter);
                  mmTokenParameter = null;
               }
               break;
            default:
               OpProjectValidator.setError(form, MINDMEISTERERROR_ID);
            }
         }
      }
      finally {
         broker.closeAndEvict();
      }

      if (token != null) {
         con.setToken(token);
         // we have a valid login token:
         try {
            Rsp.Maps maps = con.getMaps();
            Iterator<Rsp.Maps.Map> mit = maps.getMap().iterator();
            XComponent mapsDataSet = form.findComponent(MAPSDATASET_ID);
            while (mit.hasNext()) {
               XComponent row = new XComponent(XComponent.DATA_ROW);
               Map<String, Object> rowObject = new HashMap<String, Object>();
               rowObject.put("map", mit.next());
               OpProjectValidator.populateDataRowFromMap(row, form.findComponent(MAPSROWMAP_ID), rowObject, false);
               mapsDataSet.addChild(row);
            }
         } catch (OpExternalApplicationException e) {
            OpProjectValidator.setError(form, MINDMEISTERERROR_ID);
         }
      }
      
      dialogObjects.put(MINDMEISTERTOKEN_ID, token);
      dialogObjects.put(PROJECTID_ID, parameters.get(PROJECT_ID_PARAM));
      dialogObjects.put(EDITMODE_ID, parameters.get(EDIT_MODE_PARAM));

      OpProjectValidator.populateDialogFieldsFromMap(form, form.findComponent(DIALOGMAP_ID), dialogObjects);
   }

}
