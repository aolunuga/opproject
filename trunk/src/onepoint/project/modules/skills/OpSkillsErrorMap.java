/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.skills;

import onepoint.error.XErrorMap;

public class OpSkillsErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "skill.error";
//   public static final int EMPTY_NAME_ERROR = 0;
//   public static final int NO_PARENT_CATEGORY = 1;
//   public static final int INSUFICIENT_PERMISSIONS_ERROR_CODE = 2;

   public OpSkillsErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpSkillsError.EMPTY_NAME_ERROR, OpSkillsError.EMPTY_NAME_ERROR_NAME);
      registerErrorCode(OpSkillsError.NO_PARENT_CATEGORY_ERROR, OpSkillsError.NO_PARENT_CATEGORY_NAME);
      registerErrorCode(OpSkillsError.NAME_NOT_UNIQUE_ERROR, OpSkillsError.NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpSkillsError.INSUFICIENT_PERMISSIONS_ERROR, OpSkillsError.INSUFICIENT_PERMISSIONS_ERROR_NAME);
   }

}
