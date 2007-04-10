/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.work.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.work.OpWorkService;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.sql.Date;

/**
 * This class contains helper methods for managing projects data
 *
 * @author lucian.furtos
 */
public class WorkTestDataFactory extends TestDataFactory {

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public WorkTestDataFactory(OpProjectSession session) {
      super(session);
   }

   public static XMessage insertWSMsg(String id, String name, Date date, boolean completed, boolean insert, int type, double actual, double remaining, double material,
        double travel, double external, double misc) {
      XComponent workSet = new XComponent(XComponent.DATA_SET);
      XComponent row = new XComponent(XComponent.DATA_ROW);
      workSet.addChild(row);
      // 0. Name and Id
      XComponent cell = new XComponent(XComponent.DATA_CELL);
      cell.setStringValue(XValidator.choice(id, name));
      row.addChild(cell);
      // 1. actual effort
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(actual);
      row.addChild(cell);
      // 2. Remaining effort
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(remaining);
      row.addChild(cell);
      // 3. Material Costs
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(material);
      row.addChild(cell);
      // 4. Travel costs
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(travel);
      row.addChild(cell);
      // 5. External costs
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(external);
      row.addChild(cell);
      // 6. Miscellaneous Costs
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setDoubleValue(misc);
      row.addChild(cell);
      // 7.
      cell = new XComponent(XComponent.DATA_CELL);
      row.addChild(cell);
      // 8.
      cell = new XComponent(XComponent.DATA_CELL);
      row.addChild(cell);
      // 9.
      cell = new XComponent(XComponent.DATA_CELL);
      row.addChild(cell);
      // 10. Completed
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setBooleanValue(completed);
      row.addChild(cell);
      // 11. activity type
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setIntValue(type);
      row.addChild(cell);
      // 12. activity insert mode
      cell = new XComponent(XComponent.DATA_CELL);
      cell.setBooleanValue(insert);
      row.addChild(cell);

      XMessage request = new XMessage();
      request.setArgument(OpWorkService.START, date);
      request.setArgument(OpWorkService.WORK_RECORD_SET, workSet);

      return request;
   }

}
