package onepoint.project.modules.external_applications.MindMeister;


import java.util.Iterator;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleManager;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationDescription;
import onepoint.project.modules.external_applications.OpExternalApplicationParameter;
import onepoint.project.modules.external_applications.OpExternalApplicationInstance;
import onepoint.project.modules.external_applications.OpExternalApplicationsModule;

public class OpMindMeisterApplication extends OpExternalApplicationInstance {

   
   public static final String APP_KIND = "MindMeister";
   public static final String DEFAULT_INSTANCE = "instance";
   
   public static final String URL_PARAMETER = "url";
   public static final String DESCRIPTION_PARAMETER = "description";
   public static final String API_KEY_PARAMETER = "api-key";
   public static final String REST_SERVICE_PARAMETER = "rest-service";
   public static final String AUTH_SERVICE_PARAMETER = "auth-service";
   public static final String ATTACHMENT_SERVICE_PARAMETER = "attachment-service";
   public static final String SECRET_PARAMETER = "shared-secret";
   
   public static final String TOKEN_PARAMETER = "token";

   public void start(OpProjectSession session, OpExternalApplicationDescription description) {
      super.start(session, description);
      // load or create MindMeister Instance (we have only one here...)
      OpBroker broker = session.newBroker();
      checkAndCreateInstance(broker);
   }

   private void checkAndCreateInstance(OpBroker broker) {
      OpTransaction tx = null;
      try {
         OpQuery q = broker.newQuery(INSTANCE_QUERY);
         q.setString("kind", getDescription().getApplicationType());
         q.setString("instanceName", DEFAULT_INSTANCE);
         
         Iterator i = broker.iterate(q);
         if (!i.hasNext()) {
            tx = broker.newTransaction();
            OpExternalApplication app = new OpExternalApplication();
            app.setKind(getDescription().getApplicationType());
            app.setInstanceName(DEFAULT_INSTANCE);
            app.setDescription(getDescription().getParameter(DESCRIPTION_PARAMETER));
            OpExternalApplicationParameter p1 = new OpExternalApplicationParameter(URL_PARAMETER, getDescription().getParameter(URL_PARAMETER));
            app.addParameter(p1);
            OpExternalApplicationParameter p2 = new OpExternalApplicationParameter(DESCRIPTION_PARAMETER, getDescription().getParameter(DESCRIPTION_PARAMETER));
            app.addParameter(p2);
            broker.makePersistent(app);
            broker.makePersistent(p1);
            broker.makePersistent(p2);
            tx.commit();
         }
      } finally {
         if (tx != null) {
            tx.rollbackIfNecessary();
         }
         broker.closeAndEvict();
      }
   }

   public void check(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      checkAndCreateInstance(broker);
   }

   public static OpExternalApplication findMindMeisterInstance(OpBroker broker) {
      OpExternalApplication mmApp = OpExternalApplicationInstance
            .getApplicationInstance(broker, APP_KIND, DEFAULT_INSTANCE);
      return mmApp;
   }

   public static OpMindMeisterConnection newConnection(String token) {
      OpExternalApplicationDescription mmDesc = ((OpExternalApplicationsModule) OpModuleManager.getModuleRegistry()
         .getModule(OpExternalApplicationsModule.MODULE_NAME))
         .getApplicationDescription(OpMindMeisterApplication.APP_KIND);

      return new OpMindMeisterConnection(
            mmDesc
                  .getParameter(OpMindMeisterApplication.REST_SERVICE_PARAMETER),
            mmDesc
                  .getParameter(OpMindMeisterApplication.ATTACHMENT_SERVICE_PARAMETER),
            mmDesc
                  .getParameter(OpMindMeisterApplication.AUTH_SERVICE_PARAMETER),
            mmDesc.getParameter(OpMindMeisterApplication.API_KEY_PARAMETER),
            mmDesc.getParameter(OpMindMeisterApplication.SECRET_PARAMETER),
            token);
   }
}

