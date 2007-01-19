/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.List;

public class OpResourceChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String FILTERED_OUT_LOCATORS = "FilteredOutIds";
   private final static String ACTION_HANDLER = "ActionHandler";
   private final static String MULTIPLE_SELECTION = "MultipleSelection";
   private final static String ENABLE_RESOURCES = "EnableResources";
   private final static String ENABLE_POOLS = "EnablePools";

   private final static String RESOURCE_LIST_BOX_ID = "ResourceList";

   public final static String RESOURCE_SET = "ResourceSet";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;

      // Set calling frame
      String callingFrameID = (String) parameters.get(CALLING_FRAME_ID);
      form.findComponent(CALLING_FRAME_ID).setStringValue(callingFrameID);
      String actionHandler = (String) parameters.get(ACTION_HANDLER);
      form.findComponent(ACTION_HANDLER).setStringValue(actionHandler);

      //set selection model
      XComponent list = form.findComponent(RESOURCE_LIST_BOX_ID);
      Boolean multipleSelection = (Boolean) parameters.get(MULTIPLE_SELECTION);
      if (multipleSelection.booleanValue()) {
         list.setSelectionModel(XComponent.MULTIPLE_ROWS_SELECTION);
      }
      else {
         list.setSelectionModel(XComponent.SINGLE_ROW_SELECTION);
      }

      //filter
      List filteredLocators = (List) parameters.get(FILTERED_OUT_LOCATORS);
      form.findComponent(FILTERED_OUT_LOCATORS).setValue(filteredLocators);

      // Put all resource names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(RESOURCE_SET);
      OpResourceDataSetFactory.retrieveFirstLevelsResourceDataSet(session, dataSet, null, null, filteredLocators);

      //disable pools/resources
      boolean showPools = ((Boolean) parameters.get(ENABLE_POOLS)).booleanValue();
      boolean showResources = ((Boolean) parameters.get(ENABLE_RESOURCES)).booleanValue();

      form.findComponent(ENABLE_POOLS).setBooleanValue(showPools);
      form.findComponent(ENABLE_RESOURCES).setBooleanValue(showResources);

      OpResourceDataSetFactory.enableResourcesSet(dataSet, showResources, showPools);

   }

}
