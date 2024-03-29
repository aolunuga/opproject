/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.settings;

import onepoint.error.XErrorMap;

/**
 * Setting map where error codes are registered
 * @author ovidiu.lupas
 */
public class OpSettingsErrorMap extends XErrorMap {

   public final static String SETTINGS_MAP_ID = "settings.error";

     OpSettingsErrorMap() {
        super(SETTINGS_MAP_ID);
        registerErrorCode(OpSettingsError.LAST_WORK_DAY_INCORRECT, OpSettingsError.LAST_WORK_DAY_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.DAY_WORK_TIME_INCORRECT, OpSettingsError.DAY_WORK_TIME_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.WEEK_WORK_TIME_INCORRECT, OpSettingsError.WEEK_WORK_TIME_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.EMAIL_INCORRECT, OpSettingsError.EMAIL_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT, OpSettingsError.REPORT_REMOVE_TIME_PERIOD_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT, OpSettingsError.RESOURCE_MAX_AVAILABILITY_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.MILESTONE_CONTROLING_INCORRECT, OpSettingsError.MILESTONE_CONTROLING_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.FIRST_WORK_DAY_INCORRECT, OpSettingsError.FIRST_WORK_DAY_INCORRECT_NAME);
        registerErrorCode(OpSettingsError.INVALID_PULSE_VALUE, OpSettingsError.INVALID_PULSE_VALUE_NAME);
        registerErrorCode(OpSettingsError.WORKSLIP_CONTROLLING_INCORRECT, OpSettingsError.WORKSLIP_CONTROLLING_INCORRECT_NAME);
     }

}
