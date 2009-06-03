/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.module;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpOrigObject;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpRelationship;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
import onepoint.persistence.OpTransaction;
import onepoint.persistence.OpTypeManager;
import onepoint.persistence.hibernate.OpHibernateConnection;
import onepoint.persistence.hibernate.OpHibernateSchemaUpdater;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.persistence.hibernate.OpMappingsGenerator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.custom_attribute.OpCustomValuePage;
import onepoint.project.modules.custom_attribute.OpCustomizable;
import onepoint.project.modules.documents.OpDynamicResource;
import onepoint.project.modules.documents.OpDynamicResourceable;
import onepoint.project.modules.external_applications.OpExternalApplication;
import onepoint.project.modules.external_applications.OpExternalApplicationParameter;
import onepoint.project.modules.external_applications.OpExternalApplicationUser;
import onepoint.project.modules.external_applications.OpExternalApplicationUserParameter;
import onepoint.project.modules.project.OpActivityVersionWorkBreak;
import onepoint.project.modules.project.OpActivityWorkBreak;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpLockable;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;
import onepoint.project.util.OpEnvironmentManager;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;

public final class OpModuleManager {

	/**
	 * This class' logger.
	 */
	private static final XLog logger = XLogFactory.getLogger(OpModule.class);

	private static OpModuleRegistry moduleRegistry;
	private static OpModuleRegistryLoader opModuleRegistryLoader;
   private static String registryFileName = "registry.oxr.xml";


	public OpModuleManager() {
   }
	
	/**
	 * Sets the ModuleRegistryLoader to be used for loading the modules
	 *
	 * @param moduleLoader module loader
	 */
	public static void setModuleRegistryLoader(OpModuleRegistryLoader moduleLoader) {
		opModuleRegistryLoader = moduleLoader;
	}

	/**
	 * Read modules registry.
	 */
	public static void load() {
		load(getRegistryFileName());
	}

	/**
	 * Read modules registry.
	 *
	 * @param registryFileName name of the module registry file.
	 */
	public static void load(String registryFileName) {
		if (opModuleRegistryLoader == null) {
			opModuleRegistryLoader = new OpModuleRegistryLoader();
		}
		File opHome = new File(OpEnvironmentManager.getOnePointHome());
		moduleRegistry = opModuleRegistryLoader.loadModuleRegistry(opHome, registryFileName);
		if (moduleRegistry == null) {
			logger.error("The module registry wasn't initialized. No modules will be loaded.");
			return;
		}
		moduleRegistry.registerModules();
		OpBackupManager.getBackupManager().initializeBackupManager();
		//<FIXME author="Horia Chiorean" description="Is this needed ?">
		OpTypeManager.lock();
		//<FIXME>
	}

	/**
	 * Start all registered modules.
	 */
	public static void start() {
		Collection<OpSource> allSources = OpSourceManager.getAllSources();
		for (OpSource source : allSources) {
			OpModuleManager.start(source.getName());
		}
	}

	/**
	 * Start all registered modules for a given source.
	 *
	 * @param sourceName source name for which to initialize modules.
	 */
	public static void start(String sourceName) {
		// Invoke start callbacks
		OpProjectSession startupSession = new OpProjectSession(sourceName);
		for (OpModule module : moduleRegistry) {
			logger.info("Loading module " + module.getName());
			module.start(startupSession);
		}
		startupSession.close();
	}

	/**
	 * Stop all registered modules.
	 */
	public static void stop() {
		Collection<OpSource> allSources = OpSourceManager.getAllSources();
		for (OpSource source : allSources) {
			OpModuleManager.stop(source.getName());
		}
	}

	/**
	 * Stop all registered modules for a given source.
	 *
	 * @param sourceName source name
	 */
	public static void stop(String sourceName) {
		// *** Write module-registry?
		OpProjectSession shutdownSession = new OpProjectSession(sourceName);
		for (OpModule module : moduleRegistry) {
			module.stop(shutdownSession);
		}
		shutdownSession.close();
	}

	/**
	 * Calls the upgrade method for all the registered modules. This usually occurs when a db schema update takes place.
	 *
	 * @param dbVersion     database version (old one).
	 * @param latestVersion an <code>int</code> representing the latest version.
	 */
	public static void upgrade(int dbVersion, int latestVersion) {
      OpHibernateSource defaultSource = (OpHibernateSource)OpSourceManager.getDefaultSource();
      boolean existsOldOpObjectTable = defaultSource.existsTable("op_object");
      if (existsOldOpObjectTable) {
         defaultSource.enableFilters(false);
         try {
            OpProjectSession session = new OpProjectSession(defaultSource.getName());
            upgradeToNewDBVersion(session, defaultSource);
            deleteOldOpObject();
         }
         finally {
            defaultSource.enableFilters(true);
         }
      }

      Collection<OpSource> allSources = OpSourceManager.getAllSources();
		for (OpSource source : allSources) {
			OpProjectSession session = new OpProjectSession(source.getName());
			try {
				session.loadSettings(false);
				for (int i = dbVersion + 1; i <= latestVersion; i++) {
               String methodName = "upgradeToVersion" + i;
               try {
                  Method m = OpModuleManager.class.getMethod(methodName, OpProjectSession.class);
                  logger.info("Invoking " + methodName + " for module manager");
                  m.invoke(null, session);
               }
               catch (NoSuchMethodException e) {
                  logger.debug("No upgrade method " + methodName + " found for module manager");
               }
               catch (IllegalAccessException e) {
                  logger.debug("Cannot access upgrade method ", e);
               }
               catch (InvocationTargetException e) {
                  logger.error("Cannot invoke upgrade method " + methodName + " for module manager");
                  //allow exceptions thrown by upgrade methods to be handled by someone else as well
                  throw new RuntimeException(e.getCause());
               }
               for (OpModule module : moduleRegistry) {
                  try {
                     Method m = module.getClass().getMethod(methodName, OpProjectSession.class);
                     logger.info("Invoking " + methodName + " for module " + module.getName());
                     m.invoke(module, session);
						}
						catch (NoSuchMethodException e) {
							logger.debug("No upgrade method " + methodName + " found for module " + module.getName());
						}
						catch (IllegalAccessException e) {
							logger.debug("Cannot access upgrade method ", e);
						}
						catch (InvocationTargetException e) {
							logger.error("Cannot invoke upgrade method " + methodName + " for module " + module.getName(), e);
							//allow exceptions thrown by upgrade methods to be handled by someone else as well
							throw new RuntimeException(e.getCause());
						}
					}
				}
			}
			finally {
				session.close();
			}
		}
	}

   public static void upgradeToVersion88(OpProjectSession session) {

      OpHibernateSource hibernateSource = (OpHibernateSource)OpSourceManager.getDefaultSource();
      Configuration configuration = hibernateSource.getConfiguration();
      SessionFactory sf = configuration.buildSessionFactory();
      Session s = sf.openSession();
      try {
         moveCustomValuePagesToVersion81(s, hibernateSource);
      }
      finally {
         s.close();
         sf.close();
      }


      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      try {
         HashMap<OpPrototype, Set<OpPrototype>> ptmap = new HashMap<OpPrototype, Set<OpPrototype>>();
         Iterator<OpPrototype> prototypes = OpTypeManager.getPrototypes();
         while (prototypes.hasNext()) {
            OpPrototype ptype = prototypes.next();
            for (OpPrototype impType : ptype.getImplementedTypes()) {
               Set<OpPrototype> ptypeset = ptmap.get(impType);
               if (ptypeset == null) {
                  ptypeset = new HashSet<OpPrototype>();
                  ptmap.put(impType, ptypeset);
               }
               ptypeset.add(ptype);
            }
         }

         for (Map.Entry<OpPrototype, Set<OpPrototype>> entry : ptmap.entrySet()) {
            List<OpRelationship> relations = entry.getKey().getRelationships();
            for (OpRelationship relation : relations) {
               if (relation.getBackRelationship() != null) {
                  StringBuffer query = new StringBuffer();
                  query.append("delete from "+OpTypeManager.getPrototype(relation.getTypeName()).getInstanceClass().getName()+" as o where o."+relation.getBackRelationship().getName()+" is null");
                  broker.execute(broker.newQuery(query.toString()));

                  query = new StringBuffer();                  
                  query.append("delete from "+OpTypeManager.getPrototype(relation.getTypeName()).getInstanceClass().getName()+" as o where");
                  boolean first = true;
                  for (OpPrototype implementor : entry.getValue()) {
                     if (!first) {
                        query.append(" and");
                     }
                     first = false;
                     query.append(" not exists (from "+implementor.getInstanceClass().getName()+" as i where i.id = o."+relation.getBackRelationship().getName()+".id)");
                  }
                  broker.execute(broker.newQuery(query.toString()));
               }
            }
         }
         t.commit();
      }
      finally {
         broker.closeAndEvict();
      }
	}
	
	private static void upgradeToNewDBVersion(OpProjectSession session, OpSource source) {
		//if (!fromFile) 
		//   OpProjectSession session = new OpProjectSession(source.getName());
		OpHibernateSource hibernateSource = (OpHibernateSource)source;
		Configuration configuration = hibernateSource.getConfiguration();
		SessionFactory sf = configuration.buildSessionFactory();
		Session s = sf.openSession();
		OpHibernateSource hsource = (OpHibernateSource)source;
		try {
			moveOpObjectToVersion81(s, hsource);
         moveCustomValuePagesToVersion81(s, hsource);
			movePermissionsToVersion81(s, hsource);
			moveLocksToVersion81(s, hsource);
			moveDynamicResourcesToVersion81(s, hsource);
		}
		finally {
			s.close();
			sf.close();
		}

		Configuration newConfiguration = hibernateSource.getNewConfiguration();
      if (newConfiguration != null) {
         hsource.setConfiguration(newConfiguration);
         hsource.setNewConfiguration(null);
      }
	}

   private static void deleteOldOpObject() {
      OpProjectSession session = new OpProjectSession();
      OpBroker broker = session.newBroker();
		try {
			((OpHibernateConnection)(broker.getConnection())).deleteOldOpObject();
		}
		finally {
			broker.close();
		}
   }

	private static void moveOpObjectToVersion81(Session s, OpHibernateSource hsource) {
		long count = 0;
		long start = System.currentTimeMillis();
		Iterator pts = OpTypeManager.getPrototypes();
		while (pts.hasNext()) {
			OpPrototype pt = (OpPrototype) pts.next();
			if (pt.isInterface()) {
				continue;
			}
			if (pt.getInstanceClass() != OpOrigObject.class && !pt.isAbstract()) {
				Transaction t = s.beginTransaction();
				Query query = s.createQuery("from "+pt.getInstanceClass().getName()+" obj, "+OpOrigObject.class.getName()+" orig where obj.id = orig.id and obj.Created = null");
				logger.info("upgrading "+pt.getInstanceClass().getName()+" to Version 81");
				ScrollableResults iter = query.scroll();
				while (iter.next()) {
					OpObjectIfc obj = (OpObjectIfc) iter.get(0);
					OpOrigObject orig = (OpOrigObject) iter.get(1);
					// copy created and modified
					obj.setCreated(orig.getCreated());
					Timestamp modified = orig.getModified();
					if (modified == null) {
						modified = orig.getCreated();
					}
					obj.setModified(modified);
					// copy custom attributes
					if (obj instanceof OpCustomizable) {
						//	                     Set<OpCustomValuePage> pages = orig.getCustomValuePages();
						//	                     ((OpCustomizable)obj).setCustomValuePages(pages);
						OpCustomValuePage page = orig.getCustomValuePage();
						((OpCustomizable)obj).setCustomValuePage(page);
					}
					s.update(obj);

					count++;
					if (count % 1000 == 0) {
						s.flush();
						s.clear();
						logger.info("commiting 1000 (total: "+count+") updates of type "+obj.getClass().getSimpleName()+", lasted: "+(System.currentTimeMillis()-start));
						start = System.currentTimeMillis();
					}
				}
            if (pt.getInstanceClass() == OpExternalApplication.class || 
                  pt.getInstanceClass() == OpExternalApplicationParameter.class || 
                  pt.getInstanceClass() == OpExternalApplicationUser.class || 
                  pt.getInstanceClass() == OpExternalApplicationUserParameter.class ||
                  pt.getInstanceClass() == OpActivityWorkBreak.class ||
                  pt.getInstanceClass() == OpActivityVersionWorkBreak.class) {

               // move created and modified columns
                String objClassName = pt.getName();
                OpHibernateSource source = (OpHibernateSource)OpSourceManager.getDefaultSource();
                if (source.existsColumn(OpMappingsGenerator.generateTableName(objClassName), "created")) {
                   renameColumn(s, objClassName, "created", "op_created");
                }
                if (source.existsColumn(OpMappingsGenerator.generateTableName(objClassName), "modified")) {
                   renameColumn(s, objClassName, "modified", "op_modified");
                }
            }
				t.commit();
			}
		}
	}

   private static void renameColumn(Session s, String objClassName,
         String jdbcObjectKey, String jdbcOldKey) {
      s.createSQLQuery("update "+OpMappingsGenerator.generateTableName(objClassName)+" set "+jdbcOldKey+" = "+jdbcObjectKey).executeUpdate();
//                   String tableName = OpMappingsGenerator.generateTableName(objClassName);
//					    deleteKeyConstraints(s, hsource, jdbcObjectKey, tableName);
       s.createSQLQuery("alter table "+OpMappingsGenerator.generateTableName(objClassName)+" drop column "+jdbcObjectKey).executeUpdate();
   }	

	/**
	 * @param s
	 * @param hsource 
	 * @param source 
	 * @param count
	 * @pre
	 * @post
	 */
	private static void movePermissionsToVersion81(Session s, OpHibernateSource hsource) {
		// create mapping from op_object to op_id
		Class permissionClass = OpPermission.class;
		String jdbcObjectKey = "op_object";
		String tableName = OpMappingsGenerator.generateTableName(permissionClass.getSimpleName());
		if (!hsource.existsColumn(tableName, jdbcObjectKey)) { // no old tables -> do nothing
			return;
		}

		Transaction t = s.beginTransaction();
		
		HashMap<Long, Set<OpPermission>> permissionObjMap = createMapping(s, jdbcObjectKey, permissionClass);

		long count = 0;
		Query query = s.createQuery("from onepoint.project.modules.user.OpPermissionable");
		Iterator iter = query.iterate();
		while (iter.hasNext()) {
			OpPermissionable objWithPerms = (OpPermissionable) iter.next();
			Set<OpPermission> objPerms = objWithPerms.getPermissions();
			Set<OpPermission> permsToSet = permissionObjMap.get(objWithPerms.getId());
			// FIXME(dfreis 13.08.2008 15:03:29) note: calling objWithPerms.setPermissions(permsToSet) did not work
			if (permsToSet != null) {
				for (OpPermission perm : permsToSet) {
					if (perm.getObject() == null) {
						perm.setObject(objWithPerms);
						s.update(perm);
					}
				}
			}
			count++;
			if (count % 1000 == 0) {
				s.flush();
				s.clear();
				logger.info("commiting 1000 updates of permissions");
			}
		}
		// delete op_object column
	   deleteKeyConstraints(s, hsource, jdbcObjectKey, tableName);
		s.createSQLQuery("alter table "+OpMappingsGenerator.generateTableName(permissionClass.getSimpleName())+" drop column "+jdbcObjectKey).executeUpdate();
		t.commit();
		s.clear();
	}

   private static void deleteKeyConstraints(Session session,
         OpHibernateSource hSource, String columnName, String tableName) {
      OpHibernateSchemaUpdater updater = new OpHibernateSchemaUpdater(hSource.getDatabaseType());
	   Configuration configuration = hSource.getConfiguration();
	   Dialect dialect = Dialect.getDialect(configuration.getProperties());
	   Connection connection = session.connection();
      boolean autoCommit = false;
	   try {
	      autoCommit = connection.getAutoCommit();
	      connection.setAutoCommit(false);
	      DatabaseMetaData meta = connection.getMetaData();
	      List<String> script = updater.generateDropFKConstraints(tableName, columnName, meta);
	      OpHibernateConnection.execute(connection, script);
         connection.commit();
	   }
	   catch (HibernateException exc) {
         logger.error(exc.getMessage(), exc);
	   }
	   catch (SQLException exc) {
	      logger.error(exc.getMessage(), exc);
	   }
	   finally {
	      try {
            connection.setAutoCommit(autoCommit);
         } catch (SQLException exc) {
            logger.error(exc.getMessage(), exc);
         }
	   }
   }

	/**
	 * @param s
	 * @param source 
	 * @return
	 * @pre
	 * @post
	 */
	private static <PT extends OpObjectIfc> HashMap<Long, Set<PT>> createMapping(Session s, String jdbcObjectKey, Class<PT> prototypeClass) {
		//logger.info("select op_id, "+jdbcObjectKey+" from "+OpMappingsGenerator.generateTableName(prototypeClass.getSimpleName()));
		HashMap<Long, Set<PT>> permissionObjMap = new HashMap<Long, Set<PT>>();
		// check if table name still exists
		String tableName = OpMappingsGenerator.generateTableName(prototypeClass.getSimpleName());
		Query query = s.createSQLQuery("select op_id, "+jdbcObjectKey+" from "+tableName);
		Iterator iter = query.list().iterator();
		HashMap<Long, Long> permissionMap = new HashMap<Long, Long>();
		while (iter.hasNext()) {
			Object[] values = (Object[]) iter.next();
			if (values[0] != null && values[1] != null) {
				permissionMap.put(((Number)values[0]).longValue(), ((Number)values[1]).longValue());
			}
		}
		query = s.createQuery("from "+prototypeClass.getName());
		iter = query.iterate();
		while (iter.hasNext()) {
			PT perm = (PT)iter.next();
			Long objectId = permissionMap.get(perm.getId());

			Set<PT> value = permissionObjMap.get(objectId);
			if (value == null) {
				value = new HashSet<PT>();
				permissionObjMap.put(objectId,  value);
			}
			value.add(perm);
		}

		permissionMap.clear();
		permissionMap = null;
		return permissionObjMap;
	}

   /**
    * @param s
    * @param hsource 
    * @param source 
    * @param count
    * @pre
    * @post
    */
   private static void moveLocksToVersion81(Session s, OpHibernateSource hsource) {
      // create mapping from op_object to op_id
      Class lockClass = OpLock.class;
      String jdbcObjectKey = "op_target";
      String tableName = OpMappingsGenerator.generateTableName(lockClass.getSimpleName());
      if (!hsource.existsColumn(tableName, jdbcObjectKey)) { // no old tables -> do nothing
         return;
      }

      Transaction t = s.beginTransaction();
      HashMap<Long, Set<OpLock>> lockMap = createMapping(s, jdbcObjectKey, lockClass);

      long count = 0;
      Query query = s.createQuery("from "+OpLockable.class.getName());
      Iterator iter = query.iterate();
      while (iter.hasNext()) {
         OpLockable objWithLocks = (OpLockable) iter.next();
         Set<OpLock> objLocks = objWithLocks.getLocks();
         Set<OpLock> locksToSet = lockMap.get(objWithLocks.getId());
         if (locksToSet != null) {
            for (OpLock lock : locksToSet) {
               if (lock.getTarget() == null) {
                  lock.setTarget(objWithLocks);
                  s.update(lock);
               }
            }
         }
         count++;
         if (count % 1000 == 0) {
            s.flush();
            s.clear();
            logger.info("commiting 1000 updates of locks");
         }
      }
      // delete op_object column
      deleteKeyConstraints(s, hsource, jdbcObjectKey, tableName);
      s.createSQLQuery("alter table "+OpMappingsGenerator.generateTableName(lockClass.getSimpleName())+" drop column "+jdbcObjectKey).executeUpdate();
      t.commit();
      s.clear();
   }

   /**
    * @param s
    * @param hsource 
    * @param source 
    * @param count
    * @pre
    * @post
    */
   private static void moveCustomValuePagesToVersion81(Session s, OpHibernateSource hsource) {
      // create mapping from op_object to op_id
      Class customValuePageClass = OpCustomValuePage.class;
      String jdbcObjectKey = "op_object";
      String tableName = OpMappingsGenerator.generateTableName(customValuePageClass.getSimpleName());
      if (!hsource.existsColumn(tableName, jdbcObjectKey)) { // no old tables -> do nothing
         return;
      }

      Transaction t = s.beginTransaction();
      HashMap<Long, Set<OpCustomValuePage>> CustomValuePageMap = createMapping(s, jdbcObjectKey, customValuePageClass);

      long count = 0;
      Query query = s.createQuery("from "+OpCustomizable.class.getName() + " where op_customvaluepage is not null");
      Iterator iter = query.iterate();
      while (iter.hasNext()) {
         OpCustomizable objWithCustomValuePages = (OpCustomizable) iter.next();
         Set<OpCustomValuePage> objCustomValuePages = objWithCustomValuePages.getCustomValuePages();
         Set<OpCustomValuePage> CustomValuePagesToSet = CustomValuePageMap.get(objWithCustomValuePages.getId());
         if (CustomValuePagesToSet != null) {
            for (OpCustomValuePage customValuePage : CustomValuePagesToSet) {
               if (customValuePage.getObject() == null) {
                  customValuePage.setObject(objWithCustomValuePages);
                  s.update(customValuePage);
               }
            }
         }
         count++;
         if (count % 1000 == 0) {
            s.flush();
            s.clear();
            logger.info("commiting 1000 updates of CustomValuePages");
         }
      }
      // delete op_object column
      deleteKeyConstraints(s, hsource, jdbcObjectKey, tableName);
      s.createSQLQuery("alter table "+OpMappingsGenerator.generateTableName(customValuePageClass.getSimpleName())+" drop column "+jdbcObjectKey).executeUpdate();
      t.commit();
      s.clear();
   }

	/**
	 * @param s
	 * @param hsource 
	 * @param source 
	 * @param count
	 * @pre
	 * @post
	 */
	private static void moveDynamicResourcesToVersion81(Session s, OpHibernateSource hsource) {
		// create mapping from op_object to op_id
		Class lockClass = OpDynamicResource.class;
		String jdbcObjectKey = "op_object";
		String tableName = OpMappingsGenerator.generateTableName(lockClass.getSimpleName());
		if (!hsource.existsColumn(tableName, jdbcObjectKey)) { // no old tables -> do nothing
			return;
		}

		Transaction t = s.beginTransaction();
		HashMap<Long, Set<OpDynamicResource>> dynamicResourceMap = createMapping(s, jdbcObjectKey, lockClass);

		long count = 0;
		Query query = s.createQuery("from "+OpDynamicResourceable.class.getName());
		Iterator iter = query.iterate();
		while (iter.hasNext()) {
			OpDynamicResourceable objWithDynamicResources = (OpDynamicResourceable) iter.next();
			Set<OpDynamicResource> objDynamicResources = objWithDynamicResources.getDynamicResources();
			Set<OpDynamicResource> dynamicResourcesToSet = dynamicResourceMap.get(objWithDynamicResources.getId());
			if (dynamicResourcesToSet != null) {
				for (OpDynamicResource dynamicResource : dynamicResourcesToSet) {
					if (dynamicResource.getObject() == null)
						dynamicResource.setObject(objWithDynamicResources);
					s.update(dynamicResource);
				}
			}
			count++;
			if (count % 1000 == 0) {
				s.flush();
				s.clear();
				logger.info("commiting 1000 updates of dynamic resources");
			}
		}
		// delete op_object column
	   deleteKeyConstraints(s, hsource, jdbcObjectKey, tableName);
		s.createSQLQuery("alter table "+OpMappingsGenerator.generateTableName(lockClass.getSimpleName())+" drop column "+jdbcObjectKey).executeUpdate();
		t.commit();
		s.clear();
	}

	/**
	 * Checks the integrity of the modules and fixes the possible module errors.
	 */
	public static void checkModules() {
		long currentTime = System.currentTimeMillis();
		Collection<OpSource> allSources = OpSourceManager.getAllSources();
		for (OpSource source : allSources) {
			OpProjectSession session = new OpProjectSession(source.getName());
			try {
				session.loadSettings(false);
				for (OpModule module : moduleRegistry) {
					module.check(session);
					//just a hint, but profiling shows it helps
					System.gc();
				}
			} finally {
				session.close();
			}
		}
		logger.info("Total checking time: " + (System.currentTimeMillis() - currentTime) / 1000 + " sec");
	}

	public static OpModuleRegistry getModuleRegistry() {
		return moduleRegistry;
	}
	
   public static void setRegistryFileName(String fileName) {
      registryFileName = fileName;
   }
   
   public static String getRegistryFileName() {
      return registryFileName;
   }

}