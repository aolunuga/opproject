/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project_planning.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpProjectDataSetFactory;
import onepoint.service.server.XSession;

import java.util.HashMap;

public class OpProjectNavigatorFormProvider implements XFormProvider {

   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      OpProjectSession session = (OpProjectSession) s;
      XComponent dataSet = form.findComponent("ProjectNavigatorDataSet");

      OpProjectDataSetFactory.retrieveProjectDataSetRootHierarchy(session, dataSet, OpProjectDataSetFactory.ALL_TYPES, false, null);
   }

}
