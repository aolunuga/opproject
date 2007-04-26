/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.resource.test;

import onepoint.express.XComponent;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class contains helper methods for managing resource data
 *
 * @author lucian.furtos
 */
public class ResourceTestDataFactory extends TestDataFactory {

   private final static String SELECT_RESOURCE_ID_BY_NAME_QUERY = "select resource.ID from OpResource as resource where resource.Name = ?";
   private final static String SELECT_POOL_ID_BY_NAME_QUERY = "select pool.ID from OpResourcePool as pool where pool.Name = ?";

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public ResourceTestDataFactory(OpProjectSession session) {
      super(session);
   }

   /**
    * Get a resource by the name
    *
    * @param resourceName the resource name
    * @return an instance of <code>OpResource</code>
    */
   public OpResource getResourceByName(String resourceName) {
      String locator = getResourceId(resourceName);
      if (locator != null) {
         return getResourceById(locator);
      }

      return null;
   }

   /**
    * Get a resource by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpResource</code>
    */
   public OpResource getResourceById(String locator) {
      OpBroker broker = session.newBroker();

      OpResource resource = (OpResource) broker.getObject(locator);
      if (resource != null) {
         // just to inialize the collection
         if (resource.getAbsences() != null) {
            resource.getAbsences().size();
         }
         resource.getActivityAssignments().size();
         resource.getAssignmentVersions().size();
         resource.getDynamicResources().size();
         resource.getLocks().size();
         resource.getPermissions().size();
         resource.getProjectNodeAssignments().size();
         if (resource.getPool() != null) {
            resource.getPool().getName();
         }
      }
      broker.close();

      return resource;
   }

   /**
    * Get the DB identifier of a resource by name
    *
    * @param resourceName the resource name
    * @return the uniq identifier of an entity (the locator)
    */
   public String getResourceId(String resourceName) {
      OpBroker broker = session.newBroker();
      Long resourceId = null;

      OpQuery query = broker.newQuery(SELECT_RESOURCE_ID_BY_NAME_QUERY);
      query.setString(0, resourceName);
      Iterator resourceIt = broker.iterate(query);
      if (resourceIt.hasNext()) {
         resourceId = (Long) resourceIt.next();
      }
      broker.close();

      if (resourceId != null) {
         return OpLocator.locatorString(OpResource.RESOURCE, Long.parseLong(resourceId.toString()));
      }

      return null;
   }

   /**
    * Get all the resourceNames
    *
    * @return a <code>List</code> of <code>OpResource</code>
    */
   public List getAllResources() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpResource");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   public XMessage createResourceMsg(String name, String description, double available, double hourlyrate, double externalrate, boolean inheritrate, String poolid) {
      return createResourceMsg(name, description, available, hourlyrate, externalrate, inheritrate, poolid, null);
   }

   /**
    * A request to create a resource.
    *
    * @param name
    * @param description
    * @param available
    * @param hourlyrate
    * @param inheritrate
    * @param poolid
    * @param projects
    * @return
    */
   public XMessage createResourceMsg(String name, String description, double available, double hourlyrate, double externalrate, boolean inheritrate, String poolid, ArrayList projects) {
      HashMap args = new HashMap();
      args.put(OpResource.NAME, name);
      args.put(OpResource.DESCRIPTION, description);
      args.put(OpResource.AVAILABLE, new Double(available));
      args.put(OpResource.HOURLY_RATE, new Double(hourlyrate));
      args.put(OpResource.EXTERNAL_RATE, new Double(externalrate));
      args.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(inheritrate));
      args.put("PoolID", poolid);
      args.put(OpResourceService.PROJECTS, projects);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
      args.put(OpResourceService.HOURLY_RATES_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_DATA, args);
      return request;
   }

   public XMessage updateResourceMsg(String id, String name, String description, double available, double hourlyrate, double externalrate, boolean inheritrate, String userid, ArrayList projects, XComponent hourlyRatesPeriodDataSet) {
      HashMap args = new HashMap();
      args.put(OpResource.NAME, name);
      args.put(OpResource.DESCRIPTION, description);
      args.put(OpResource.AVAILABLE, new Double(available));
      args.put(OpResource.HOURLY_RATE, new Double(hourlyrate));
      args.put(OpResource.EXTERNAL_RATE, new Double(externalrate));
      args.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(inheritrate));
      args.put("UserID", userid);
      args.put(OpResourceService.PROJECTS, projects);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
      if(hourlyRatesPeriodDataSet != null){
         args.put(OpResourceService.HOURLY_RATES_SET, hourlyRatesPeriodDataSet);
      }
      else{
         args.put(OpResourceService.HOURLY_RATES_SET, new XComponent(XComponent.DATA_SET));
      }

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_ID, id);
      request.setArgument(OpResourceService.RESOURCE_DATA, args);
      return request;
   }

   /**
    * Get a resource pool by the name
    *
    * @param poolName the resource pool name
    * @return an instance of <code>OpResourcePool</code>
    */
   public OpResourcePool getResourcePoolByName(String poolName) {
      String locator = getResourcePoolId(poolName);
      if (locator != null) {
         return getResourcePoolById(locator);
      }

      return null;
   }

   /**
    * Get a resource pool by the locator
    *
    * @param locator the uniq identifier (locator) of an entity
    * @return an instance of <code>OpResourcePool</code>
    */
   public OpResourcePool getResourcePoolById(String locator) {
      OpBroker broker = session.newBroker();

      OpResourcePool pool = (OpResourcePool) broker.getObject(locator);
      if (pool != null) {
         // just to inialize the collection
         pool.getDynamicResources().size();
         pool.getLocks().size();
         pool.getPermissions().size();
         pool.getResources().size();
         pool.getSubPools().size();
         if (pool.getSuperPool() != null) {
            pool.getSuperPool().getName();
         }
      }
      broker.close();

      return pool;
   }

   /**
    * Get the DB identifier of a resource pool by name
    *
    * @param poolName the resource pool name
    * @return the uniq identifier of an entity (the locator)
    */
   public String getResourcePoolId(String poolName) {
      OpBroker broker = session.newBroker();
      Long poolId = null;

      OpQuery query = broker.newQuery(SELECT_POOL_ID_BY_NAME_QUERY);
      query.setString(0, poolName);
      Iterator poolIt = broker.iterate(query);
      if (poolIt.hasNext()) {
         poolId = (Long) poolIt.next();
      }
      broker.close();

      if (poolId != null) {
         return OpLocator.locatorString(OpResourcePool.RESOURCE_POOL, Long.parseLong(poolId.toString()));
      }

      return null;
   }

   /**
    * Get all the resource pools
    *
    * @return a <code>List</code> of <code>OpResourcePool</code>
    */
   public List getAllResourcePools() {
      OpBroker broker = session.newBroker();

      OpQuery query = broker.newQuery("from OpResourcePool");
      List result = broker.list(query);
      broker.close();

      return result;
   }

   /**
    * Create an insert pool message request
    *
    * @param name
    * @param description
    * @param hourlyrate
    * @param superid
    * @return
    */
   public XMessage createPoolMsg(String name, String description, double hourlyrate, double externalrate, String superid) {
      HashMap args = new HashMap();
      args.put(OpResourcePool.NAME, name);
      args.put(OpResourcePool.DESCRIPTION, description);
      args.put(OpResourcePool.HOURLY_RATE, new Double(hourlyrate));
      args.put(OpResourcePool.EXTERNAL_RATE, new Double(externalrate));
      args.put("SuperPoolID", superid);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.POOL_DATA, args);
      return request;
   }

   /**
    * Create an update resource pool message
    *
    * @param id
    * @param name
    * @param description
    * @param hourlyrate
    * @return
    */
   public XMessage updatePoolMsg(String id, String name, String description, Double hourlyrate, Double externalrate) {
      HashMap args = new HashMap();
      args.put(OpResourcePool.NAME, name);
      args.put(OpResourcePool.DESCRIPTION, description);
      args.put(OpResourcePool.HOURLY_RATE, hourlyrate);
      args.put(OpResourcePool.EXTERNAL_RATE, externalrate);
      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.POOL_ID, id);
      request.setArgument(OpResourceService.POOL_DATA, args);
      return request;
   }

   /**
    * A request to create a resource.
    *
    * @param id
    * @param available
    * @param hourlyrate
    * @param inheritrate
    * @param poolid
    * @param projects
    * @return
    */
   public XMessage importUserMsg(String id, double available, double hourlyrate, boolean inheritrate, String poolid, ArrayList projects) {
      HashMap args = new HashMap();
      args.put("UserID", id);
      args.put(OpResource.AVAILABLE, new Double(available));
      args.put(OpResource.HOURLY_RATE, new Double(hourlyrate));
      args.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(inheritrate));
      args.put("PoolID", poolid);
      args.put(OpResourceService.PROJECTS, projects);
//      args.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_DATA, args);
      return request;
   }


}
