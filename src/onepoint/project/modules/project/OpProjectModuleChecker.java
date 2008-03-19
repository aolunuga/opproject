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
         fixAssignmentsProject(session, projectId);
      }
   }


   /**
    * Makes sure that all the existing assignments have the same project plan as the activity
    *
    * @param session   a <code>OpProjectSession</code> used during the upgrade procedure.
    * @param projectId a <code>long</code> the id of a project
    */
   private void fixAssignmentsProject(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery assignmentsQuery = broker.newQuery("from OpAssignment assignment where assignment.ProjectPlan != assignment.Activity.ProjectPlan and assignment.ProjectPlan.ProjectNode.ID=?");
         assignmentsQuery.setLong(0, projectId);
         OpTransaction tx = broker.newTransaction();
         Iterator<OpAssignment> assignmentsIt = broker.iterate(assignmentsQuery);
         while (assignmentsIt.hasNext()) {
            OpAssignment assignment = assignmentsIt.next();
            assignment.setProjectPlan(assignment.getActivity().getProjectPlan());
            broker.updateObject(assignment);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }


   /**
    * Deletes all the "orphan" assignment versions (not liked to an activity version)
    *
    * @param session   a <code>OpProjectSession</code> used during the upgrade procedure.
    * @param projectId a <code>long</code> the id of a project
    */
   private void cleanUpAssignments(OpProjectSession session, long projectId) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery assignmentVersionQuery = broker.newQuery("from OpAssignmentVersion assignmentVersion where assignmentVersion.ActivityVersion is null and assignmentVersion.PlanVersion.ProjectPlan.ProjectNode.ID=?");
         assignmentVersionQuery.setLong(0, projectId);
         OpTransaction tx = broker.newTransaction();
         Iterator<OpAssignmentVersion> assignmentsIt = broker.iterate(assignmentVersionQuery);
         while (assignmentsIt.hasNext()) {
            OpAssignmentVersion assignmentVersion = assignmentsIt.next();
            broker.deleteObject(assignmentVersion);
         }
         tx.commit();
      }
      finally {
         broker.closeAndEvict();
      }
   }

   /**
    * Returns a list of ids of project which have a certain type.
    *
    * @param session      a <code>OpProjectSession</code> the server session.
    * @param projectyType a <code>byte</code> the type of a project.
    * @return a <code>List(long)</code> which is a list of ids.
    */
   protected List<Long> getProjectsOfType(OpProjectSession session, byte projectyType) {
      List<Long> result = new ArrayList<Long>();
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(" select project.ID from OpProjectNode project where project.Type=? ");
         query.setByte(0, projectyType);
         Iterator it = broker.iterate(query);
         while (it.hasNext()) {
            result.add((Long) it.next());
         }
         return result;
      }
      finally {
         broker.closeAndEvict();         
      }
   }
}
