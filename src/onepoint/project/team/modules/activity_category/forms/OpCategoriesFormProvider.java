/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.team.modules.activity_category.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObjectOrderCriteria;
import onepoint.persistence.OpQuery;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityCategory;
import onepoint.service.server.XSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpCategoriesFormProvider implements XFormProvider {

   private static final XLog logger = XLogFactory.getLogger(OpCategoriesFormProvider.class, true);

   /*form's components */
   private final static String CATEGORY_DATA_SET = "CategoryDataSet";
   private final static String IS_ADMIN_ROLE_DATA_FIELD = "AdminRoleDataField";
   private final static String NEW_CATEGORY_BUTTON = "NewCategory";
   private final static String INFO_BUTTON = "Info";
   private final static String EDIT_BUTTON = "Edit";
   private final static String DELETE_BUTTON = "Delete";


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      OpBroker broker = session.newBroker();
      //disable buttons that require selection
      form.findComponent(INFO_BUTTON).setEnabled(false);
      form.findComponent(EDIT_BUTTON).setEnabled(false);
      form.findComponent(DELETE_BUTTON).setEnabled(false);

      boolean isUserAdministrator = session.userIsAdministrator();
      //only admin can create categories
      form.findComponent(NEW_CATEGORY_BUTTON).setEnabled(isUserAdministrator);
      form.findComponent(IS_ADMIN_ROLE_DATA_FIELD).setBooleanValue(isUserAdministrator);

      //configure activity category sort order
      Map sortOrder = new HashMap(1);
      sortOrder.put(OpActivityCategory.NAME, OpObjectOrderCriteria.ASCENDING);
      OpObjectOrderCriteria categoryOrderCriteria = new OpObjectOrderCriteria(OpActivityCategory.ACTIVITY_CATEGORY, sortOrder);
      OpQuery query = broker.newQuery("select category from OpActivityCategory as category" + categoryOrderCriteria.toHibernateQueryString("category"));
      Iterator categories = broker.list(query).iterator();

      //fill category data set
      XComponent dataSet = form.findComponent(CATEGORY_DATA_SET);

      XComponent dataRow = null;
      XComponent dataCell = null;
      OpActivityCategory category = null;
      while (categories.hasNext()) {
         category = (OpActivityCategory) categories.next();
         //do not show inactive categories
         if (!category.getActive()) {
            continue;
         }
         dataRow = new XComponent(XComponent.DATA_ROW);
         dataRow.setStringValue(category.locator());
         dataSet.addChild(dataRow);
         // Name (0)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(category.getName());
         dataRow.addChild(dataCell);
         // Description (0)
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setStringValue(category.getDescription());
         dataRow.addChild(dataCell);
         // Color
         dataCell = new XComponent(XComponent.DATA_CELL);
         dataCell.setIntValue(category.getColor());
         dataRow.addChild(dataCell);
      }

      broker.close();
   }

}
