/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.user;

public abstract class OpUserError {

   // Error codes
   public final static int PASSWORD_MISMATCH = 1;
   public final static int USER_UNKNOWN = 2;

   public final static int FIRST_NAME_MISSING = 3;
   public final static int LAST_NAME_MISSING = 4;
   public final static int LOGIN_MISSING = 5;
   public final static int PASSWORD_MISSING = 6;
   public final static int LOGIN_ALREADY_USED = 7;

   public final static int GROUP_NAME_MISSING = 8;
   public final static int GROUP_NAME_ALREADY_USED = 9;

   public final static int USER_NOT_FOUND = 10;
   public final static int GROUP_NOT_FOUND = 11;

   public final static int GROUP_NOT_EMPTY = 12;
   public final static int EMAIL_INCORRECT = 13;

   public final static int INSUFFICIENT_PRIVILEGES = 14;
   public final static int SESSION_USER = 15;
   public final static int EVERYONE_GROUP = 16;
   public final static int LOOP_ASSIGNMENT = 17;
   public final static int INVALID_USER_LEVEL = 18;

   // Error names
   public final static String PASSWORD_MISMATCH_NAME = "PasswordMismatch";
   public final static String USER_UNKNOWN_NAME = "UserUnknown";

   public final static String FIRST_NAME_MISSING_NAME = "FirstNameMissing";
   public final static String LAST_NAME_MISSING_NAME = "LastNameMissing";
   public final static String LOGIN_MISSING_NAME = "LoginNameMissing";
   public final static String PASSWORD_MISSING_NAME = "PasswordMissing";
   public final static String LOGIN_ALREADY_USED_NAME = "LoginAlreadyUsed";

   public final static String GROUP_NAME_MISSING_NAME = "GroupNameMissing";
   public final static String GROUP_NAME_ALREADY_USED_NAME = "GroupNameAlreadyUsed";

   public final static String USER_NOT_FOUND_NAME = "UserNotFound";
   public final static String GROUP_NOT_FOUND_NAME = "GroupNotFound";

   public final static String GROUP_NOT_EMPTY_NAME = "GroupNotEmpty";

   public final static String EMAIL_INCORRECT_NAME = "EmailIncorrect";

   public final static String INSUFFICIENT_PRIVILEGES_NAME = "InsufficientPrivileges";
   public final static String SESSION_USER_NAME = "SessionUser";
   public final static String EVERYONE_GROUP_NAME = "EveryoneGroup";
   public final static String LOOP_ASSIGNMENT_NAME = "LoopAssignment";
   public final static String INVALID_USER_LEVEL_NAME = "InvalidUserLevel";

}