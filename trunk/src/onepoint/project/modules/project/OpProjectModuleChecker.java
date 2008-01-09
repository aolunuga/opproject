/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;
import onepoint.project.module.OpModuleChecker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Check class for Project Module.
 *
 * @author mihai.costin
 * @author horia.chiorean
 */
public class OpProjectModuleChecker implements OpModuleChecker {

   /**
    * @see onepoint.project.module.OpModuleChecker#check(onepoint.project.OpProjectSession)
    */
   public void check(OpProjectSession session) {
      Iterator<Long> it = getProjectsOfType(session, OpProjectNode.PROJECT).iterator();
      while (it.hasNext()) {
         long projectId = it.next();
         cleanUpAssignments(session, projectId);
      }
   }

   /**
    * Deletes all the "orphan" assignment versions (not liked to an activity version)
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    * @param projectId a <code>long</code> the id of a project
    */
   private void cleanUpAssignments(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      OpQuery assignmentVersionQuery = broker.newQuery("from OpAssignmentVersion assignmentVersion where assignmentVersion.ActivityVersion is null and assignmentVersion.PlanVersion.ProjectPlan.ProjectNode.ID=?");
      assignmentVersionQuery.setLong(0, projectId);
      OpTransaction tx = broker.newTransaction();
      Iterator<OpAssignmentVersion> assignmentsIt = broker.iterate(assignmentVersionQuery);
      while (assignmentsIt.hasNext()) {
         OpAssignmentVersion assignmentVersion = assignmentsIt.next();
         broker.deleteObject(assignmentVersion);
      }
      tx.commit();
      broker.closeAndEvict();
   }

   /**
    * Returns a list of ids of project which have a certain type.
    * @param session a <code>OpProjectSession</code> the server session.
    * @param projectyType a <code>byte</code> the type of a project.
    * @return a <code>List(long)</code> which is a list of ids.
    */
   protected List<Long> getProjectsOfType(OpProjectSession session, byte projectyType) {
      List<Long> result = new ArrayList<Long>();
      OpBroker broker = session.newBroker();
      OpQuery query = broker.newQuery(" select project.ID from OpProjectNode project where project.Type=? ");
      query.setByte(0, projectyType);
      Iterator it = broker.iterate(query);
      while (it.hasNext()) {
         result.add((Long) it.next());
      }
      broker.closeAndEvict();
      return result;
   }
}