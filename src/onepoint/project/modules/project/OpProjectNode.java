/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;

import java.sql.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpProjectNode extends OpObject {

   public final static String PROJECT_NODE = "OpProjectNode";

   public final static String NAME = "Name";
   public final static String TYPE = "Type";
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

   // Project types are ordered in default sort order
   public final static byte PORTFOLIO = 1;
   public final static byte PROGRAM = 2;
   public final static byte PROJECT = 3;
   public final static byte TEMPLATE = 4;

   // Root portfolio name and description
   public final static String ROOT_PROJECT_PORTFOLIO_NAME = "${RootProjectPortfolioName}";
   public final static String ROOT_PROJECT_PORTFOLIO_DESCRIPTION = "${RootProjectPortfolioDescription}";
   public final static String ROOT_PROJECT_PORTFOLIO_ID_QUERY = "select portfolio.ID from OpProjectNode as portfolio where portfolio.Name = '"
        + ROOT_PROJECT_PORTFOLIO_NAME + "' and portfolio.Type = " + PORTFOLIO;

   public final static Integer DEFAULT_PRIORITY = 5;
   public final static Integer DEFAULT_PROBABILITY = 100;
   public final static Boolean DEFAULT_ARCHIVED = Boolean.FALSE;

   private String name;
   private byte type;
   private String description;
   private Date start;
   private Date finish;
   private Double budget;
   private Integer probability = DEFAULT_PROBABILITY;
   private Boolean archived = DEFAULT_ARCHIVED;
   private Integer priority = DEFAULT_PRIORITY;
   private OpProjectNode superNode;
   private Set subNodes;
   private OpProjectNode templateNode;
   private Set instanceNodes;
   private OpProjectPlan plan;
   private Set<OpProjectNodeAssignment> assignments;
   private Set<OpGoal> goals;
   private Set<OpToDo> toDos;
   private OpProjectStatus status;
   private Set<OpAttachment> attachments = new HashSet<OpAttachment>();

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setType(byte type) {
      this.type = type;
   }

   public byte getType() {
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

   public void setSubNodes(Set subNodes) {
      this.subNodes = subNodes;
   }

   public Set getSubNodes() {
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

   /**
    * Sets the attachments on the project node entity and sets the <code>OpProjectNode</code> on each
    *    <code>OpAttachment</code> from the set.
    *
    * @param attachments - the <code>Set<OpAttachment></code> of attachments that will be set on the project node.
    */
   public void addAttachments(Set<OpAttachment> attachments) {
      this.attachments = attachments;
      for (OpAttachment attachment : attachments) {
         attachment.setProjectNode(this);
      }
   }

   /**
    * Fills the project node object with values from the request map, and validates the data.
    *
    * @param request a <code>Map</code> of (String,Object) pairs representing a client
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

}
