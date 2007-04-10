/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpResource;

public class OpProjectNodeAssignment extends OpObject {

   public final static String PROJECT_NODE_ASSIGNMENT = "OpProjectNodeAssignment";

   public final static String RESOURCE = "Resource";
   public final static String PROJECT_NODE = "ProjectNode";
   // TODO: Maybe general %-assignment
   // TODO: Maybe store rate as well?

   private OpResource resource;
   private OpProjectNode projectNode;

   public void setResource(OpResource resource) {
      this.resource = resource;
   }

   public OpResource getResource() {
      return resource;
   }

   public void setProjectNode(OpProjectNode projectNode) {
      this.projectNode = projectNode;
   }

   public OpProjectNode getProjectNode() {
      return projectNode;
   }

}
