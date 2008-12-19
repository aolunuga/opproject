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
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectIfc;
import onepoint.persistence.OpOrigObject;
import onepoint.persistence.OpPrototype;
import onepoint.persistence.OpSource;
import onepoint.persistence.OpSourceManager;
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

	public final static String MODULE_REGISTRY_FILE_NAME = "registry.oxr.xml";

	/**
	 * This class' logger.
	 */
	private static final XLog logger = XLogFactory.getLogger(OpModule.class);

	private static OpModuleRegistry moduleRegistry;
	private static OpModuleRegistryLoader opModuleRegistryLoader;

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
		load(MODULE_REGISTRY_FILE_NAME);
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
		Collection<OpSource> allSources = OpSourceManager.getAllSources();
		for (OpSource source : allSources) {
			OpProjectSession session = new OpProjectSession(source.getName());
			try {
				if (((OpHibernateSource)source).existsTable("op_object")) {
					upgradeToVersion81(session, source);
				}
				session.loadSettings(false);
				for (int i = dbVersion + 1; i <= latestVersion; i++) {
//					if (i == 81) {
//						if (((OpHibernateSource)source).existsTable("op_object")) {
//						upgradeToVersion81(session, source);
//						}
//					}
					for (OpModule module : moduleRegistry) {
						String methodName = "upgradeToVersion" + i;
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

	private static void upgradeToVersion81(OpProjectSession session, OpSource source) {
		//if (!fromFile) {
		//   OpProjectSession session = new OpProjectSession(source.getName());
		OpHibernateSource hibernateSource = (OpHibernateSource)source;
		Configuration configuration = hibernateSource.getConfiguration();
		SessionFactory sf = configuration.buildSessionFactory();
		Session s = sf.openSession();
		OpHibernateSource hsource = (OpHibernateSource)source;
		try {
			moveOpObjectToVersion81(s, hsource);
			movePermissionsToVersion81(s, hsource);
			moveLocksToVersion81(s, hsource);
			moveDynamicResourcesToVersion81(s, hsource);
		}
		finally {
			s.close();
			sf.close();
		}

		OpBroker broker = session.newBroker();
		try {
			((OpHibernateConnection)(broker.getConnection())).deleteOldOpObject();
		}
		finally {
			Configuration newConfiguration = hibernateSource.getNewConfiguration();
			if (newConfiguration != null) {
				hsource.setConfiguration(newConfiguration);
				hsource.setNewConfiguration(null);
			}
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
				t.commit();
			}
		}
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
}