/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.skills;

import onepoint.error.XErrorMap;

public class OpSkillsErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "skills.error";
//   public static final int EMPTY_NAME_ERROR = 0;
//   public static final int NO_PARENT_CATEGORY = 1;
//   public static final int INSUFICIENT_PERMISSIONS_ERROR_CODE = 2;

   public OpSkillsErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpSkillsError.EMPTY_NAME_ERROR, OpSkillsError.EMPTY_NAME_ERROR_NAME);
      registerErrorCode(OpSkillsError.NO_PARENT_CATEGORY_ERROR, OpSkillsError.NO_PARENT_CATEGORY_NAME);
      registerErrorCode(OpSkillsError.NAME_NOT_UNIQUE_ERROR, OpSkillsError.NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpSkillsError.INSUFICIENT_PERMISSIONS_ERROR, OpSkillsError.INSUFICIENT_PERMISSIONS_ERROR_NAME);
      registerErrorCode(OpSkillsError.CATEGORY_NOT_FOUND, OpSkillsError.CATEGORY_NOT_FOUND_NAME);
      registerErrorCode(OpSkillsError.ROOT_CATEGORY_NOT_EDITABLE, OpSkillsError.ROOT_CATEGORY_NOT_EDITABLE_NAME);
      registerErrorCode(OpSkillsError.SKILL_NOT_FOUND, OpSkillsError.SKILL_NOT_FOUND_NAME);
      registerErrorCode(OpSkillsError.SKILLS_STILL_IN_USE, OpSkillsError.SKILLS_STILL_IN_USE_NAME);
      registerErrorCode(OpSkillsError.CYCLIC_MOVE, OpSkillsError.CYCLIC_MOVE_NAME);
   }

}
