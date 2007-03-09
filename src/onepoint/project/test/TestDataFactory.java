/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.test;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpObject;
import onepoint.persistence.OpTransaction;
import onepoint.project.OpProjectSession;

/**
 * This is a helper class that helps you to generate strctures necessary into test (E.g to call services) and
 * in the same time to retrieve data from database since there is no  clear mechanism for doing that into product.
 *
 * @author calin.pavel
 */
public class TestDataFactory {

   // This is the session that must be used to get data.
   protected OpProjectSession session;


   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public TestDataFactory(OpProjectSession session) {
      this.session = session;
   }

   /**
    * Delete an object
    *
    * @param object a pesisted <code>OpObject</code>
    */
   public void deleteObject(OpObject object) {
      OpBroker broker = session.newBroker();
      OpTransaction t = broker.newTransaction();
      broker.deleteObject(object);
      t.commit();
      broker.close();
   }

   /**
    * Helper method used to create a permission set
    *
    * @param level    the permission level (Administrator|Contributer|Observer)
    * @param userId   the locator of the user
    * @param userName the cname of the user
    * @return the permission DATA_SET
    */
   public static XComponent createPermissionSet(byte level, String userId, String userName) {
      XComponent permSet = new XComponent(XComponent.DATA_SET);
      XComponent row = new XComponent(XComponent.DATA_ROW);
      XComponent cell = new XComponent(XComponent.DATA_CELL);
      // first level
      row.setOutlineLevel(0);
      row.addChild(cell);
      cell.setByteValue(level);
      permSet.addChild(row);
      // user level
      row = new XComponent(XComponent.DATA_ROW);
      row.setOutlineLevel(1);
      row.setValue(XValidator.choice(userId, userName));
      permSet.addChild(row);

      return permSet;
   }
}
