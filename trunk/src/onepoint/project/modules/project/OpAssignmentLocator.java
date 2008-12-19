/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.project;

import java.util.Iterator;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;

/**
 * @author dfreis
 *
 */
public class OpAssignmentLocator {

   private static final String SELECT_ASSIGNMENT_QUERY = "select ass from OpAssignment as ass where ass.Resource.id = ? and ass.Activity.id = ?";

   /**
    * @param session
    * @param broker
    * @param resource
    * @param activity
    * @return
    * @pre
    * @post
    */
   public static OpAssignment getAssignment(OpProjectSession session, OpBroker broker, OpResource resource, OpActivity activity) {
      OpQuery query = broker.newQuery(SELECT_ASSIGNMENT_QUERY);
      query.setLong(0, resource.getId());
      query.setLong(1, activity.getId());
      Iterator iter = broker.iterate(query);
      if (iter.hasNext()) {
         return (OpAssignment)iter.next();
      }
      return null;
   }

}
