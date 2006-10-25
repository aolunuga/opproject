/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider used for moving resources or pools
 *
 * @author ovidiu.lupas
 */
public class OpMoveResourceChooserFormProvider implements XFormProvider {

   /* form parameters */
   public final static String POOL_DATA_SET = "PoolDataSet";
   public final static String ENTITY_NODE_ID_FIELD = "EntityIdField";
   public final static String DESCRIPTOR_FIELD = "DescriptorField";

   /* map params */
   public final static String DESCRIPTOR = "descriptor";
   public final static String ENTITY_NODE_ID = "entity_id";

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      /*get needed params */
      String descriptor = (String) parameters.get(DESCRIPTOR);
      String entityId = (String) parameters.get(ENTITY_NODE_ID);

      OpBroker broker = session.newBroker();
      //load it
      OpObject obj = broker.getObject(entityId);
      if (obj == null) {
         return;
      }
      /*fill the entity node id locator*/
      form.findComponent(ENTITY_NODE_ID_FIELD).setStringValue(obj.locator());
      /*fill form's descriptor field */
      form.findComponent(DESCRIPTOR_FIELD).setStringValue(descriptor);

      /*the data set of pool which will be filled */
      XComponent dataSet = form.findComponent(POOL_DATA_SET);

      if (descriptor.equals(OpResourceDataSetFactory.RESOURCE_DESCRIPTOR)) {
         OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet);
         OpResource selectedResource = (OpResource) obj;
         OpResourcePool pool = selectedResource.getPool();

         OpQuery query = broker.newQuery("select count(subPool) from OpResourcePool pool inner join pool.SubPools subPool where pool.ID = ?");
         query.setLong(0, pool.getID());
         Integer counter = (Integer) broker.iterate(query).next();

         XComponent dataRow;
         OpLocator locator;
         /*filter the data set */
         for (int i = dataSet.getChildCount() - 1; i >= 0; i--) {
            dataRow = (XComponent) dataSet.getChild(i);
            locator = OpLocator.parseLocator(dataRow.getStringValue());
            String prototype = locator.getPrototype().getName();
            if (prototype.equals(OpResource.RESOURCE)) { //remove each resource
               dataSet.removeChild(i);
            }
            else { //pool
               if (pool.locator().equals(locator.toString()) && counter.intValue() == 0) {
                  dataSet.removeChild(i);
               }
            }
         }
      }
      else { //POOL_DESCRIPTOR
         OpResourcePool selectedPool = (OpResourcePool) obj;
         if (!selectedPool.getName().equals(OpResourcePool.ROOT_RESOURCE_POOL_NAME)) {
            OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet);
         }
         OpResourcePool superPool = selectedPool.getSuperPool();
         if (superPool != null) {
            OpQuery query = broker.newQuery("select count(subPool) from OpResourcePool pool inner join pool.SubPools subPool where pool.ID = ? and subPool.ID != ?");
            query.setLong(0, superPool.getID());
            query.setLong(1, selectedPool.getID());
            Integer counter = (Integer) broker.iterate(query).next();

            XComponent dataRow;
            OpLocator locator;
            /*filter the data set */
            for (int i = dataSet.getChildCount() - 1; i >= 0; i--) {
               dataRow = (XComponent) dataSet.getChild(i);
               locator = OpLocator.parseLocator(dataRow.getStringValue());
               String prototype = locator.getPrototype().getName();
               if (prototype.equals(OpResource.RESOURCE)) { //remove each resource
                  dataSet.removeChild(i);
               }
               else { //pool
                  if (selectedPool.locator().equals(locator.toString())) {
                     dataSet.removeDataRows(dataRow.getSubRows());
                     dataSet.removeChild(i);
                  }
                  else if (superPool.locator().equals(locator.toString()) && counter.intValue() == 0) {
                     dataSet.removeChild(i);
                  }
               }
            }
         }
      }

      broker.close();
   }
}
