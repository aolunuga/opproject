/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.skills;

import onepoint.persistence.OpEntityException;
import onepoint.persistence.OpObject;
import onepoint.service.server.XServiceException;

import java.util.HashSet;
import java.util.Set;

public class OpSkill extends OpObject {

//   public final static String SKILL_CATEGORY = "OpResourcePool";

   public final static String NAME = "Name";
   public final static String DESCRIPTION = "Description";
   public final static String SUPER_CATEGORY = "SuperCategory";

   public static final byte SKILL_TYPE = 1;
   public static final byte CATEGORY_TYPE = 2;

   public final static String SUB_SKILLS = "SubSkills";

   // Root resource pool
   public final static String ROOT_SKILL_CATEGORY_NAME = "${RootSkillCategoryName}";
   public final static String ROOT_SKILL_CATEGORY_DESCRIPTION = "${RootSkillCategoryDescription}";
   public final static String ROOT_SKILL_CATEGORY_ID_QUERY = "select category.ID from OpSkill as category where category.Name = '" + ROOT_SKILL_CATEGORY_NAME + "'";

   private Set<OpSkill> subSkills;// = new HashSet<OpSkill>();

   private byte type;
   private String name;
   private String description;
   private OpSkill superCategory;
   private Set<OpSkillRating> ratings;

   /**
    * 
    */
   public OpSkill() {
      this(SKILL_TYPE);
   }

   /**
    * 
    */
   public OpSkill(byte type) {
      setType(type); 
   }

   public void setSubSkills(Set subSkills) {
      this.subSkills = subSkills;
   }

   public Set getSubSkills() {
      return subSkills;
   }

   public byte getType() {
      return type;
   }
   
   public void setType(byte type) {
      this.type = type;
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

   public void setSuperCategory(OpSkill superPool) {
      this.superCategory = superPool;
   }
   
   public OpSkill getSuperCategory() {
      return superCategory;
   }

   /**
    * Test if this <code>OpSkill</code> is valid.
    *
    * @throws onepoint.persistence.OpEntityException
    *          if any validation constraints are broken
    */
   public void validate()
        throws OpEntityException {
      if (name == null) {
         throw (new OpEntityException(OpSkillsError.EMPTY_NAME_ERROR));
      }
      
      if (description == null) {
         description = "";
      }
      if (subSkills != null) {
         for (OpSkill sub : subSkills) {
            sub.validate();
         }
      }

   }

   public Set<OpSkillRating> getRatings() {
      return ratings;
   }

   private void setRatings(Set<OpSkillRating> ratings) {
      this.ratings = ratings;
   }

   public void addRating(OpSkillRating rating) {
      if (this.ratings == null) {
         this.ratings = new HashSet<OpSkillRating>();
      }
      this.ratings.add(rating);
   }
}