package onepoint.project.modules.external_applications;

import java.util.Iterator;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;

public abstract class OpExternalApplicationInstance {

   private OpExternalApplicationDescription description = null;
   
   public static final String INSTANCE_QUERY = "" +
         "select app from \n" +
         "  OpExternalApplication as app \n" +
         "where \n" +
         "  app.Kind = :kind \n" +
         "  and app.InstanceName = :instanceName \n";

   public static final String USER_APP_RELATION_QUERY = "" +
      		"select link from \n" +
      		"  OpExternalApplicationUser as link \n" +
      		"where \n" +
      		"  link.User.id = :userId \n" +
      		"  and link.Application.id = :appId ";

   public void start(OpProjectSession session, OpExternalApplicationDescription description) {
      this.description = description;
   }

   public void check(OpProjectSession session) {
   }

   public void stop(OpProjectSession session) {
   }

   public OpExternalApplicationDescription getDescription() {
      return description;
   }

   public static OpExternalApplication getApplicationInstance(OpBroker broker, String kind, String instanceName) {
      OpQuery q = broker.newQuery(INSTANCE_QUERY);
      q.setString("kind", kind);
      q.setString("instanceName", instanceName);
      
      Iterator i = broker.iterate(q);
      if (!i.hasNext()) {
         return null;
      }
      return (OpExternalApplication) i.next();
   }

   public OpExternalApplication getApplicationInstance(OpBroker broker, String instanceName) {
      OpQuery q = broker.newQuery(INSTANCE_QUERY);
      q.setString("kind", getDescription().getApplicationType());
      q.setString("instanceName", instanceName);
      
      Iterator i = broker.iterate(q);
      if (!i.hasNext()) {
         return null;
      }
      return (OpExternalApplication) i.next();
   }
   
   public static OpExternalApplicationUser findExternalApplicationRelation(
         OpBroker broker, OpExternalApplication extApp, long userID) {
      // get any existing relation between app and current user:
      OpQuery lq = broker.newQuery(OpExternalApplicationInstance.USER_APP_RELATION_QUERY);
      lq.setLong("userId", userID);
      lq.setLong("appId", extApp.getId());
      Iterator i = broker.iterate(lq);
      OpExternalApplicationUser link = null;
      if (i.hasNext()) {
         link = (OpExternalApplicationUser) i.next();
      }
      return link;
   }

}
