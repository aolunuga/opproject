/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.work;

/**
 * Work slip error codes and names
 * @author ovidiu.lupas
 */
public abstract class OpWorkError {

   // Error codes
   public final static int INCORRECT_ACTUAL_EFFORT = 1;
   public final static int INCORRECT_REMAINING_EFFORT = 2;
   public final static int INCORRECT_MATERIAL_COSTS = 3;
   public final static int INCORRECT_TRAVEL_COSTS = 4;
   public final static int INCORRECT_EXTERNAL_COSTS = 5;
   public final static int INCORRECT_MISCELLANEOUS_COSTS = 6;
   public final static int DATE_MISSING = 7;
   public final static int DUPLICATE_DATE = 8;
   public final static int WORK_SLIP_NOT_FOUND = 9;
   public final static int INCORRECT_WORK_SLIP = 10;
   public final static int INCORRECT_ASSIGNMENT = 11;
   public final static int CREATOR_MISSING = 12;
   public final static int WORK_RECORDS_MISSING = 13;

   // Error names
   public final static String INCORRECT_ACTUAL_EFFORT_NAME = "IncorrectActualEffort";
   public final static String INCORRECT_REMAINING_EFFORT_NAME = "IncorrectRemainingEffort";
   public final static String INCORRECT_MATERIAL_COSTS_NAME = "IncorrectMaterialCosts";
   public final static String INCORRECT_TRAVEL_COSTS_NAME = "IncorrectTravelCosts";
   public final static String INCORRECT_EXTERNAL_COSTS_NAME = "IncorrectExternalCosts";
   public final static String INCORRECT_MISCELLANEOUS_COSTS_NAME = "IncorrectMiscellaneousCosts";
   public final static String DATE_MISSING_NAME = "DateMissing";
   public final static String DUPLICATE_DATE_NAME = "DuplicateDate";
   public final static String WORK_SLIP_NOT_FOUND_NAME = "WorkSlipNotFound";
   public final static String CREATOR_MISSING_NAME = "CreatorMissing";
   public final static String WORK_RECORDS_MISSING_NAME = "WorkRecordsMissing";
}
