/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.sql.Date;
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

   private String name;
   private byte type;
   private String description;
   private Date start;
   private Date finish;
   private double budget;
   private OpProjectNode superNode;
   private Set subNodes;
   private OpProjectNode templateNode;
   private Set instanceNodes;
   private OpProjectPlan plan;
   private Set<OpProjectNodeAssignment> assignments;
   private Set goals;
   private Set toDos;
   private OpProjectStatus status;

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

   public void setBudget(double budget) {
      this.budget = budget;
   }

   public double getBudget() {
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

   public void setGoals(Set goals) {
      this.goals = goals;
   }

   public Set getGoals() {
      return goals;
   }

   public void setToDos(Set toDos) {
      this.toDos = toDos;
   }

   public Set getToDos() {
      return toDos;
   }

   public void setStatus(OpProjectStatus status) {
      this.status = status;
   }

   public OpProjectStatus getStatus() {
      return status;
   }
}
