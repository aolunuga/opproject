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

   // Error names
   public static final String EMPTY_NAME_ERROR_NAME = "SkillNameNotSpecified";
   public static final String NO_PARENT_CATEGORY_NAME = "SuperCategoryNotSpecified";
   public static final String NAME_NOT_UNIQUE_NAME = "NameNotUnique";
   public static final String INSUFICIENT_PERMISSIONS_ERROR_NAME = "InsuficientPermissions";
}
