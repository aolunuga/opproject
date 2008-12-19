/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import onepoint.persistence.OpCustomSubTypable;
import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpSubTypable;
import onepoint.project.modules.calendars.OpWorkCalendar;
import onepoint.project.modules.custom_attribute.OpCustomType;
import onepoint.project.modules.custom_attribute.OpCustomizableObject;
import onepoint.project.modules.customers.OpCustomer;
import onepoint.project.modules.discussion.OpDiscussionGroup;
import onepoint.project.modules.documents.OpFolder;
import onepoint.project.modules.report.OpReport;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.user.OpLock;
import onepoint.project.modules.user.OpLockable;
import onepoint.project.modules.user.OpPermission;
import onepoint.project.modules.user.OpPermissionable;

public class OpProjectNode extends OpCustomizableObject implements OpPermissionable, OpLockable, OpSubTypable, OpCustomSubTypable {

   public final static String PROJECT_NODE = "OpProjectNode";

   public final static String NAME = "Name";
   public final static String TYPE = "Type";
   public final static String CUSTOM_TYPE = "CustomType";
   public final static String DESCRIPTION = "Description";
   public final static String START = "Start";
   public final static String FINISH = "Finish";
   public final static String BUDGET = "Budget";
   public final static String SUPER_NODE = "SuperNode";
   public final static String SUB_NODES = "SubNodes";
   public final static String TEMPLATE_NODE = "TemplateNode";
   public final static String INSTANCE_NODES = "InstanceNodes";
   public final static String PLAN = "Plan";
   public final static String ASSIGNMENTS = "Assignments";
   public final static String GOALS = "Goals";
   public final static String TO_DOS = "ToDos";
   public final static String STATUS = "Status";
   public final static String PROBABILITY = "Probability";
   public final static String PRIORITY = "Priority";
   public final static String ARCHIVED = "Archived";
   public final static String PROGRESS_TRACKED = "ProgressTracked";

   // Project types are ordered in default sort order
   public final static byte PORTFOLIO = 1;
   public final static byte PROGRAM = 2;
   public final static byte PROJECT = 3;
   public final static byte TEMPLATE = 4;

   // Root portfolio name and description
   public final static String ROOT_PROJECT_PORTFOLIO_NAME = "${RootProjectPortfolioName}";
   public final static String ROOT_PROJECT_PORTFOLIO_DESCRIPTION = "${RootProjectPortfolioDescription}";
   public final static String ROOT_PROJECT_PORTFOLIO_ID_QUERY = "select portfolio.id from OpProjectNode as portfolio where portfolio.Name = '"
        + ROOT_PROJECT_PORTFOLIO_NAME + "' and portfolio.Type = " + PORTFOLIO;

   public final static Integer DEFAULT_PRIORITY = 5;
   public final static Integer DEFAULT_PROBABILITY = 100;
   public final static Boolean DEFAULT_ARCHIVED = Boolean.FALSE;

   private String name;
   private byte type;
   private OpCustomType customType;
   private String description;
   private Date start;
   private Date finish;
   private Double budget;
   private Integer probability = DEFAULT_PROBABILITY;
   private Boolean archived = DEFAULT_ARCHIVED;
   private Integer priority = DEFAULT_PRIORITY;
   private OpProjectNode superNode;
   private Set<OpProjectNode> subNodes;
   private OpProjectNode templateNode;
   private Set instanceNodes;
   private OpProjectPlan plan;
   private Set<OpProjectNodeAssignment> assignments;
   private Set<OpGoal> goals;
   private Set<OpToDo> toDos;
   private OpProjectStatus status;
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();
   private Set<OpReport> reports  = new HashSet<OpReport>();
   private Set<OpFolder> folders = new HashSet<OpFolder>();
   private OpCustomer customer = null;
   private Set<OpDiscussionGroup> discussions;
   private Set<OpPermission> permissions;
   private Set<OpLock> locks;
   private Set<OpProgramLink> subProjects;
   private Set<OpProgramLink> programs;
   private Set<OpActivity> programActivities;
   private Set<OpActivityVersion> programActivityVersions;
   
   public OpProjectNode() {
   }
   
   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setType(byte type) {
      this.type = type;
   }

   public void setCustomType(OpCustomType customType) {
      this.customType = customType;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpCustomSubTypable#getCustomTypeName()
    */
   public OpCustomType getCustomType() {
      return customType;
   }

   public Byte getType() {
      return type;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setStart(Date start) {
      this.start = start;
   }

   public Date getStart() {
      return start;
   }

   public void setFinish(Date finish) {
      this.finish = finish;
   }

   public Date getFinish() {
      return finish;
   }

   public void setBudget(Double budget) {
      this.budget = budget;
   }

   public Double getBudget() {
      return budget;
   }

   public void setSuperNode(OpProjectNode superNode) {
      this.superNode = superNode;
   }

   public OpProjectNode getSuperNode() {
      return superNode;
   }

   public void setSubNodes(Set<OpProjectNode> subNodes) {
      this.subNodes = subNodes;
   }

   public Set<OpProjectNode> getSubNodes() {
      return subNodes;
   }

   public void setTemplateNode(OpProjectNode templateNode) {
      this.templateNode = templateNode;
   }

   public OpProjectNode getTemplateNode() {
      return templateNode;
   }

   public void setInstanceNodes(Set instanceNodes) {
      this.instanceNodes = instanceNodes;
   }

   public Set getInstanceNodes() {
      return instanceNodes;
   }

   public void setAssignments(Set<OpProjectNodeAssignment> assignments) {
      this.assignments = assignments;
   }

   public Set<OpProjectNodeAssignment> getAssignments() {
      return assignments;
   }

   public OpProjectNodeAssignment getAssignmentForResource(OpResource r) {
      if (getAssignments() == null) {
         return null;
      }
      for (OpProjectNodeAssignment pna : getAssignments()) {
         if (pna.getResource().getId() == r.getId()) {
            return pna;
         }
      }
      return null;
   }
   
   public void setPlan(OpProjectPlan plan) {
      this.plan = plan;
   }

   public OpProjectPlan getPlan() {
      return plan;
   }

   public void setGoals(Set<OpGoal> goals) {
      this.goals = goals;
   }

   public Set<OpGoal> getGoals() {
      return goals;
   }

   public void setToDos(Set<OpToDo> toDos) {
      this.toDos = toDos;
   }

   public Set<OpToDo> getToDos() {
      return toDos;
   }

   public void setStatus(OpProjectStatus status) {
      this.status = status;
   }

   public OpProjectStatus getStatus() {
      return status;
   }

   public void setAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
   }
   
   public Set<OpAttachment> getAttachments() {
      return attachments;
   }

   /**
    * Sets the <code>Set</code> of archived reports defined on this project.
    *
    * @param reports - the <code>Set</code> of reports.
    */
   public void setReports(Set<OpReport> reports) {
      this.reports = reports;
   }

   /**
    * Gets the <code>Set</code> of reports defined on this project.
    *
    * @return a <code>Set</code> containing the reports defined on this project.
    */
   public Set<OpReport> getReports() {
      return reports;
   }

   /**
    * Sets the <code>Set</code> of folders defined on this project.
    * @param folders - the <code>Set</code> of folders.
    */
   public void setFolders(Set<OpFolder> folders) {
      this.folders = folders;
   }

   /**
    * Gets the <code>Set</code> of folders defined on this project.
    * @return a <code>Set</code> containing the folders defined on this project.
    */
   public Set<OpFolder> getFolders() {
      return folders;
   }

   /**
    * Gets the project's probability (%)
    *
    * @return a <code>Byte</code> the project's probability in %.
    */
   public Integer getProbability() {
      return probability;
   }

   /**
    * Sets the project's probability (%)
    *
    * @param probability a <code>Byte</code> the project's probability in %.
    */
   public void setProbability(Integer probability) {
      this.probability = probability;
   }


   /**
    * Gets the value of the archived attribute.
    *
    * @return a <code>Boolean</code> representing the value of the archived attribute.
    */
   public Boolean getArchived() {
      return archived;
   }

   /**
    * Sets the value of the archived attributed.
    *
    * @param archived a <code>Boolean</code> which if true, indicates the project is archived.
    */
   public void setArchived(Boolean archived) {
      this.archived = archived;
   }

   /**
    * Gets the project's priority (1..9).
    *
    * @return a <code>Byte</code> the project's priority.
    */
   public Integer getPriority() {
      return priority;
   }

   /**
    * Sets the project's priority (1..9).
    *
    * @param priority a <code>Byte</code> the project's priority.
    */
   public void setPriority(Integer priority) {
      this.priority = priority;
   }

   public OpCustomer getCustomer() {
      return customer;
   }

   public void setCustomer(OpCustomer customer) {
      this.customer = customer;
   }

   public Set<OpDiscussionGroup> getDiscussions() {
      return discussions;
   }

   private void setDiscussions(Set<OpDiscussionGroup> discussions) {
      this.discussions = discussions;
   }

   public void addDiscussion(OpDiscussionGroup discussion) {
      if (getDiscussions() == null) {
         setDiscussions(new HashSet<OpDiscussionGroup>());
      }
      if (getDiscussions().add(discussion)) {
         discussion.setProject(this);
      }
   }
   
   public void removeDiscussion(OpDiscussionGroup discussion) {
      if (getDiscussions() == null) {
         return;
      }
      if (getDiscussions().remove(discussion)) {
         discussion.setProject(null);
      }
   }
   
   /**
    * Fills the project node object with values from the request map, and validates the data.
    *
    * @param request a <code>Map</code> of (String,Object) pairs representing a customer
    *                request.
    * @throws OpEntityException if the project data is not valid.
    */
   public void fillProjectNode(Map request)
        throws OpEntityException {
      Number type = (Number) request.get(TYPE);
      if (type == null) {
         throw new IllegalArgumentException("The type of the project node was not set on the request");
      }
      switch(type.byteValue()) {
         case PORTFOLIO: {
            fillBasicProjectNode(request);
            break;
         }
         case PROJECT: {
            fillProject(request);
            break;
         }
      }
      this.validate();
   }

   /**
    * Fills a project with data from a request.
    * @param request a <code>Map</code> of (String, Object) representing a client request.
    */
   private void fillProject(Map request) {
      this.fillBasicProjectNode(request);

      Date start = (Date) request.get(START);
      this.setStart(start);

      Date finish = (Date) request.get(FINISH);
      this.setFinish(finish);

      Double budget = (Double) request.get(BUDGET);
      this.setBudget(budget != null ? budget : 0);

      Integer priority = (Integer) request.get(PRIORITY);
      this.setPriority(priority);

      Integer probability = (Integer) request.get(PROBABILITY);
      this.setProbability(probability);

      Boolean archived = (Boolean) request.get(ARCHIVED);
      this.setArchived(archived);
   }

   public Map getProjectData() {
      Map pd= new HashMap();
      
      pd.put(TYPE, getType());
      pd.put(NAME, getName());
      pd.put(DESCRIPTION, getDescription());
      pd.put(START, getStart());
      pd.put(FINISH, getFinish());
      pd.put(BUDGET, getBudget());
      pd.put(PRIORITY, getPriority());
      pd.put(PROBABILITY, getProbability());
      pd.put(ARCHIVED, getArchived());
      return pd; 
   }
   
   /**
    * Fills a portfolio with data from a request.
    *
    * @param request a <code>Map</code> of (String, Object) representing a client request.
    */
   private void fillBasicProjectNode(Map request) {
      Number type = (Number) request.get(TYPE);
      if (type != null) {
         this.setType(type.byteValue());
      }

      String name = (String) request.get(NAME);
      this.setName(name);

      String description = (String) request.get(DESCRIPTION);
      this.setDescription(description);
   }

   /**
    * Validates the project node according to its type.
    *
    * @throws OpEntityException if the project is not valid.
    */
   private void validate()
        throws OpEntityException {
      switch (type) {
         case PROJECT: {
            validateProject();
            break;
         }
         case PORTFOLIO: {
            validateBasicProjectNode();
            break;
         }
      }
   }

   /**
    * Validates a project based on the value of its fields.
    *
    * @throws OpEntityException if the project is not valid.
    */
   private void validateProject()
        throws OpEntityException {
      if (name == null || name.trim().length() == 0) {
         throw new OpEntityException(OpProjectError.PROJECT_NAME_MISSING);
      }
      if (start == null) {
         throw new OpEntityException(OpProjectError.START_DATE_MISSING);
      }
      if (finish != null && start.after(finish)) {
         throw new OpEntityException(OpProjectError.END_DATE_INCORRECT);
      }
      if (budget == null || budget < 0) {
         throw new OpEntityException(OpProjectError.BUDGET_INCORRECT);
      }
      if (probability == null || probability < 0 || probability > 100) {
         throw new OpEntityException(OpProjectError.PROBABILITY_NOT_VALID);
      }
      if (priority == null || priority < 1 || priority > 9) {
         throw new OpEntityException(OpProjectError.PRIORITY_NOT_VALID);
      }
   }

   /**
    * Validates a portfolio based on the value of its fields.
    *
    * @throws OpEntityException if the portfolio is not valid.
    */
   private void validateBasicProjectNode()
        throws OpEntityException {
      if (name == null || name.trim().length() == 0) {
         throw new OpEntityException(OpProjectError.PORTFOLIO_NAME_MISSING);
      }
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#getPermissions()
    */
   public Set<OpPermission> getPermissions() {
      return permissions;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpPermissionable#setPermissions(java.util.Set)
    */
   public void setPermissions(Set<OpPermission> permissions) {
      this.permissions = permissions;
   }
   
   public void addPermission(OpPermission permission) {
      if (getPermissions() == null) {
         setPermissions(new HashSet<OpPermission>());
      }
      if (getPermissions().add(permission)) {
//    	  if (permission.getObject() != this) {
    	  permission.setObject(this);
//    	  }
      }
   }

   /**
    * @param opPermission
    * @pre
    * @post
    */
   public void removePermission(OpPermission opPermission) {
      Set<OpPermission> perm = getPermissions();
      if (perm != null) {
    	  if (perm.remove(opPermission)) {
    		  opPermission.setObject(null);
    	  }
      }
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpLockable#getLocks()
    */
   public Set<OpLock> getLocks() {
      return locks;
   }

   /* (non-Javadoc)
    * @see onepoint.persistence.OpLockable#setLocks(java.util.Set)
    */
   public void setLocks(Set<OpLock> locks) {
      this.locks = locks;
   }

   public void addLock(OpLock lock) {
      if (getLocks() == null) {
         setLocks(new HashSet<OpLock>());
      }
      if (getLocks().add(lock)) {
         lock.setTarget(this);
      }
   }
   
   public void removeLock(OpLock lock) {
      if (getLocks() == null) {
         return;
      }
      if (getLocks().remove(lock)) {
         lock.setTarget(null);
      }
   }

   public Set<OpProgramLink> getSubProjects() {
      return subProjects;
   }

   
   private void setSubProjects(Set<OpProgramLink> subProjects) {
      this.subProjects = subProjects;
   }

   public OpProgramLink addSubProject(OpProjectNode subProject) {
      return new OpProgramLink(this, subProject);
   }
   
   public OpProgramLink addProgram(OpProjectNode program) {
      return new OpProgramLink(program, this);
   }
   
   public void addSubProjectLink(OpProgramLink link) {
      if (getSubProjects() == null) {
         setSubProjects(new HashSet<OpProgramLink>());
      }
      if (getSubProjects().add(link)) {
         link.setProgram(this);
      }
   }
   
   public void addProgramLink(OpProgramLink link) {
      if (getPrograms() == null) {
         setPrograms(new HashSet<OpProgramLink>());
      }
      if (getPrograms().add(link)) {
         link.setSubProject(this);
      }
   }
   
   public Set<OpProgramLink> getPrograms() {
      return programs;
   }

   private void setPrograms(Set<OpProgramLink> programs) {
      this.programs = programs;
   }

   /**
    * @deprecated
    */
   public Set<OpActivity> getProgramActivities() {
      return programActivities;
   }

   /**
    * @deprecated
    */
   private void setProgramActivities(Set<OpActivity> programActivities) {
      this.programActivities = programActivities;
   }
   
   /**
    * @deprecated
    */
   public void addProgramActivity(OpActivity programActivity) {
      if (getProgramActivities() == null) {
         setProgramActivities(new HashSet<OpActivity>());
      }
      if (getProgramActivities().add(programActivity)) {
         programActivity.setSubProject(this);
      }
   }
   
   /**
    * @deprecated
    */
   public void removeProgramActivity(OpActivity programActivity) {
      if (getProgramActivities() == null) {
         return;
      }
      if (getProgramActivities().remove(programActivity)) {
         programActivity.setSubProject(null);
      }
   }
   
   public Set<OpActivityVersion> getProgramActivityVersions() {
      return programActivityVersions;
   }

   private void setProgramActivityVersions(Set<OpActivityVersion> programActivityVersions) {
      this.programActivityVersions = programActivityVersions;
   }
   
   public void addProgramActivityVersion(OpActivityVersion programActivityVersion) {
      if (getProgramActivityVersions() == null) {
         setProgramActivityVersions(new HashSet<OpActivityVersion>());
      }
      if (getProgramActivityVersions().add(programActivityVersion)) {
         programActivityVersion.setSubProject(this);
      }
   }
   
   public void removeProgramActivityVersion(OpActivityVersion programActivityVersion) {
      if (getProgramActivityVersions() == null) {
         return;
      }
      if (getProgramActivityVersions().remove(programActivityVersion)) {
         programActivityVersion.setSubProject(null);
      }
   }

   public boolean updateWorkCalendar(OpWorkCalendar newWC) {
      return getPlan().updateWorkCalendar(newWC);
   }

   public void addSubNode(OpProjectNode pn) {
      if (getSubNodes() == null) {
         setSubNodes(new HashSet<OpProjectNode>());
      }
      if (getSubNodes().add(pn)) {
         pn.setSuperNode(this);
      }
   }

   public void removeSubNode(OpProjectNode pn) {
      if (getSubNodes() == null) {
         return;
      }
      if (getSubNodes().remove(pn)) {
         pn.setSuperNode(null);
      }
   }

   public void addGoal(OpGoal ng) {
      if (getGoals() == null) {
         setGoals(new HashSet<OpGoal>());
      }
      if (getGoals().add(ng)) {
         ng.setProjectNode(this);
      }
   }

   public void addProjectNodeAssignment(OpProjectNodeAssignment ass) {
      if (getAssignments() == null) {
         setAssignments(new HashSet<OpProjectNodeAssignment>());
      }
      if (getAssignments().add(ass)) {
         ass.setProjectNode(this);
      }
   }
   
   public void removeProjectNodeAssignment(OpProjectNodeAssignment del) {
      if (getAssignments() == null) {
         return;
      }
      if (getAssignments().remove(del)) {
         del.setProjectNode(null);
      }
   }
   
}
