/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project.test;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.*;
import onepoint.persistence.hibernate.OpHibernateSource;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.modules.project.*;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.*;
import onepoint.project.util.OpEnvironmentManager;
import onepoint.project.util.OpSHA1;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocaleMapLoader;
import onepoint.resource.XResourceBroker;
import onepoint.util.XCalendar;
import onepoint.util.XEnvironment;

public class OpProjectTestData {

   private static final XLog logger = XLogFactory.getLogger(OpProjectTestData.class, true);

   private static void addTestPermissions(OpBroker broker, OpObject object, OpUser cs, OpUser tw, OpGroup everyone) {
      OpPermission permission = new OpPermission();
      permission.setAccessLevel(OpPermission.ADMINISTRATOR);
      permission.setSubject(cs);
      permission.setObject(object);
      broker.makePersistent(permission);
      permission = new OpPermission();
      permission.setAccessLevel(OpPermission.ADMINISTRATOR);
      permission.setSubject(tw);
      permission.setObject(object);
      broker.makePersistent(permission);
      permission = new OpPermission();
      permission.setAccessLevel(OpPermission.OBSERVER);
      permission.setSubject(everyone);
      permission.setObject(object);
      broker.makePersistent(permission);
   }

   public static void main(String[] arguments) {

      // Parse command-line paramters
      String project_home = null;
      if ((arguments.length == 2) && (arguments[0].equals("-onepoint_home"))) {
         project_home = arguments[1];
      }
      else {
         project_home = OpEnvironmentManager.getEnvironmentVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME);
         if (project_home == null) {
            logger.fatal("USAGE: OpProjectTestData -onepoint_home <onepoint-home-dir>");
            System.exit(1);
         }
      }

      // TODO: Add code for setting start/end of assignments in XActivitySetFactory
      // (If collection-assignments stay in the system -- not sure yet)
      // TODO: Add colors to status tool and rename to "Termine"; add Milestone-Trend-Analysis; Milestone-Table
      // (Attention: Trend-Analysis and table should show dates regarding to a base-time-line [control points])
      // (Therefore, the number is fixed and is not necessarily bound to the number of delays that occured)
      // *** Hardcoded number of control points as dates: We need the milestone dates at these control dates
      // ==> We probably have to take them from time-log
      // TODO: Maybe add package project_exchange package for import/export functionality
      // (Might be easier to specifiy test data in XML than to hard-code in Java program)
      // (Also: Solves our problem w/Umlauts)
      // TODO: Time-wise simply make current test data a little bit more complex (WBS)

      // Load prototypes
      logger.info("Loading prototypes...");

//      XLog.setLevel(Level.INFO);

      // Setup environment
      XEnvironment.setVariable(onepoint.project.configuration.OpConfiguration.ONEPOINT_HOME, project_home);
      String modules_path = "onepoint/project/modules/";
      String commercial_modules_path = "onepoint/project/team/modules/";

      XResourceBroker.setResourcePath("onepoint/project");
      XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap("/locales.olm.xml");
      XLocaleManager.setLocaleMap(locale_map);

      OpPrototypeLoader prototype_loader = new OpPrototypeLoader();
      OpPrototype subject_prototype = prototype_loader.loadPrototype(modules_path + "user/subject.opt.xml");
      if (subject_prototype == null) {
         logger.error("ERROR: Could not load prototype for class OpSubject");
         System.exit(1);
      }
      OpPrototype user_prototype = prototype_loader.loadPrototype(modules_path + "user/user.opt.xml");
      OpPrototype contact_prototype = prototype_loader.loadPrototype(modules_path + "user/contact.opt.xml");
      OpPrototype group_prototype = prototype_loader.loadPrototype(modules_path + "user/group.opt.xml");
      OpPrototype user_assignment_prototype = prototype_loader.loadPrototype(modules_path
           + "user/user_assignment.opt.xml");
      OpPrototype group_assignment_prototype = prototype_loader.loadPrototype(modules_path
           + "user/group_assignment.opt.xml");
      OpPrototype lock_prototype = prototype_loader.loadPrototype(modules_path + "user/lock.opt.xml");
      OpPrototype preference_prototype = prototype_loader.loadPrototype(modules_path + "user/preference.opt.xml");
      OpPrototype permission_prototype = prototype_loader.loadPrototype(modules_path + "user/permission.opt.xml");
      OpPrototype resource_prototype = prototype_loader.loadPrototype(modules_path + "resource/resource.opt.xml");
      OpPrototype content_prototype = prototype_loader.loadPrototype(modules_path + "documents/content.opt.xml");
      OpPrototype document_prototype = prototype_loader.loadPrototype(modules_path + "documents/document.opt.xml");
      OpPrototype dynamic_resource_prototype = prototype_loader.loadPrototype(modules_path + "documents/dynamic_resource.opt.xml");
      OpPrototype pool_prototype = prototype_loader.loadPrototype(modules_path + "resource/resource_pool.opt.xml");
      OpPrototype project_node_prototype = prototype_loader.loadPrototype(modules_path + "project/project_node.opt.xml");
      OpPrototype project_node_assignment_prototype = prototype_loader.loadPrototype(modules_path + "project/project_node_assignment.opt.xml");
      OpPrototype goal_prototype = prototype_loader.loadPrototype(modules_path + "project/goal.opt.xml");
      OpPrototype to_do_prototype = prototype_loader.loadPrototype(modules_path + "project/to_do.opt.xml");
      OpPrototype activity_category_prototype = prototype_loader.loadPrototype(modules_path
           + "project/activity_category.opt.xml");
      OpPrototype project_plan_prototype = prototype_loader.loadPrototype(modules_path + "project/project_plan.opt.xml");
      OpPrototype activity_prototype = prototype_loader.loadPrototype(modules_path + "project/activity.opt.xml");
      OpPrototype attachment_prototype = prototype_loader.loadPrototype(modules_path + "project/attachment.opt.xml");
      OpPrototype assignment_prototype = prototype_loader.loadPrototype(modules_path + "project/assignment.opt.xml");
      OpPrototype work_period_prototype = prototype_loader.loadPrototype(modules_path + "project/work_period.opt.xml");
      OpPrototype dependency_prototype = prototype_loader.loadPrototype(modules_path + "project/dependency.opt.xml");
      OpPrototype activity_comment_prototype = prototype_loader.loadPrototype(modules_path + "project/activity_comment.opt.xml");
      OpPrototype project_plan_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/project_plan_version.opt.xml");
      OpPrototype activity_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/activity_version.opt.xml");
      OpPrototype attachment_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/attachment_version.opt.xml");
      OpPrototype assignment_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/assignment_version.opt.xml");
      OpPrototype work_period_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/work_period_version.opt.xml");
      OpPrototype dependency_version_prototype = prototype_loader.loadPrototype(modules_path
           + "project/dependency_version.opt.xml");
      OpPrototype work_record_prototype = prototype_loader.loadPrototype(modules_path + "work/work_record.opt.xml");
      OpPrototype work_slip_prototype = prototype_loader.loadPrototype(modules_path + "work/work_slip.opt.xml");
      OpPrototype setting_prototype = prototype_loader.loadPrototype(modules_path + "settings/setting.opt.xml");
      OpPrototype report_archive_prototype = prototype_loader.loadPrototype(commercial_modules_path + "report_archive/report.opt.xml");
      OpPrototype report_type_prototype = prototype_loader.loadPrototype(commercial_modules_path + "report_archive/report_type.opt.xml");
      OpPrototype schedule_prototype = prototype_loader.loadPrototype(modules_path + "schedule/schedule.opt.xml");

      // *** Add error-handling
      logger.info("Prototypes loaded.");

      // Register prototypes
      logger.info("Registering prototypes...");
      OpTypeManager.registerPrototype(subject_prototype);
      OpTypeManager.registerPrototype(user_prototype);
      OpTypeManager.registerPrototype(contact_prototype);
      OpTypeManager.registerPrototype(group_prototype);
      OpTypeManager.registerPrototype(user_assignment_prototype);
      OpTypeManager.registerPrototype(group_assignment_prototype);
      OpTypeManager.registerPrototype(lock_prototype);
      OpTypeManager.registerPrototype(preference_prototype);
      OpTypeManager.registerPrototype(permission_prototype);
      OpTypeManager.registerPrototype(content_prototype);
      OpTypeManager.registerPrototype(document_prototype);
      OpTypeManager.registerPrototype(dynamic_resource_prototype);
      OpTypeManager.registerPrototype(resource_prototype);
      OpTypeManager.registerPrototype(pool_prototype);
      OpTypeManager.registerPrototype(project_node_prototype);
      OpTypeManager.registerPrototype(project_node_assignment_prototype);
      OpTypeManager.registerPrototype(activity_category_prototype);
      OpTypeManager.registerPrototype(goal_prototype);
      OpTypeManager.registerPrototype(to_do_prototype);
      OpTypeManager.registerPrototype(project_plan_prototype);
      OpTypeManager.registerPrototype(activity_prototype);
      OpTypeManager.registerPrototype(attachment_prototype);
      OpTypeManager.registerPrototype(assignment_prototype);
      OpTypeManager.registerPrototype(work_period_prototype);
      OpTypeManager.registerPrototype(dependency_prototype);
      OpTypeManager.registerPrototype(activity_comment_prototype);
      OpTypeManager.registerPrototype(project_plan_version_prototype);
      OpTypeManager.registerPrototype(activity_version_prototype);
      OpTypeManager.registerPrototype(attachment_version_prototype);
      OpTypeManager.registerPrototype(assignment_version_prototype);
      OpTypeManager.registerPrototype(work_period_version_prototype);
      OpTypeManager.registerPrototype(dependency_version_prototype);
      OpTypeManager.registerPrototype(project_plan_version_prototype);
      OpTypeManager.registerPrototype(work_record_prototype);
      OpTypeManager.registerPrototype(work_slip_prototype);
      OpTypeManager.registerPrototype(setting_prototype);
      OpTypeManager.registerPrototype(report_archive_prototype);
      OpTypeManager.registerPrototype(report_type_prototype);
      OpTypeManager.registerPrototype(schedule_prototype);
      // *** Add error-handling
      logger.info("Prototypes registered.");

      // Lock type-manager (for resolving relationships)
      logger.info("Locking type-manager...");
      OpTypeManager.lock();
      logger.info("Type-manager locked.");

      // Load and register object source
      // OpSource mysql = XSQLSourceLoader.loadSource("mysql_auto.xml");
      OpConfigurationLoader configurationLoader = new OpConfigurationLoader();
      onepoint.project.configuration.OpConfiguration configuration = configurationLoader.loadConfiguration(project_home + "/"
           + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
      if (configuration == null) {
         logger.error("ERROR: Could not load configuration file " + project_home + "/"
              + OpConfigurationLoader.CONFIGURATION_FILE_NAME);
         System.exit(1);
      }

      OpHibernateSource mysql_auto = new OpHibernateSource(configuration.getDatabaseConfiguration().getDatabaseUrl(),
           configuration.getDatabaseConfiguration().getDatabaseDriver(), configuration.getDatabaseConfiguration().getDatabasePassword(),
           configuration.getDatabaseConfiguration().getDatabaseLogin(), configuration.getDatabaseConfiguration().getDatabaseType());

      logger.info("Registering SQL object source...");
      OpSourceManager.registerSource(mysql_auto);
      logger.info("SQL object source registered.");
      OpSourceManager.setDefaultSource(mysql_auto);

      // Open schema (changed for Hibernate; does not work for persistence.sql)
      mysql_auto.open();

      // Test dropping and creating schema
      logger.info("Creating schema in default-source...");
      OpBroker broker = OpPersistenceManager.newBroker();
      broker.dropSchema();
      broker.createSchema();
      logger.info("Schema successfully created.");

      // Create identification-related system objects (helpers supply their own transactions)
      OpUserService.createAdministrator(broker);
      OpGroup everyone = OpUserService.createEveryone(broker);
      broker.close();

      // Create session and new object broker (session caches administrator and everyone IDs)
      OpProjectSession session = new OpProjectSession();
      broker = session.newBroker();
      everyone = session.everyone(broker);

      // Create system objects
      OpResourcePool rootPool = OpResourceService.createRootPool(session, broker);
      OpProjectNode rootPortfolio = OpProjectAdministrationService.createRootPortfolio(session, broker);

      /* --- default settings are now hard-coded in application

      OpTransaction t = broker.newTransaction();

      OpSetting s = new OpSetting();
      s.setName(OpSettingsService.FIRST_WORKDAY);
      s.setValue("2");
      broker.makePersistent(s);
      s = new OpSetting();
      s.setName(OpSettingsService.LAST_WORKDAY);
      s.setValue("6");
      broker.makePersistent(s);
      s = new OpSetting();
      s.setName(OpSettingsService.DAY_WORKTIME);
      s.setValue("8");
      broker.makePersistent(s);
      s = new OpSetting();
      s.setName(OpSettingsService.WEEK_WORKTIME);
      s.setValue("40");
      broker.makePersistent(s);

      t.commit();
      */

      logger.info("Settings successfully inserted");

      OpTransaction t = broker.newTransaction();

      OpSHA1 sha1 = new OpSHA1();

      // Insert test users
      OpUser cs = new OpUser();
      cs.setName("cs");
      cs.setDisplayName("Cornelia Schulz");
      cs.setPassword(sha1.calculateHash(""));
      broker.makePersistent(cs);
      OpContact contact = new OpContact();
      contact.setUser(cs);
      contact.setFirstName("Cornelia");
      contact.setLastName("Schulz");
      contact.setEMail("");
      contact.setPhone("");
      contact.setMobile("");
      contact.setFax("");
      broker.makePersistent(contact);
      OpUserAssignment userAssignment = new OpUserAssignment();
      userAssignment.setUser(cs);
      userAssignment.setGroup(everyone);
      broker.makePersistent(userAssignment);

      OpUser jm = new OpUser();
      jm.setName("jm");
      jm.setDisplayName("Josef Muster");
      jm.setPassword(sha1.calculateHash(""));
      broker.makePersistent(jm);
      contact = new OpContact();
      contact.setUser(jm);
      contact.setFirstName("Josef");
      contact.setLastName("Muster");
      contact.setEMail("");
      contact.setPhone("");
      contact.setMobile("");
      contact.setFax("");
      broker.makePersistent(contact);
      userAssignment = new OpUserAssignment();
      userAssignment.setUser(jm);
      userAssignment.setGroup(everyone);
      broker.makePersistent(userAssignment);

      OpUser tw = new OpUser();
      tw.setName("tw");
      tw.setDisplayName("Thomas Winter");
      tw.setPassword(sha1.calculateHash(""));
      broker.makePersistent(tw);
      contact = new OpContact();
      contact.setUser(tw);
      contact.setFirstName("Thomas");
      contact.setLastName("Winter");
      contact.setEMail("");
      contact.setPhone("");
      contact.setMobile("");
      contact.setFax("");
      broker.makePersistent(contact);

      OpGroup tg = new OpGroup();
      tg.setName("Test Group");
      tg.setDisplayName("Test Group");
      tg.setDescription("This is a test group");
      broker.makePersistent(tg);
      userAssignment = new OpUserAssignment();
      userAssignment.setUser(tw);
      userAssignment.setGroup(everyone);
      broker.makePersistent(userAssignment);

      OpUser as = new OpUser();
      as.setName("as");
      as.setDisplayName("Alois Stadler");
      as.setPassword(sha1.calculateHash("as"));
      broker.makePersistent(as);
      contact = new OpContact();
      contact.setUser(as);
      contact.setFirstName("Alois");
      contact.setLastName("Stadler");
      contact.setEMail("");
      contact.setPhone("");
      contact.setMobile("");
      contact.setFax("");
      broker.makePersistent(contact);

      userAssignment = new OpUserAssignment();
      userAssignment.setUser(as);
      userAssignment.setGroup(everyone);
      broker.makePersistent(userAssignment);

      userAssignment = new OpUserAssignment();
      userAssignment.setUser(as);
      userAssignment.setGroup(tg);
      broker.makePersistent(userAssignment);

      t.commit();

      logger.info("Users successfully inserted.");

      // Insert test user preferences (locale names)
      t = broker.newTransaction();

      OpPreference preference = new OpPreference();
      preference.setName(OpPreference.LOCALE);
      preference.setValue("de");
      preference.setUser(cs);
      broker.makePersistent(preference);
      preference = new OpPreference();
      preference.setName(OpPreference.LOCALE);
      preference.setValue("en");
      preference.setUser(tw);
      broker.makePersistent(preference);

      t.commit();

      t = broker.newTransaction();

      // Insert test pool and resources
      OpResourcePool pool = new OpResourcePool();
      pool.setName("Consultants");
      pool.setHourlyRate(200.0);
      pool.setSuperPool(rootPool);
      broker.makePersistent(pool);
      addTestPermissions(broker, pool, cs, tw, everyone);

      OpResource cschulz = new OpResource();
      cschulz.setName("cschulz");
      cschulz.setDescription("Cornelia Schulz");
      cschulz.setUser(cs);
      cschulz.setHourlyRate(250.0);
      cschulz.setInheritPoolRate(false);
      cschulz.setPool(rootPool);
      broker.makePersistent(cschulz);
      addTestPermissions(broker, cschulz, cs, tw, everyone);

      OpResource jmuster = new OpResource();
      jmuster.setName("jmuster");
      jmuster.setDescription("Josef Muster");
      jmuster.setUser(jm);
      jmuster.setInheritPoolRate(true);
      jmuster.setHourlyRate(250.0);
      jmuster.setPool(pool);
      broker.makePersistent(jmuster);
      addTestPermissions(broker, jmuster, cs, tw, everyone);

      OpResource shausberg = new OpResource();
      shausberg.setName("shausberg");
      shausberg.setDescription("Sabine Hausberg");
      shausberg.setInheritPoolRate(true);
      shausberg.setHourlyRate(250.0);
      shausberg.setPool(pool);
      broker.makePersistent(shausberg);
      addTestPermissions(broker, shausberg, cs, tw, everyone);

      OpResource twinter = new OpResource();
      twinter.setName("twinter");
      twinter.setDescription("Thomas Winter");
      twinter.setUser(tw);
      twinter.setInheritPoolRate(true);
      twinter.setHourlyRate(250.0);
      twinter.setPool(pool);
      broker.makePersistent(twinter);
      addTestPermissions(broker, twinter, cs, tw, everyone);

      t.commit();
      logger.info("Resources successfully inserted.");

      t = broker.newTransaction();

      // Insert test projects
      XCalendar.getDefaultCalendar();
      OpProjectNode external_projects = new OpProjectNode();
      external_projects.setType(OpProjectNode.PORTFOLIO);
      external_projects.setSuperNode(rootPortfolio);
      external_projects.setName("Externe Projekte");
      external_projects.setDescription("Portfolio f\u00FCr externe Projekte");
      broker.makePersistent(external_projects);
      addTestPermissions(broker, external_projects, cs, tw, everyone);

      OpProjectNode htiSapProject = new OpProjectNode();
      htiSapProject.setType(OpProjectNode.PROJECT);
      htiSapProject.setSuperNode(rootPortfolio);
      htiSapProject.setName("HTI-SAP");
      htiSapProject.setDescription("SAP-Einf\u00FChrung bei HTI");
      htiSapProject.setStart(XCalendar.parseDate("1.8.2005"));
      htiSapProject.setFinish(XCalendar.parseDate("25.11.2005"));
      // hti_sap.setDuration(calendar.parseLocalizedDuration("17w"));
      broker.makePersistent(htiSapProject);
      addTestPermissions(broker, htiSapProject, cs, tw, everyone);

      OpProjectNode er_siebel = new OpProjectNode();
      er_siebel.setType(OpProjectNode.PROJECT);
      er_siebel.setSuperNode(rootPortfolio);
      er_siebel.setName("ER-Siebel");
      er_siebel.setDescription("Siebel-Implementation f\u00FCr Ehmann & Rudt");
      er_siebel.setStart(XCalendar.parseDate("1.8.2005"));
      er_siebel.setFinish(XCalendar.parseDate("5.8.2005"));
      // er_siebel.setDuration(calendar.parseLocalizedDuration("17w"));
      broker.makePersistent(er_siebel);
      addTestPermissions(broker, er_siebel, cs, tw, everyone);
      OpProjectPlan erSiebelPlan = new OpProjectPlan();
      erSiebelPlan.setProjectNode(er_siebel);
      erSiebelPlan.setStart(er_siebel.getStart());
      erSiebelPlan.setFinish(er_siebel.getFinish());
      broker.makePersistent(erSiebelPlan);

      OpProjectNode hti_sc = new OpProjectNode();
      hti_sc.setType(OpProjectNode.PROJECT);
      hti_sc.setSuperNode(rootPortfolio);
      hti_sc.setName("HTI-SC");
      hti_sc.setDescription("Sicherheitscheck bei HTI");
      hti_sc.setStart(XCalendar.parseDate("1.8.2005"));
      hti_sc.setFinish(XCalendar.parseDate("5.8.2005"));
      // hti_sc.setDuration(calendar.parseLocalizedDuration("17w"));
      broker.makePersistent(hti_sc);
      addTestPermissions(broker, hti_sc, cs, tw, everyone);
      OpProjectPlan htiScPlan = new OpProjectPlan();
      htiScPlan.setProjectNode(hti_sc);
      htiScPlan.setStart(hti_sc.getStart());
      htiScPlan.setFinish(hti_sc.getFinish());
      broker.makePersistent(htiScPlan);

      OpProjectNode obankProject = new OpProjectNode();
      obankProject.setType(OpProjectNode.PROJECT);
      obankProject.setSuperNode(external_projects);
      obankProject.setName("O-BANK");
      obankProject.setDescription("Online Banking Applikation");
      obankProject.setStart(XCalendar.parseDate("1.8.2005"));
      obankProject.setFinish(XCalendar.parseDate("5.8.2005"));
      // obank.setDuration(calendar.parseLocalizedDuration("17w"));
      broker.makePersistent(obankProject);
      addTestPermissions(broker, obankProject, cs, tw, everyone);

      t.commit();

      t = broker.newTransaction();

      // Assign resources to project HTI-SAP
      OpProjectNodeAssignment projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(htiSapProject);
      projectAssignment.setResource(cschulz);
      broker.makePersistent(projectAssignment);
      projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(htiSapProject);
      projectAssignment.setResource(jmuster);
      broker.makePersistent(projectAssignment);
      projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(htiSapProject);
      projectAssignment.setResource(shausberg);
      broker.makePersistent(projectAssignment);
      projectAssignment = new OpProjectNodeAssignment();
      projectAssignment.setProjectNode(htiSapProject);
      projectAssignment.setResource(twinter);
      broker.makePersistent(projectAssignment);

      t.commit();

      t = broker.newTransaction();

      // Insert test activities
      OpAssignment assignment = null;
      OpWorkPeriod workPeriod = null;
      OpDependency dependency = null;

      OpProjectPlan hti_sap = new OpProjectPlan();
      hti_sap.setProjectNode(htiSapProject);
      hti_sap.setStart(XCalendar.parseDate("1.8.2005"));
      hti_sap.setFinish(XCalendar.parseDate("25.11.2005"));
      broker.makePersistent(hti_sap);

      OpActivity activity1 = new OpActivity();
      activity1.setName("Gespr\u00e4che mit Gesch\u00e4ftsf\u00FChrung");
      activity1.setStart(XCalendar.parseDate("1.8.2005"));
      activity1.setFinish(XCalendar.parseDate("2.8.2005"));
      activity1.setDuration(2 * 8);
      activity1.setBaseEffort(2 * 8);
      activity1.setBasePersonnelCosts(activity1.getBaseEffort() * cschulz.getHourlyRate());
      activity1.setActualEffort(2 * 8);
      activity1.setActualPersonnelCosts(activity1.getActualEffort() * cschulz.getHourlyRate());
      activity1.setComplete((byte) 100);
      activity1.setSequence(0);
      broker.makePersistent(activity1);
      activity1.setProjectPlan(hti_sap);
      broker.updateObject(activity1);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity1);
      workPeriod.setStart(activity1.getStart());
      workPeriod.setBaseEffort(activity1.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(2 * 8);
      assignment.setComplete((byte) 100);
      assignment.setActualEffort(2 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(cschulz);
      assignment.setActivity(activity1);
      broker.updateObject(assignment);
      OpAttachment attachment = new OpAttachment();
      attachment.setLinked(true);
      attachment.setName("Technology Forum");
      attachment.setLocation("http://www.heise.de");
      attachment.setProjectPlan(hti_sap);
      attachment.setActivity(activity1);
      broker.makePersistent(attachment);

      OpActivity activity2 = new OpActivity();
      activity2.setName("Gespr\u00e4che mit Abteilungsleitern");
      activity2.setStart(XCalendar.parseDate("3.8.2005"));
      activity2.setFinish(XCalendar.parseDate("5.8.2005"));
      activity2.setDuration(3 * 8);
      activity2.setBaseEffort(3 * 8);
      activity2.setBasePersonnelCosts(activity2.getBaseEffort() * cschulz.getHourlyRate());
      activity2.setActualEffort(3 * 8);
      activity2.setActualPersonnelCosts(activity2.getActualEffort() * cschulz.getHourlyRate());
      activity2.setComplete((byte) 100);
      activity2.setSequence(1);
      broker.makePersistent(activity2);
      activity2.setProjectPlan(hti_sap);
      broker.updateObject(activity2);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity2);
      workPeriod.setStart(activity2.getStart());
      workPeriod.setBaseEffort(activity2.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(3 * 8);
      assignment.setComplete((byte) 100);
      assignment.setActualEffort(3 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(cschulz);
      assignment.setActivity(activity2);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity1);
      dependency.setSuccessorActivity(activity2);
      broker.updateObject(dependency);

      OpActivity activity3 = new OpActivity();
      activity3.setName("Anforderungen analysieren");
      activity3.setStart(XCalendar.parseDate("8.8.2005"));
      activity3.setFinish(XCalendar.parseDate("19.8.2005"));
      activity3.setDuration(10 * 8);
      activity3.setBaseEffort(10 * 8);
      activity3.setBasePersonnelCosts(activity3.getBaseEffort() * jmuster.getHourlyRate());
      activity3.setActualEffort(11 * 8);
      activity3.setActualPersonnelCosts(activity3.getActualEffort() * jmuster.getHourlyRate());
      activity3.setComplete((byte) 100);
      activity3.setSequence(2);
      broker.makePersistent(activity3);
      activity3.setProjectPlan(hti_sap);
      broker.updateObject(activity3);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity3);
      workPeriod.setStart(activity3.getStart());
      workPeriod.setBaseEffort(activity3.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(10 * 8);
      assignment.setComplete((byte) 100);
      assignment.setActualEffort(11 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(jmuster);
      assignment.setActivity(activity3);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity2);
      dependency.setSuccessorActivity(activity3);
      broker.updateObject(dependency);

      OpActivity activity4 = new OpActivity();
      activity4.setName("Abnahme Anforderungen");
      activity4.setStart(XCalendar.parseDate("22.8.2005"));
      activity4.setFinish(XCalendar.parseDate("26.8.2005"));
      activity4.setDuration(5 * 8);
      activity4.setBaseEffort(5 * 8);
      activity4.setBasePersonnelCosts(activity4.getBaseEffort() * cschulz.getHourlyRate());
      activity4.setActualEffort(5 * 8);
      activity4.setActualPersonnelCosts(activity4.getActualEffort() * cschulz.getHourlyRate());
      activity4.setComplete((byte) 100);
      activity4.setSequence(3);
      broker.makePersistent(activity4);
      activity4.setProjectPlan(hti_sap);
      broker.updateObject(activity4);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity4);
      workPeriod.setStart(activity4.getStart());
      workPeriod.setBaseEffort(activity4.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(5 * 8);
      assignment.setComplete((byte) 100);
      assignment.setActualEffort(5 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(cschulz);
      assignment.setActivity(activity4);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity3);
      dependency.setSuccessorActivity(activity4);
      broker.updateObject(dependency);

      OpActivity milestone1 = new OpActivity();
      milestone1.setName("Anforderungen abgenommen");
      milestone1.setType(OpActivity.MILESTONE);
      milestone1.setStart(XCalendar.parseDate("26.8.2005"));
      milestone1.setFinish(XCalendar.parseDate("26.8.2005"));
      milestone1.setSequence(4);
      milestone1.setComplete((byte) 100);
      broker.makePersistent(milestone1);
      milestone1.setProjectPlan(hti_sap);
      broker.updateObject(milestone1);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity4);
      dependency.setSuccessorActivity(milestone1);
      broker.updateObject(dependency);

      OpActivity activity5 = new OpActivity();
      activity5.setName("Implementation");
      activity5.setStart(XCalendar.parseDate("29.8.2005"));
      activity5.setFinish(XCalendar.parseDate("21.10.2005"));
      activity5.setDuration(40 * 8);
      activity5.setBaseEffort(40 * 8);
      activity5.setBasePersonnelCosts(activity5.getBaseEffort() * twinter.getHourlyRate());
      activity5.setActualEffort(50 * 8);
      activity5.setActualPersonnelCosts(activity5.getActualEffort() * twinter.getHourlyRate());
      activity5.setComplete((byte) 80);
      activity5.setSequence(5);
      broker.makePersistent(activity5);
      activity5.setProjectPlan(hti_sap);
      broker.updateObject(activity5);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity5);
      workPeriod.setStart(activity5.getStart());
      workPeriod.setBaseEffort(activity5.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(40 * 8);
      assignment.setComplete((byte) 80);
      assignment.setActualEffort(50 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(twinter);
      assignment.setActivity(activity5);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(milestone1);
      dependency.setSuccessorActivity(activity5);
      broker.updateObject(dependency);

      OpActivity activity6 = new OpActivity();
      activity6.setName("Test");
      activity6.setStart(XCalendar.parseDate("24.10.2005"));
      activity6.setFinish(XCalendar.parseDate("4.11.2005"));
      activity6.setDuration(10 * 8);
      activity6.setBaseEffort(10 * 8);
      activity6.setBasePersonnelCosts(activity6.getBaseEffort() * shausberg.getHourlyRate());
      activity6.setSequence(6);
      broker.makePersistent(activity6);
      activity6.setProjectPlan(hti_sap);
      broker.updateObject(activity6);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity6);
      workPeriod.setStart(activity6.getStart());
      workPeriod.setBaseEffort(activity6.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(10 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(shausberg);
      assignment.setActivity(activity6);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity5);
      dependency.setSuccessorActivity(activity6);
      broker.updateObject(dependency);

      OpActivity activity7 = new OpActivity();
      activity7.setName("Fehlerbereinigung");
      activity7.setStart(XCalendar.parseDate("7.11.2005"));
      activity7.setFinish(XCalendar.parseDate("18.11.2005"));
      activity7.setDuration(10 * 8);
      activity7.setBaseEffort(10 * 8);
      activity7.setBasePersonnelCosts(activity7.getBaseEffort() * twinter.getHourlyRate());
      activity7.setSequence(7);
      broker.makePersistent(activity7);
      activity7.setProjectPlan(hti_sap);
      broker.updateObject(activity7);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity7);
      workPeriod.setStart(activity7.getStart());
      workPeriod.setBaseEffort(activity7.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(10 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(twinter);
      assignment.setActivity(activity7);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity6);
      dependency.setSuccessorActivity(activity7);
      broker.updateObject(dependency);

      OpActivity activity8 = new OpActivity();
      activity8.setName("Dokumentation");
      activity8.setStart(XCalendar.parseDate("7.11.2005"));
      activity8.setFinish(XCalendar.parseDate("18.11.2005"));
      activity8.setDuration(10 * 8);
      activity8.setBaseEffort(10 * 8);
      activity8.setBasePersonnelCosts(activity8.getBaseEffort() * cschulz.getHourlyRate());
      activity8.setSequence(8);
      broker.makePersistent(activity8);
      activity8.setProjectPlan(hti_sap);
      broker.updateObject(activity8);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity8);
      workPeriod.setStart(activity8.getStart());
      workPeriod.setBaseEffort(activity8.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(10 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(cschulz);
      assignment.setActivity(activity8);
      broker.updateObject(assignment);

      OpActivity activity9 = new OpActivity();
      activity9.setName("Abnahme");
      activity9.setStart(XCalendar.parseDate("21.11.2005"));
      activity9.setFinish(XCalendar.parseDate("25.11.2005"));
      activity9.setDuration(5 * 8);
      activity9.setBaseEffort(5 * 8);
      activity9.setBasePersonnelCosts(activity9.getBaseEffort() * cschulz.getHourlyRate());
      activity9.setSequence(9);
      broker.makePersistent(activity9);
      activity9.setProjectPlan(hti_sap);
      broker.updateObject(activity9);
      workPeriod = new OpWorkPeriod();
      workPeriod.setProjectPlan(hti_sap);
      workPeriod.setActivity(activity9);
      workPeriod.setStart(activity9.getStart());
      workPeriod.setBaseEffort(activity9.getBaseEffort());
      broker.makePersistent(workPeriod);
      assignment = new OpAssignment();
      broker.makePersistent(assignment);
      assignment.setBaseEffort(5 * 8);
      assignment.setProjectPlan(hti_sap);
      assignment.setResource(cschulz);
      assignment.setActivity(activity9);
      broker.updateObject(assignment);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity7);
      dependency.setSuccessorActivity(activity9);
      broker.updateObject(dependency);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity8);
      dependency.setSuccessorActivity(activity9);
      broker.updateObject(dependency);

      OpActivity milestone2 = new OpActivity();
      milestone2.setName("Projekt abgenommen");
      milestone2.setType(OpActivity.MILESTONE);
      milestone2.setStart(XCalendar.parseDate("25.11.2005"));
      milestone2.setFinish(XCalendar.parseDate("25.11.2005"));
      milestone2.setSequence(10);
      broker.makePersistent(milestone2);
      milestone2.setProjectPlan(hti_sap);
      broker.updateObject(milestone2);
      dependency = new OpDependency();
      broker.makePersistent(dependency);
      dependency.setProjectPlan(hti_sap);
      dependency.setPredecessorActivity(activity9);
      dependency.setSuccessorActivity(milestone2);
      broker.updateObject(dependency);

      t.commit();
      t = broker.newTransaction();

      // Add WBS test data
      OpProjectPlan obank = new OpProjectPlan();
      obank.setProjectNode(obankProject);
      obank.setStart(XCalendar.parseDate("1.8.2005"));
      obank.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(obank);

      OpActivity activity = new OpActivity();
      activity.setSequence(0);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P1: Anforderungen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(1);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Unterst\u00FCtzung von Bankstandards");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(2);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Sicherheits- bestimmungen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(3);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Rollenmodell");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(4);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M1: Anforderungs- design abgeschlossen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(5);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P2: Forschung");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(6);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Erweiterte Verschl\u00FCsselungs- methoden");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(7);
      activity.setOutlineLevel((byte) 1);
      activity.setName("TAN vs. Token-basierte Systeme");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(8);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M2: Forschung abgeschlossen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(9);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P3: Systemdesign");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(10);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Design: User Interface");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(11);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Design: Middleware");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(12);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Design: Datenbank");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(13);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M3: Design fertig");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(14);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P4: Umsetzung");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(15);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Implementation: User Interface (inklusive Prototyp)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(16);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Implementation: Middleware");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(17);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Implementation: Datenbankschema");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(18);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Applikations- integration");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(19);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M4: Umsetzung abgeschlossen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(20);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P5: Projekttests");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(21);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Test: User Interface (teilautomatisiert)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(22);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Test: Middleware (JUnit)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(23);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Test: Datenbankschema (SQL-automatisiert)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(24);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Integrationstest (manuell)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(25);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M5: Projekttests erfolgreich beendet");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(26);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P6: Dokumentation");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(27);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Systemhandbuch (Administratoren)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(28);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Online Help (Endbenutzer)");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(29);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M6: Dokumentation fertig");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(30);
      activity.setType(OpActivity.COLLECTION);
      activity.setName("P7: Projektabschluss");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(31);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Projekt- dokumentation");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setSequence(32);
      activity.setOutlineLevel((byte) 1);
      activity.setName("Projekt-Feedback- Workshop");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      activity.setDuration(5 * 8);
      // activity.setBaseEffort(5 * 8);
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      activity = new OpActivity();
      activity.setType(OpActivity.MILESTONE);
      activity.setSequence(33);
      activity.setOutlineLevel((byte) 1);
      activity.setName("M7: Projekt abgeschlossen");
      activity.setStart(XCalendar.parseDate("1.8.2005"));
      activity.setFinish(XCalendar.parseDate("5.8.2005"));
      broker.makePersistent(activity);
      activity.setProjectPlan(obank);
      broker.updateObject(activity);

      t.commit();

      broker.close();

   }

}
