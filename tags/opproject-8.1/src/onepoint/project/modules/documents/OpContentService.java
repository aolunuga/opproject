/*
 * Copyright(c) Onepoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.documents;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectService;
import onepoint.project.OpProjectSession;
import onepoint.service.XMessage;
import onepoint.service.server.XServiceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class that handles content related operations.
 *
 * @author mihai.costin
 */
public class OpContentService extends OpProjectService {

   public final static String SERVICE_NAME = "ContentService";
   private final static String VIEWED_CONTENTS = "NewlyViewedContents";
   private final static String GET_CONTENT_IDS_WITH_ZERO_REF_COUNT = "select content.id from OpContent content where content.RefCount=0 and content.id in (:contentIds)";
   private final static String DELETE_CONTENT_WITH_ID = "delete from OpContent obj where obj.id in (:objectIds)";

   /**
    * Returns the instance of the content service which was registered with the service manager.
    *
    * @return a <code>OpContentService</code> if the service was registered with the
    *         service manager, <code>null</code> otherwise.
    */
   public static OpContentService getService() {
      return (OpContentService) XServiceManager.getService(SERVICE_NAME);
   }

   /**
    * Deletes all <code>OpContent</code> objects that have the reference count = 0 and which have their ids set on
    *    the request.
    *
    * @param session the <code>OpProjectSession</code> object.
    * @param request - the <code>XMessage</code> object representing the request.
    * @return a <code>XMessage</code> object representing the response.
    */
   public XMessage deleteZeroRefContents(OpProjectSession session, XMessage request) {
      List<String> viewedContentsList = (List<String>) request.getArgument(VIEWED_CONTENTS);
      if(viewedContentsList != null && !viewedContentsList.isEmpty()) {
         List<Long> contentIds = new ArrayList<Long>();
         for (String locator : viewedContentsList) {
            contentIds.add(OpLocator.parseLocator(locator).getID());
         }

         OpBroker broker = session.newBroker();
         deleteZeroRefContentsWithIds(broker, contentIds);
         broker.close();
      }
      return null;
   }

   /**
    * Deletes all <code>OpContent</code> objects, which ids are in the <code>List</code> passed as parameter and which
    * have a zero reference on them.
    *
    * @param broker    - the <code>OpBroker</code> object.
    * @param contentIds the <code>List<Long></code> containing the ids of the contents which are candidates for
    *                   deletion..
    */
   void deleteZeroRefContentsWithIds(OpBroker broker, List<Long> contentIds) {
      if (contentIds == null || contentIds.isEmpty()) {
         return;
      }
      OpQuery query = broker.newQuery(GET_CONTENT_IDS_WITH_ZERO_REF_COUNT);
      query.setCollection("contentIds", contentIds);
      List ids = broker.list(query);
      if (ids != null && !ids.isEmpty()) {
         OpQuery deleteQuery = broker.newQuery(DELETE_CONTENT_WITH_ID);
         deleteQuery.setCollection("objectIds", ids);
         broker.execute(deleteQuery);
      }
   }
}