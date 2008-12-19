/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.custom_attribute;

import java.util.LinkedList;
import java.util.List;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectIfc;
import onepoint.project.OpProjectSession;
import onepoint.project.OpService;

/**
 * @author dfreis
 *
 */
public class OpCustomAttributeServiceImpl implements OpService {

   /**
    * The name of this service.
    */
   public static final String SERVICE_NAME = "CustomAttributeService";

   /* (non-Javadoc)
    * @see onepoint.project.OpService#getName()
    */
   public String getName() {
      return SERVICE_NAME;
   }

   /**
    * @param session
    * @pre
    * @post
    */
   public void init(OpProjectSession session) {
   }
   /**
    * @param session
    * @param broker
    * @param name
    * @param string
    * @param string2
    * @return
    * @pre
    * @post
    */
   public <O extends OpObjectIfc> List<O> getObjects(OpProjectSession session, OpBroker broker, 
         OpCustomAttribute attribute, Object attributeValue) {
      return new LinkedList<O>();
   }

}
