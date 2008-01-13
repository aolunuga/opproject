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

import java.util.Iterator;

/**
 * Check class for Project Module.
 *
 * @author mihai.costin
 */
public class OpProjectModuleChecker implements OpModuleChecker {


   public void check(OpProjectSession session) {
      cleanUpAssignments(session);
      fixAssignmentsProject(session);
   }

   /**
    * Makes sure that all the existing assignments have the same project plan as the activity
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   private void fixAssignmentsProject(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery assignmentsQuery = broker.newQuery("from OpAssignment assignment where assignment.ProjectPlan != assignment.Activity.ProjectPlan");
      OpTransaction tx = broker.newTransaction();
      Iterator<OpAssignment> assignmentsIt = broker.iterate(assignmentsQuery);
      while (assignmentsIt.hasNext()) {
         OpAssignment assignment = assignmentsIt.next();
         assignment.setProjectPlan(assignment.getActivity().getProjectPlan());
         broker.updateObject(assignment);
      }
      tx.commit();
      broker.closeAndEvict();
   }

   /**
    * Deletes all the "orphan" assignment versions (not liked to an activity version)
    *
    * @param session a <code>OpProjectSession</code> used during the upgrade procedure.
    */
   private void cleanUpAssignments(OpProjectSession session) {
      OpBroker broker = session.newBroker();
      OpQuery assignmentVersionQuery = broker.newQuery("from OpAssignmentVersion assignmentVersion where assignmentVersion.ActivityVersion is null");
      OpTransaction tx = broker.newTransaction();
      Iterator<OpAssignmentVersion> assignmentsIt = broker.iterate(assignmentVersionQuery);
      while (assignmentsIt.hasNext()) {
         OpAssignmentVersion assignmentVersion = assignmentsIt.next();
         broker.deleteObject(assignmentVersion);
      }
      tx.commit();
      broker.closeAndEvict();
   }
}
