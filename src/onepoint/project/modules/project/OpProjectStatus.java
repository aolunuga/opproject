/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

import java.util.Set;

/**
 * Project Status Entity.
 *
 * @author mihai.costin
 */
public class OpProjectStatus extends OpObject {

   public final static String PROJECT_STATUS = "OpProjectStatus";
   public final static String SEQUENCE = "Sequence";
   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String COLOR = "Color";
   public final static String PROJECTS = "Projects";

   private int sequence;
   private String name;
   private String description;
   private int color;
   private Set<OpProjectNode> projects;
   /**
    * Flag indicating whether a project status is active or not (i.e deleted but referenced)
    */
   private boolean active = true;

   public void setSequence(int sequence) {
      this.sequence = sequence;
   }

   public int getSequence() {
      return sequence;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDescription() {
      return description;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public int getColor() {
      return color;
   }

   public void setProjects(Set<OpProjectNode> projects) {
      this.projects = projects;
   }

   public Set<OpProjectNode> getProjects() {
      return projects;
   }

   /**
    * Gets the value of the active flag.
    *
    * @return a <code>boolean</code> indicating whether the project status is active or not.
    */
   public boolean getActive() {
      return active;
   }

   /**
    * Sets the value of the active flag.
    *
    * @param active a <code>boolean</code> indicating whether the project status is active or not.
    */
   public void setActive(boolean active) {
      this.active = active;
   }

}
