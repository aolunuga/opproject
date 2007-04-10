/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.activity_category;

/**
 * @author ovidiu.lupas
 */
public abstract class OpActivityCategoryError {

   // Error codes
   public final static int CATEGORY_NAME_NOT_SPECIFIED = 1;
   public final static int CATEGORY_NAME_NOT_UNIQUE = 2;
   public final static int CATEGORY_NOT_FOUND = 3;
   public final static int INSUFFICIENT_PRIVILEGES = 4;
   
   // Error names
   public final static String CATEGORY_NAME_NOT_SPECIFIED_NAME = "CategoryNameNotSpecified";
   public final static String CATEGORY_NAME_NOT_UNIQUE_NAME = "CategoryNameNotUnique";
   public final static String CATEGORY_NOT_FOUND_NAME = "CategoryNotFound";
   public final static String INSUFFICIENT_PRIVILEGES_NAME = "InsuffcientPrivileges";

}
