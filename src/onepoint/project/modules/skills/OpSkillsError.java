/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.skills;

public interface OpSkillsError {

   // Error codes
   public static final int EMPTY_NAME_ERROR = 1;
   public static final int NO_PARENT_CATEGORY_ERROR = 2;
   public static final int NAME_NOT_UNIQUE_ERROR = 3;
   public static final int INSUFICIENT_PERMISSIONS_ERROR = 4;
   public static final int ROOT_CATEGORY_NOT_EDITABLE = 5;
   public static final int CATEGORY_NOT_FOUND = 6;
   public static final int SKILL_NOT_FOUND = 7;
   public static final int SKILLS_STILL_IN_USE = 8;
   public static final int CYCLIC_MOVE = 9;

   // Error names
   public static final String EMPTY_NAME_ERROR_NAME = "SkillNameNotSpecified";
   public static final String NO_PARENT_CATEGORY_NAME = "SuperCategoryNotSpecified";
   public static final String NAME_NOT_UNIQUE_NAME = "NameNotUnique";
   public static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";
   public static final String ROOT_CATEGORY_NOT_EDITABLE_NAME = "RootCategoryNotEditable";
   public static final String CATEGORY_NOT_FOUND_NAME = "CategoryNotFound";
   public static final String SKILL_NOT_FOUND_NAME = "SkillNotFound";
   public static final String SKILLS_STILL_IN_USE_NAME = "SkillsStillInUse";
   public static final String CYCLIC_MOVE_NAME = "CyclicMove";

}
