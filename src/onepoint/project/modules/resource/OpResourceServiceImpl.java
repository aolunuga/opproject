/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.resource;

import java.util.Iterator;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;
import onepoint.project.modules.work.OpWorkServiceImpl;

/**
 * @author dfreis
 *
 */
public class OpResourceServiceImpl implements OpService {

   /**
    * The name of this service.
    */
   public static final String SERVICE_NAME = "ResourceService";
   private static final XLog logger = XLogFactory.getServerLogger(OpResourceServiceImpl.class);

   String SELECT_RESOURCE_BY_NAME_QUERY = "select resource from OpResource as resource where resource.Name = ?";

   
   /* (non-Javadoc)
    * @see onepoint.project.OpService#getName()
    */
   public String getName() {
      return SERVICE_NAME;
   }

   /**
    * @param session
    * @param broker
    * @param resource
    * @return
    * @pre
    * @post
    */
   public OpResource getResourceByName(OpProjectSession session, OpBroker broker, String name) {
      OpQuery query = broker.newQuery(SELECT_RESOURCE_BY_NAME_QUERY);
      query.setString(0, name);
      Iterator iter = broker.iterate(query);
      if (iter.hasNext()) {
         return (OpResource) iter.next();
      }
      return null;
   }

}
