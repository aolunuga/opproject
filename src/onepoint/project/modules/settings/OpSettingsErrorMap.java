/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
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
     }

}
