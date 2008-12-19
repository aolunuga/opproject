/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModule;
import onepoint.project.module.OpModuleChecker;
import onepoint.project.modules.backup.OpBackupManager;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.work.OpWorkRecord;
import onepoint.project.modules.work.OpWorkSlip;
import onepoint.project.util.OpBulkFetchIterator;
import onepoint.project.util.OpBulkFetchIterator.LongIdConverter;

public class OpProjectModule extends OpModule {

   private static final XLog logger = XLogFactory.getLogger(OpModule.class);

   public final static byte PORTFOLIO_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;
   public final static byte PROJECT_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.CONTRIBUTOR + OpPermission.OBSERVER;
   public final static byte TEMPLATE_ACCESS_LEVELS = OpPermission.ADMINISTRATOR + OpPermission.MANAGER + OpPermission.OBSERVER;

   /**
    * The name of project module.
    */
   public static final String MODULE_NAME = "project";

   private final static String OLD_ROOT_PORTFOLIO_NAME = "{$RootProjectPortfolioName}";

   /**
    * Returns the name of the start form for this module.
    *
    * @return a <code>String</code> representing the path to the start form.
    */
   public String getStartFormPath() {
      return "/modules/project/forms/projects.oxf.xml";
   }

   /**
    * @see onepoint.project.module.OpModule#start(onepoint.project.OpProjectSession)
    */
   @Override
   public void start(OpProjectSession session) {

      //Register system objects with backup manager (for backup backward compatibility, add the old names as well)
      OpBackupManager.addSystemObjectIDQuery(OLD_ROOT_PORTFOLIO_NAME, OpProjectNode.ROOT_PROJECT_PORTFOLIO_ID_QUERY);
      OpBackupManager.addSystemObjectIDQuery(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME, OpProjectNode.ROOT_PROJECT_PORTFOLIO_ID_QUERY);

      // Check if hard-wired portfolio object "Root Project Portfolio" exists (if not create it)
      OpBroker broker = session.newBroker();
      try {
         if (OpProjectAdministrationService.findRootPortfolio(broker) == null && !updateRootPortfolioName(broker)) {
            OpProjectAdministrationService.createRootPortfolio(session, broker);
         }
      }
      finally {
         broker.close();
      }
   }

   /**
    * Upgrades this module to version #5 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion5(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         updateRootPortfolioName(broker);
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Upgrades this module to version #11 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion11(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery allProjectsQuery = broker.newQuery("from OpProjectNode projectNode where projectNode.Type = :type");
         allProjectsQuery.setParameter("type", OpProjectNode.PROJECT);
         OpTransaction tx = broker.newTransaction();
         Iterator<OpProjectNode> projectsIt = broker.iterate(allProjectsQuery);
         while (projectsIt.hasNext()) {
            OpProjectNode project = projectsIt.next();
            project.setArchived(OpProjectNode.DEFAULT_ARCHIVED);
            project.setPriority(OpProjectNode.DEFAULT_PRIORITY);
            project.setProbability(OpProjectNode.DEFAULT_PROBABILITY);
            broker.updateObject(project);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Upgrades this module to version #21 (via reflection).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion21(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery("from OpProjectPlanVersion planVersion");
         Iterator it = broker.iterate(query);
         OpTransaction transaction = broker.newTransaction();
         while (it.hasNext()) {
            OpProjectPlanVersion planVersion = (OpProjectPlanVersion) it.next();
            if (planVersion.isBaseline() == null) {
               planVersion.setBaseline(Boolean.FALSE);
               broker.updateObject(planVersion);
            }
         }
         transaction.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }


   /**
    * Upgrades this module to version #25 (via reflection - must be public).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion25(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpUser administrator = session.administrator(broker);
         OpQuery allPlanVersions = broker.newQuery("from OpProjectPlanVersion projectVersion");
         OpTransaction tx = broker.newTransaction();
         Iterator<OpProjectPlanVersion> projectsIt = broker.iterate(allPlanVersions);
         while (projectsIt.hasNext()) {
            OpProjectPlanVersion planVersion = projectsIt.next();
            OpUser user;
            try {
               user = broker.getObject(OpUser.class, new Long(planVersion.getCreator()));
            }
            catch (NumberFormatException e) {
               user = null;
            }

            String displayName = (user != null) ? user.getDisplayName() : administrator.getDisplayName();
            planVersion.setCreator(displayName);
            broker.updateObject(planVersion);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }


   /**
    * Upgrades this module to version #30 (via reflection - must be public).
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   public void upgradeToVersion32(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      try {
         OpUser administrator = session.administrator(broker);
         OpQuery allProjectPlans = broker.newQuery("from OpProjectPlan project");
         OpTransaction tx = broker.newTransaction();
         Iterator<OpProjectPlan> projectsIt = broker.iterate(allProjectPlans);
         while (projectsIt.hasNext()) {
            OpProjectPlan plan = projectsIt.next();
            //it the plan has any versions, take the creator of the last version
            OpProjectPlanVersion version = plan.getLatestVersionOld();
            String displayName = (version != null) ? version.getCreator() : administrator.getDisplayName();
            plan.setCreator(displayName);

            //set the version number on the project plan
            int versions = OpProjectDataSetFactory.getPlanVersionsCount(broker, plan);
            if (plan.hasWorkingVersion()) {
               versions--;
            }
            plan.setVersionNumber(versions);
            broker.updateObject(plan);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }
   
   public void upgradeToVersion86(OpProjectSession session) {

      OpBroker broker = session.newBroker();
      try {
         int count = 0;
         
         // new data model: keep version, setup links,
         OpUser administrator = session.administrator(broker);
         
         OpTransaction tx = broker.newTransaction();
         
         OpQuery idQ = broker
         .newQuery("select asv.id from OpAssignmentVersion as asv where " +
               "asv.Assignment is null");
         Iterator<Long> assVIdIt = broker.iterate(idQ);
         List<Long> allIds = new ArrayList<Long>();
         while (assVIdIt.hasNext()) {
            allIds.add((Long) assVIdIt .next());
         }
         Iterator<Long> memIt = allIds.iterator();
         OpBulkFetchIterator<OpAssignmentVersion, Long> asvit = new OpBulkFetchIterator<OpAssignmentVersion, Long>(
               broker, memIt, broker.newQuery("select assV from "
               + "OpAssignmentVersion as assV "
               + "left join fetch assV.ActivityVersion as actV "
               + "left join fetch actV.Activity as act "
               + "where assV.id in (:ids)"),
         new LongIdConverter(), "ids");

         while (asvit.hasNext()) {
            OpAssignmentVersion asv = asvit.next();
            if (count % 1000 == 0) {
               logger.info("linking OpAssignmentVersion: " + count);
            }
            count++;
            if (asv.getActivityVersion() == null || asv.getActivityVersion().getActivity() == null) {
               continue;
            }
            Set<OpAssignment> ass = asv.getActivityVersion().getActivity().getAssignments();
            if (ass == null) {
               continue;
            }
            for (OpAssignment as : ass) {
               if (as.getResource().getId() == asv.getResource().getId()) {
                  as.addAssignmentVersion(asv);
                  break;
               }
            }
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #00:" + count);
         
         OpQuery allProjectPlans = broker.newQuery("from OpProjectPlan project");
         tx = broker.newTransaction();
         Iterator<OpProjectPlan> projectsIt = broker.iterate(allProjectPlans);
         count = 0;
         while (projectsIt.hasNext()) {
            OpProjectPlan plan = projectsIt.next();
            Timestamp checkinTime = plan.getModified();
                       
            // fix dates:
            plan.setStart(OpGanttValidator.normalizeDate(plan.getStart()));
            plan.setFinish(OpGanttValidator.normalizeDate(plan.getFinish()));
            
            plan.getProjectNode().setStart(OpGanttValidator.normalizeDate(plan.getProjectNode().getStart()));
            plan.getProjectNode().setFinish(OpGanttValidator.normalizeDate(plan.getProjectNode().getFinish()));
            
            if (plan.getLatestVersion() != null) {
               // already upgraded
               continue;
            }
            // update checkintime of plan versions:
            if (plan.getVersions() != null) {
               for (OpProjectPlanVersion v: plan.getVersions()) {
                  // wild guess, but the best we have...
                  v.setCheckInTime(v.getCreated());
               }
            }
            boolean explicitBaseline = (plan.getBaselineVersionOld() != null);
            // first: make a copy of the checked in version and
            // keep it in the latest and eventually base pointers
            
            // OPP-1093: fix version number ?!?
            plan.setVersionNumber(plan.getVersionNumber() >= 0 ? plan.getVersionNumber() : 0);

            // create checked in version:
            OpProjectPlanVersion pv = OpActivityVersionDataSetFactory
                  .getInstance().newProjectPlanVersion(session, broker, plan,
                        null, plan.getVersionNumber(), true);
            // TODO: find a better guess:
            pv.setCheckInTime(checkinTime);
            plan.setLatestVersion(pv);
            plan.setBaseVersion(explicitBaseline ? plan.getBaselineVersionOld() : pv);
            
            plan.setWorkingVersion(plan.getWorkingVersionOld());
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #01:" + count);
         
         // more new datamodel: assignmentversion will know their assignments
         tx = broker.newTransaction();
         OpQuery allAssignmentVersions = broker.newQuery("from OpAssignmentVersion as av order by av.Assignment.Activity.id");
         Iterator<OpAssignmentVersion> ait = broker.iterate(allAssignmentVersions);
         count = 0;
         while (ait.hasNext()) {
            OpAssignmentVersion av = ait.next();
            // find the assignment...
            Iterator<OpAssignment> actAssIt = av.getActivityVersion().getActivity().getAssignments().iterator();
            while (actAssIt.hasNext() && av.getAssignment() == null) {
               OpAssignment ass = actAssIt.next();
               if (ass.getResource().getId() == av.getResource().getId()) {
                  ass.addAssignmentVersion(av);
               }
            }
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #02:" + count);
         
         // new costs-sums in workslips:
         tx = broker.newTransaction();
         OpQuery allWorkSlips = broker.newQuery("from OpWorkSlip");
         Iterator<OpWorkSlip> wsIt = broker.iterate(allWorkSlips);
         count = 0;
         while (wsIt.hasNext()) {
            double ct0s = 0d;
            double ct1s = 0d;
            double ct2s = 0d;
            double ct3s = 0d;
            double ct4s = 0d;
            OpWorkSlip ws = wsIt.next();
            if (ws.getRecords() != null) {
               for(OpWorkRecord wr : ws.getRecords()) {
                  ct0s += wr.getTravelCosts();
                  ct1s += wr.getMaterialCosts();
                  ct2s += wr.getExternalCosts();
                  ct3s += wr.getMiscellaneousCosts();
               }
            }
            ws.setTotalActualOtherCosts0(ct0s);
            ws.setTotalActualOtherCosts1(ct1s);
            ws.setTotalActualOtherCosts2(ct2s);
            ws.setTotalActualOtherCosts3(ct3s);
            ws.setTotalActualOtherCosts4(ct4s);
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #03:" + count);
         
         // update program-pointers (1):
         tx = broker.newTransaction();
         OpQuery allImportedActivites = broker.newQuery("from OpActivity as a where a.MasterActivity is not null");
         Iterator<OpActivity> iait = broker.iterate(allImportedActivites);
         count = 0;
         while (iait.hasNext()) {
            OpActivity a = iait.next();
            OpActivity master = a.getMasterActivity();
            // find the project plan version number:
            Integer vni = new Integer(a.getProjectPlan().getVersionNumber());
            Timestamp vDate = a.getModified(); 
            Set<OpActivityVersion> avs = a.getVersions();

            OpActivityVersion srcVersion = findBestVersion(vni, null, avs);
            OpActivityVersion tgtVersion = findBestVersion(null, a.getProjectPlan().getLatestVersion().getCheckInTime(), a.getMasterActivity().getVersions());
            if (tgtVersion == null) {
               Integer vniM = new Integer(master.getProjectPlan().getVersionNumber());
               tgtVersion = findBestVersion(vniM, null, a.getMasterActivity().getVersions());
            }
            a.getMasterActivity().removeShallowCopy(a);
            if (a.getDeleted()) {
               continue;
            }

            if (srcVersion == null) {
               logger.error("upgradeToVersion86 - no checked in version found after upgrade (1): " + a.getProjectPlan().getProjectNode().getName() + "-" + a.locator());
               continue;
            }
            if (tgtVersion == null) {
               logger.error("upgradeToVersion86 - no target version found after upgrade (1): " + a.getProjectPlan().getProjectNode().getName() + "-" + a.locator());
               continue;
            }
            // remove the link from the OpActivities:
            tgtVersion.addShallowCopy(srcVersion);
            tgtVersion.setPublicActivity(true); // obviously ;-)
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #04:" + count);

         // update program-pointers (2):
         tx = broker.newTransaction();
         OpQuery allImportedActivityVersion = broker.newQuery("from OpActivityVersion as a where a.MasterActivity is not null");
         Iterator<OpActivityVersion> iavit = broker.iterate(allImportedActivityVersion);
         count = 0;
         while (iavit.hasNext()) {
            OpActivityVersion av = iavit.next();
            // find the project plan version number:
            Integer vni = new Integer(av.getProjectPlan().getVersionNumber());
            Timestamp vDate = av.getModified(); 

            OpActivityVersion tgtVersion = findBestVersion(null, av.getPlanVersion().getCheckInTime(), av.getMasterActivity().getVersions());
            if (tgtVersion == null) {
               Integer vniM = new Integer(av.getMasterActivity().getProjectPlan().getVersionNumber());
               tgtVersion = findBestVersion(vniM, null, av.getMasterActivity().getVersions());
            }
            av.getMasterActivity().removeShallowVersion(av);
            
            if (tgtVersion == null) {
               logger.error("upgradeToVersion86 - no target version found after upgrade (2): " + av.getProjectPlan().getProjectNode().getName() + "-" + av.locator());
               continue;
            }
            // remove the link from the OpActivities:
            tgtVersion.addShallowCopy(av);
            tgtVersion.setPublicActivity(true); // obviously ;-)
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #05:" + count);

         // update program-pointers (3):
         tx = broker.newTransaction();
         OpQuery allSubProjectActivities = broker.newQuery("from OpActivity as a where a.SubProject is not null");
         Iterator<OpActivity> sait = broker.iterate(allSubProjectActivities);
         count = 0;
         while (sait.hasNext()) {
            OpActivity a = sait.next();
            // find the project plan version number:
            OpProjectNode tp = a.getSubProject();
            tp.removeProgramActivity(a);

            Timestamp vDate = tp.getPlan().getLatestVersion() != null ? tp
                  .getPlan().getLatestVersion().getCheckInTime() : null; 

            if (vDate == null) {
               continue;
            }
            Set<OpActivityVersion> avs = a.getVersions();
            OpActivityVersion srcVersion = findBestVersion(null, vDate, avs);
            
            if (srcVersion == null) {
               logger.warn("upgradeToVersion86 - no checked in version found after upgrade (3): " + a.getProjectPlan().getProjectNode().getName() + "-" + a.locator());
               continue;
            }
            tp.addProgramActivityVersion(srcVersion);
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #06:" + count);

         tx = broker.newTransaction();
         OpQuery allExportedActivities = broker.newQuery("from OpActivityVersion as a where a.Attributes > :maxAttribute");
         allExportedActivities.setInteger("maxAttribute", OpGanttValidator.EXPORTED_TO_SUPERPROJECT);
         Iterator<OpActivityVersion> eait = broker.iterate(allExportedActivities);
         count = 0;
         while (eait.hasNext()) {
            OpActivityVersion a = eait.next();
            if ((a.getAttributes() & OpGanttValidator.EXPORTED_TO_SUPERPROJECT) == OpGanttValidator.EXPORTED_TO_SUPERPROJECT) {
               a.setPublicActivity(true);
            }
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #07:" + count);

         tx = broker.newTransaction();
         OpQuery allTemplates = broker.newQuery("from OpProjectNode as pn where pn.Type = :templateType");
         allTemplates.setByte("templateType", OpProjectNode.TEMPLATE);
         Iterator<OpProjectNode> tit = broker.iterate(allTemplates);
         count = 0;
         while (tit.hasNext()) {
            OpProjectNode pn = tit.next();
            pn.getPlan().setCalculationMode(OpProjectPlan.INDEPENDENT);
            count++;
         }
         tx.commit();
         logger.debug("upgradeToVersion86 - #08:" + count);
      }
      finally {
         broker.closeAndEvict();
      }
   }

   private OpActivityVersion findBestVersion(Integer vni, Timestamp vDate,
         Set<OpActivityVersion> avs) {
      OpActivityVersion bestVersion = null;
      if (avs != null) {
         Iterator<OpActivityVersion> avsit = avs.iterator();
         Timestamp nearest = null;
         while (avsit.hasNext()) {
            OpActivityVersion av = avsit.next();
            if (vni != null && av.getPlanVersion().getVersionNumber() == vni.intValue()) {
               bestVersion = av;
               break;
            }
            Timestamp versionCheckInTime = av.getPlanVersion().getCheckInTime();
            if ((vDate != null)
                  && (!versionCheckInTime.after(vDate))
                  && (nearest == null || nearest.before(versionCheckInTime))) {
               nearest = versionCheckInTime;
               bestVersion = av;
            }
         }
      }
      return bestVersion;
   }
   
   /**
    * Changes the name of the root project portfolio from the old resource naming - starting with {$
    * to the new naming with ${ - only if the old naming exists.
    *
    * @param broker a <code>OpBroker</code> used for persistence operations.
    * @return a <code>true</code> if the update was successfully done.
    */
   private boolean updateRootPortfolioName(OpBroker broker) {
      OpProjectNode oldRoot = OpProjectAdministrationService.findProjectNode(broker, OLD_ROOT_PORTFOLIO_NAME, OpProjectNode.PORTFOLIO);
      if (oldRoot != null) {
         oldRoot.setName(OpProjectNode.ROOT_PROJECT_PORTFOLIO_NAME);
         oldRoot.setDescription(OpProjectNode.ROOT_PROJECT_PORTFOLIO_DESCRIPTION);
         OpTransaction t = broker.newTransaction();
         broker.updateObject(oldRoot);
         t.commit();
         return true;
      }
      return false;
   }

   /**
    * @see onepoint.project.module.OpModule#getCheckerList()
    */
   @Override
   public List<OpModuleChecker> getCheckerList() {
      List<OpModuleChecker> checkers = new ArrayList<OpModuleChecker>();
      checkers.add(new OpProjectModuleChecker());
      return checkers;
   }

}
