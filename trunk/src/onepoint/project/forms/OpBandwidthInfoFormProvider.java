/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.express.server.XFormProvider;
import onepoint.service.server.XSession;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectCalendar;

import java.util.HashMap;

/**
 * Form provider for the bandwidth info dialog.
 *
 * @author mihai.costin
 */
public class OpBandwidthInfoFormProvider implements XFormProvider {

   private static final String MIN_VALUE = "MinValue";
   private static final String MAX_VALUE = "MaxValue";
   private static final String AVG_VALUE = "AvgValue";

   private static final String MIN_FIELD = "MinField";
   private static final String MAX_FIELD = "MaxField";
   private static final String AVG_FIELD = "AvgField";

   public void prepareForm(XSession session, XComponent form, HashMap parameters) {

      OpProjectSession projectSession = (OpProjectSession) session;
      OpProjectCalendar calendar = projectSession.getCalendar();

      Double minValue = (Double) parameters.get(MIN_VALUE);
      String minValueString = calendar.localizedDoubleToString(minValue.doubleValue(), 2);
      Double maxValue = (Double) parameters.get(MAX_VALUE);
      String maxValueString = calendar.localizedDoubleToString(maxValue.doubleValue(), 2);
      Double avgValue = (Double) parameters.get(AVG_VALUE);
      String avgValueString = calendar.localizedDoubleToString(avgValue.doubleValue(), 2);

      form.findComponent(MIN_FIELD).setStringValue(minValueString);
      form.findComponent(MAX_FIELD).setStringValue(maxValueString);
      form.findComponent(AVG_FIELD).setStringValue(avgValueString);

   }
}
