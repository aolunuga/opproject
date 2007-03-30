/**
 * 
 */
package onepoint.project.modules.my_tasks;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

import com.lowagie.text.pdf.AsianFontMapper;

import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.persistence.hibernate.OpHibernateConnection;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.project.OpActivity;
import onepoint.project.modules.project.OpAssignment;
import onepoint.project.modules.project.OpAttachment;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.project.modules.project.OpProjectNode;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionSetFactory;
import onepoint.project.modules.user.OpUser;
import onepoint.project.modules.user.OpUserError;
import onepoint.project.modules.user.OpUserServiceImpl;
import onepoint.service.server.XServiceException;

/**
 * @author dfreis
 *
 */
public class OpMyTasksServiceImpl
{ 
  
  private static final XLog logger_ = XLogFactory.getLogger(OpMyTasksServiceImpl.class, true);
  private static final String MAX_ACTIVITY_SEQUENCE = "select max(activity.Sequence) from OpActivity activity";
  static final OpMyTasksErrorMap ERROR_MAP = new OpMyTasksErrorMap();

  // Activity types
  public final static byte TYPE_STANDARD = OpGanttValidator.STANDARD;
  public final static byte TYPE_MILESTONE = OpGanttValidator.MILESTONE;
  public final static byte TYPE_COLLECTION = OpGanttValidator.COLLECTION;
  public final static byte TYPE_TASK = OpGanttValidator.TASK;
  public final static byte TYPE_COLLECTION_TASK = OpGanttValidator.COLLECTION_TASK;
  public final static byte TYPE_SCHEDULED_TASK = OpGanttValidator.SCHEDULED_TASK;
  public final static byte TYPE_ADHOC_TASK = OpGanttValidator.ADHOC_TASK;

  private static final String ALL_ROOT_ACTIVITIES = 
    "select activity from OpActivity as activity"+
    " inner join activity.Assignments as assignment"+
    " where assignment.Resource.ID in (:resourceIds)"+
    " and activity.SuperActivity = null"+ // no parent
//    " and activity.ProjectPlan.ID :project"+ 
    " and activity.Deleted = false"+  
    " order by activity.Sequence";

  private static final String ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE = 
    "select activity from OpActivity as activity"+
    " inner join activity.Assignments as assignment"+
    " where assignment.Resource.ID in (:resourceIds)"+
    " and activity.SuperActivity = null"+ // no parent
//    " and activity.ProjectPlan.ID :project"+ 
    " and activity.Type in (:types)"+ // = 6 == ADHOC_TASK
    " and activity.Deleted = false"+  
    " order by activity.Sequence";

//  private static final Criteria ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE = 
//    new Criteria(OpActivity.class)
//  .createAlias("Assignments","assignments")
//  .add(Expression.gt("assignments.Resource.ID", new Long(83)))
//  .add(Expression.isNull("SuperActivity"))
//  .add(Expression.eq("Deleted", new Boolean(false)))
//  .add(Expression.in("Type", new Byte[] {0,1,2,3,4,5,6}))
//  .addOrder( Order.asc("Sequence"))
  
  public Iterator<OpActivity> getMyRootTasks(OpProjectSession session, OpBroker broker)
  throws XServiceException {
    // get myResourceIds
    OpUser user = session.user(broker);
    Set<OpResource> resources = user.getResources();
    // get myProjectIds
//    LinkedList<Long> project_ids = new LinkedList<Long>();
//    Iterator<OpResource> resource_iter = resources.iterator();
//    Set<OpProjectNode> project_nodes;
//    Iterator<OpProjectNode> project_iter;
//    while (resource_iter.hasNext()) {
//      project_nodes = resource_iter.next().getProjectNodeAssignments();
//      project_iter = project_nodes.iterator();
//      while (project_iter.hasNext()) {
//        project_ids.add(project_iter.next().getID());
//      }
//    }

    // construct query
    OpQuery query = broker.newQuery(ALL_ROOT_ACTIVITIES);
    query.setCollection("resourceIds", resources);
    
    // type save required...
    final Iterator iter = broker.iterate(query);
    return new Iterator<OpActivity>() {
      public boolean hasNext() {
        return iter.hasNext();
      }

      public OpActivity next() {
        return (OpActivity)iter.next();
      }

      public void remove() {
        iter.remove();
      }
    };
//    return(broker.iterate(query));
  }

  public Iterator<OpActivity> getMyRootTasks(OpProjectSession session, OpBroker broker, BitSet types)
  throws XServiceException {
    // get myResourceIds
    OpUser user = session.user(broker);
    Set<OpResource> resources = user.getResources();
    // construct query
    OpQuery query = broker.newQuery(ALL_ROOT_ACTIVITIES_OF_GIVEN_TYPE);
    query.setCollection("resourceIds", resources);
    LinkedList<Integer> type_list = new LinkedList<Integer>();
    for (int bit = types.nextSetBit(0); bit >= 0; bit = types.nextSetBit(bit+1)) {
      type_list.add(bit);
    }
    query.setCollection("types", type_list);
    // type save required...
    final Iterator iter = broker.iterate(query);
    return new Iterator<OpActivity>() {
      public boolean hasNext() {
        return iter.hasNext();
      }

      public OpActivity next() {
        return (OpActivity)iter.next();
      }

      public void remove() {
        iter.remove();
      }
    };
    //return(broker.iterate(query));    
  }

  public Iterator<OpActivity> getMyChildTasks(OpProjectSession session, OpBroker broker, OpActivity activity)
  throws XServiceException {
    return(getMyChildTasks(session, broker, activity, null));
    //   return(activity.getSubActivities().iterator());
  }

  public Iterator<OpActivity> getMyChildTasks(OpProjectSession session, OpBroker broker, OpActivity activity, BitSet types)
  throws XServiceException {
    LinkedList<OpActivity> ret = new LinkedList<OpActivity>();
    Iterator<OpActivity> iter = activity.getSubActivities().iterator();
    OpActivity to_add;
    while (iter.hasNext()) {
      to_add = iter.next();
      if (readGranted(session, to_add))
      {
        if (types == null || types.get(to_add.getType())) {
          ret.add(to_add);
        }
      }
    }
    return(ret.iterator());
  }
  
  /**
   * Puts the given <code>activity</code> to the users list of activities. 
   * @param activity the Map of attributes representing the work slip
   * @throws XExeption the given {@link OpWorkSlip} is of an invalid state, or the given {@link OpWorkSlip} already exists.
   * @throws IllegalArgumentException if a required attribute is missing. In this case the work slip will not be added.
   */

  public void insertMyAdhocTask(OpProjectSession session, OpBroker broker, 
      OpActivity activity)
  throws XServiceException
  {
    //task name - mandatory
    if (activity.getName() == null) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EMPTY_NAME_ERROR_CODE)));
    }

    //project & resource
    if (activity.getProjectPlan() == null) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_PROJECT_ERROR_CODE)));
    }

    if (activity.getAssignments() == null || activity.getAssignments().isEmpty()) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
    }

    // can only insert AdHoc tasks
    if (activity.getType() != OpActivity.ADHOC_TASK) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE)));
    }
   
    // check rights
    if (!writeGranted(session, activity)) {
      throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP, 
          OpUserError.INSUFFICIENT_PRIVILEGES)); 
    }

    // get activity sequence
    int sequence = 0;
    OpBroker query_broker = session.newBroker();
    OpQuery query = query_broker.newQuery(MAX_ACTIVITY_SEQUENCE);
    Iterator it = query_broker.list(query).iterator();
    if (it.hasNext()) {
      Integer maxSeq = (Integer) it.next();
      if (maxSeq != null) {
        sequence = maxSeq.intValue() + 1;
      }
    }
    query_broker.close();
    activity.setSequence(sequence);
//    activity.setProjectPlan(activity.getProjectPlan());
//    activity.setAttachments(new HashSet());
//    activity.setAssignments(new HashSet());
    
    broker.makePersistent(activity);
    Iterator<OpAssignment> assignmentIter = activity.getAssignments().iterator();
    while (assignmentIter.hasNext()) {
      broker.makePersistent(assignmentIter.next());
    }
    // attachments are explicitely stored
    Iterator<OpAttachment> attachmentIter = activity.getAttachments().iterator();
    OpAttachment attachment;
    OpContent content;
    while (attachmentIter.hasNext()) {
      attachment = attachmentIter.next();
      content = attachment.getContent();
      if (content != null) {
        broker.makePersistent(content);        
      }
      broker.makePersistent(attachment);
    }
  
    broker.getConnection().flush();
  }
     
  public void deleteMyAdhocTask(OpProjectSession session, OpBroker broker, OpActivity activity)
    throws XServiceException
  {
    if (activity == null) {
      logger_.warn("ERROR: Could not find object with ID " + activity.getID());
      throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.TASK_NOT_FOUND_ERROR_CODE));
    }
    // check rights
    if (!writeGranted(session, activity)) {
      throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP, 
          OpUserError.INSUFFICIENT_PRIVILEGES)); 
    }

    if ((activity.getType() & OpActivity.ADHOC_TASK) != OpActivity.ADHOC_TASK) {
      throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE)); 
    }      
    
    // cannot delete if work records exist
    for (Iterator iterator = activity.getAssignments().iterator(); iterator.hasNext();) {
       OpAssignment assignment = (OpAssignment) iterator.next();
       if (!assignment.getWorkRecords().isEmpty()) {
         throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EXISTING_WORKSLIP_ERROR_CODE));
       }
    }
    broker.deleteObject(activity);
  }

  public void updateMyAdhocTask(OpProjectSession session, OpBroker broker, OpActivity activity)
    throws XServiceException
  {
    if (activity == null) {
      throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.TASK_NOT_FOUND_ERROR_CODE));
    }
    // check rights
    if (!writeGranted(session, activity)) {
      throw new XServiceException(session.newError(OpUserServiceImpl.ERROR_MAP, 
          OpUserError.INSUFFICIENT_PRIVILEGES)); 
    }
    
    if ((activity.getType() & OpActivity.ADHOC_TASK) != OpActivity.ADHOC_TASK) {
      throw new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.INVALID_TYPE_ERROR_CODE)); 
    }      

    //task name - mandatory
    if (activity.getName() == null) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.EMPTY_NAME_ERROR_CODE)));
    }

    //project & resource
    if (activity.getProjectPlan() == null) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_PROJECT_ERROR_CODE)));
    }

    if (activity.getAssignments() == null || activity.getAssignments().isEmpty()) {
      throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
    }
    broker.getConnection().flush();
  } 

  /**
   * @param id
   * @return
   * @throws XServiceException 
   */

  public OpActivity getMyActivityById(OpProjectSession session, OpBroker broker, long id)
    throws XServiceException {
    OpActivity activity = (OpActivity)broker.getObject(OpActivity.class, id);
    if (!readGranted(session, activity)) {
      return(null);
    }
    return(activity);
  }
  
  public OpActivity getMyActivityByIdString(OpProjectSession session, OpBroker broker, String id_string)
    throws XServiceException
  {
    OpActivity activity = (OpActivity)broker.getObject(id_string);
    if (!readGranted(session, activity)) {
      return(null);
    }
    
    return(activity);
  }
  

//  public void insertMyAttachment(OpProjectSession session, OpBroker broker, OpAttachment attachment, OpActivity activity)
//  throws XServiceException {
//    OpActivityDataSetFactory.createAttachment(broker, activity, activity.getProjectPlan(), attachmentElement, null);
//  }

  public void deleteMyAttachment(OpProjectSession session, OpBroker broker, OpAttachment attachment) {
    broker.deleteObject(attachment);
  }

//  public void updateMyAttachment(OpProjectSession session, OpBroker broker, OpAttachment attachment)
//    throws XServiceException {
//  }
  
  private static boolean accessGranted(OpProjectSession session, 
      OpProjectNode project, OpResource resource) {
    if (!session.isLoggedOn())
      return(false);
    if (session.userIsAdministrator())
      return(true);
    
    byte level = project.getEffectiveAccessLevel();
    if ((level & OpPermission.ADMINISTRATOR) == OpPermission.ADMINISTRATOR ||
        (level & OpPermission.MANAGER) == OpPermission.MANAGER) {
      return(true);
    }
    if ((level & OpPermission.CONTRIBUTOR) == OpPermission.CONTRIBUTOR) {
      return(session.isUser(resource.getUser()));
    }
    return(false);
  }
  
  private static boolean readGranted(OpProjectSession session, 
      OpActivity activity) {
    if (!session.isLoggedOn())
      return(false);
    OpProjectPlan project_plan = activity.getProjectPlan();
    byte level = project_plan.getProjectNode().getEffectiveAccessLevel();
    if ((level & OpPermission.ADMINISTRATOR) == OpPermission.ADMINISTRATOR ||
        (level & OpPermission.MANAGER) == OpPermission.MANAGER) {
      return(true);
    }
//    if ((activity.getType() & TYPE_ADHOC_TASK) == TYPE_ADHOC_TASK) {
//      OpBroker broker = session.newBroker();
//      if ((session.user(broker).getAssignments().size() <= 0) && 
//          OpProjectDataSetFactory.getProjectToResourceMap(session).isEmpty())
//        return(false); // user has no assignments and no permissions to projects
//      return(true);
//    }
    if (((level & OpPermission.CONTRIBUTOR) == OpPermission.CONTRIBUTOR) ||
       ((activity.getType() & TYPE_ADHOC_TASK) == TYPE_ADHOC_TASK)) {

      // now check that user is owner of at least one assignments
      Iterator assignment_iter = activity.getAssignments().iterator();
      OpAssignment assignment;
      if (!assignment_iter.hasNext()) { // no assignments
        return(true);
      }
      while (assignment_iter.hasNext()) {
        assignment = (OpAssignment) assignment_iter.next();
        OpResource resource = assignment.getResource();
        if (resource == null)
          throw(new XServiceException(session.newError(ERROR_MAP, OpMyTasksError.NO_RESOURCE_ERROR_CODE)));
        if (session.isUser(resource.getUser())) {
          return(true);
        }
      }
      return(false);
    }
    return(false);
  }

  private static boolean writeGranted(OpProjectSession session, 
      OpActivity activity) {
    // admin, manager or (contributor with at least one assignment to the user) 
    return(readGranted(session, activity));
//    if (!session.isLoggedOn())
//      return(false);
//    if (session.userIsAdministrator())
//      return(true);
//    
//    byte level = activity.getProjectPlan().getProjectNode().getEffectiveAccessLevel();
//    if ((level & OpPermission.ADMINISTRATOR) == OpPermission.ADMINISTRATOR ||
//        (level & OpPermission.MANAGER) == OpPermission.MANAGER) {
//      return(true);
//    }
//    if ((level & OpPermission.CONTRIBUTOR) == OpPermission.CONTRIBUTOR) {
//      // now check that user is owner of all assignments
//      Iterator assignment_iter = activity.getAssignments().iterator();
//      OpAssignment assignment;
//      while (assignment_iter.hasNext()) {
//        assignment = (OpAssignment) assignment_iter.next();
//        if (!session.isUser(assignment.getResource().getUser())) {
//          return(false);
//        }
//      }
//      return(true);
//    }
//    return(false);
  }
}
