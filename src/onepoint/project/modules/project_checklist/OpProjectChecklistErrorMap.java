/**
 * Copyright(c) OnePoint Software GmbH 2008. All Rights Reserved.
 */
package onepoint.project.modules.project_checklist;

import onepoint.error.XErrorMap;

/**
 * @author mihai.costin
 */
public class OpProjectChecklistErrorMap extends XErrorMap {

   private final static String RESOURCE_MAP_ID = "project_checklist.error";

   public OpProjectChecklistErrorMap() {
      super(RESOURCE_MAP_ID);
      registerErrorCode(OpProjectChecklistError.TODO_PRIORITY_ERROR, OpProjectChecklistError.TODO_PRIORITY_ERROR_NAME);
   }
}
