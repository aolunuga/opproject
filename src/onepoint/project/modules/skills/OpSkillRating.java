/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.modules.skills;

import onepoint.persistence.OpObject;
import onepoint.project.modules.resource.OpResource;

/**
 * @author dfreis
 *
 */
public class OpSkillRating extends OpObject {
   private double rating = 0d;
   private OpResource resource;
   private OpSkill skill;
   
   public double getRating() {
      return rating;
   }
   public void setRating(double rating) {
      this.rating = rating;
   }
   public OpResource getResource() {
      return resource;
   }
   public void setResource(OpResource resource) {
      this.resource = resource;
   }
   public OpSkill getSkill() {
      return skill;
   }
   public void setSkill(OpSkill skill) {
      this.skill = skill;
   }
//   @Override
//   public String toString() {
//      return "<OpSkillReating>"+skill.getName()+", "+resource.getName()+", "+rating+"</OpSkillReating>";
//   }
}
