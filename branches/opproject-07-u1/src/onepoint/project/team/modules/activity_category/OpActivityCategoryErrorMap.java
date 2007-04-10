/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.activity_category;

import onepoint.error.XErrorMap;

public class OpActivityCategoryErrorMap extends XErrorMap {

   public final static String RESOURCE_MAP_ID = "activity_category.error";

   OpActivityCategoryErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED, OpActivityCategoryError.CATEGORY_NAME_NOT_SPECIFIED_NAME);
      registerErrorCode(OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE, OpActivityCategoryError.CATEGORY_NAME_NOT_UNIQUE_NAME);
      registerErrorCode(OpActivityCategoryError.CATEGORY_NOT_FOUND, OpActivityCategoryError.CATEGORY_NOT_FOUND_NAME);
      registerErrorCode(OpActivityCategoryError.INSUFFICIENT_PRIVILEGES, OpActivityCategoryError.INSUFFICIENT_PRIVILEGES_NAME);
   }

}
