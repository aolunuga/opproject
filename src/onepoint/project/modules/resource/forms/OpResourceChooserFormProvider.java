/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.modules.resource.forms;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.express.server.XFormProvider;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpLocator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourceDataSetFactory;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.service.server.XSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OpResourceChooserFormProvider implements XFormProvider {

   // Form parameters and component IDs
   private final static String CALLING_FRAME_ID = "CallingFrameID";
   private final static String FILTERED_OUT_IDS = "FilteredOutIds";
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

      // Put all resource names into project data-set (values are IDs)
      XComponent dataSet = form.findComponent(RESOURCE_SET);
      OpBroker broker = session.newBroker();
      OpResourceDataSetFactory.retrieveResourceDataSet(session, broker, dataSet);
      broker.close();

      //filter if necessary
      List filteredIds = (List) parameters.get(FILTERED_OUT_IDS);
      filterResourcesSet(filteredIds, dataSet);

      //disable pools/resources
      boolean showPools = ((Boolean) parameters.get(ENABLE_POOLS)).booleanValue();
      boolean showResources = ((Boolean) parameters.get(ENABLE_RESOURCES)).booleanValue();
      enableResourcesSet(dataSet, showResources, showPools);

   }

   /**
    * Makes item selectable in the resources data set, based on the value of the given parameters.
    * @param dataSet a <code>XComponent(DATA_SET)</code> representing the resources set.
    * @param showResources a <code>boolean</code> indicating whether to make resources selectable or not.
    * @param showPools a <code>boolean</code> indicating whether to make pools selectable or not.
    */
   private void enableResourcesSet(XComponent dataSet, boolean showResources, boolean showPools) {
      for (int i = 0; i < dataSet.getChildCount(); i++) {
         XComponent dataRow = (XComponent) dataSet.getChild(i);
         String id = XValidator.choiceID(dataRow.getStringValue());
         OpLocator locator = OpLocator.parseLocator(id);
         Class prototypeClass = locator.getPrototype().getInstanceClass();
         if (prototypeClass.equals(OpResource.class)) {
            dataRow.setSelectable(showResources);
         }
         else if (prototypeClass.equals(OpResourcePool.class)) {
            dataRow.setSelectable(showPools);
         }
      }
   }


   /**
    * Filters out from the given resources set the resource rows which have the id among the given list of filtered ids.
    * @param filteredIds a <code>List</code> of choice ids.
    * @param dataSet a <code>XComponent(DATA_SET)</code> representing the resources data set.
    */
   private void filterResourcesSet(List filteredIds, XComponent dataSet) {
      //filter out data
      List removed = new ArrayList();
      if (filteredIds != null) {
         for (int i = 0; i < dataSet.getChildCount(); i++) {
            XComponent row = (XComponent) dataSet.getChild(i);
            String rowId = XValidator.choiceID(row.getStringValue());
            if (filteredIds.contains(rowId)) {
               removed.add(row);
            }
         }
         for (int i = 0; i < removed.size(); i++) {
            XComponent row = (XComponent) removed.get(i);
            dataSet.removeChild(row.getIndex());
         }
      }
   }
}
