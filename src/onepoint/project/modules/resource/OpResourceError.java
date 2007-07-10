/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.resource;

public interface OpResourceError {

   // Error codes
   public final static int RESOURCE_NAME_NOT_SPECIFIED = 1;
   public final static int RESOURCE_NAME_NOT_UNIQUE = 2;
   public final static int POOL_NAME_NOT_SPECIFIED = 3;
   public final static int POOL_NAME_NOT_UNIQUE = 4;
   public final static int AVAILABILITY_NOT_VALID = 5;
   public final static int HOURLY_RATE_NOT_VALID = 6;
   public final static int USER_ID_NOT_SPECIFIED = 7;
   public final static int NAME_FORMAT_NOT_SPECIFIED = 8;
   public final static int NAME_FORMAT_NOT_VALID = 9;
   public final static int RESOURCE_NOT_FOUND = 10;
   public final static int UPDATE_ACCESS_DENIED = 11;
   public final static int POOL_NOT_FOUND = 12;
   public final static int RESOURCE_NAME_NOT_VALID = 13;
   public final static int ASSIGN_ACCESS_DENIED = 14;
   public final static int RESOURCE_ASSIGNMENT_WARNING = 15;
   public final static int MANAGER_ACCESS_DENIED = 16;
   public final static int DELETE_RESOURCE_ASSIGNMENTS_DENIED = 17;
   public final static int DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED = 18;
   public final static int ACTIVITY_ASSIGNMENTS_EXIST_ERROR = 19;
   public final static int LOOP_ASSIGNMENT_ERROR = 20;
   public final static int EXTERNAL_RATE_NOT_VALID = 21;
   public final static int ACTIVITY_ASSIGNMENT_VERSIONS_EXIST_ERROR = 26;
   public final static int RESPONSIBLE_ACTIVITIES_EXIST_ERROR = 27;
   public final static int RESPONSIBLE_ACTIVITY_VERSIONS_EXIST_ERROR = 28;

   // Error names
   public final static String RESOURCE_NAME_NOT_SPECIFIED_NAME = "ResourceNameNotSpecified";
   public final static String RESOURCE_NAME_NOT_UNIQUE_NAME = "ResourceNameNotUnique";
   public final static String POOL_NAME_NOT_SPECIFIED_NAME = "PoolNameNotSpecified";
   public final static String POOL_NAME_NOT_UNIQUE_NAME = "PoolNameNotUnique";
   public final static String AVAILABILITY_NOT_VALID_NAME = "AvailabilityNotValid";
   public final static String HOURLY_RATE_NOT_VALID_NAME = "HourlyRateNotValid";
   public final static String USER_ID_NOT_SPECIFIED_NAME = "UserIDNotSpecified";
   public final static String NAME_FORMAT_NOT_SPECIFIED_NAME = "NameFormatNotSpecified";
   public final static String NAME_FORMAT_NOT_VALID_NAME = "NameFormatNotValid";
   public final static String RESOURCE_NOT_FOUND_NAME = "ResourceNotFound";
   public final static String UPDATE_ACCESS_DENIED_NAME = "UpdateAccessDenied";
   public final static String POOL_NOT_FOUND_NAME = "PoolNotFound";
   public final static String RESOURCE_NAME_NOT_VALID_NAME = "ResourceNameNotValid";
   public final static String ASSIGN_ACCESS_DENIED_NAME = "AssignAccessDenied";
   public final static String RESOURCE_ASSIGNMENT_WARNING_NAME = "ResourceAssignmentWarning";
   public final static String MANAGER_ACCESS_DENIED_NAME = "ManagerAccessDenied";
   public final static String DELETE_RESOURCE_ASSIGNMENTS_DENIED_NAME = "DeleteResourceAssignmentsDenied";
   public final static String DELETE_POOL_RESOURCE_ASSIGNMENTS_DENIED_NAME = "DeletePoolResourceAssignmentsDenied";
   public final static String ACTIVITY_ASSIGNMENTS_EXIST_ERROR_NAME = "ActivityAssignmentsExistError";
   public final static String LOOP_ASSIGNMENT_ERROR_NAME = "LoopAssignmentError";
   public final static String EXTERNAL_RATE_NOT_VALID_NAME = "ExternalRateNotValid";
   public final static String ACTIVITY_ASSIGNMENT_VERSIONS_EXIST_ERROR_NAME = "ActivityAssignmentVersionsExistError";
   public final static String RESPONSIBLE_ACTIVITIES_EXIST_ERROR_NAME = "ResponsibleActivitiesExistError";
   public final static String RESPONSIBLE_ACTIVITY_VERSIONS_EXIST_ERROR_NAME = "ResponsibleActivityVersionsExistError";
}
