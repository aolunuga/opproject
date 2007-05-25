/**
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */
package onepoint.project.modules.project_planning.test;

import onepoint.express.XComponent;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.project.OpActivityComment;
import onepoint.project.modules.project_planning.OpProjectPlanningService;
import onepoint.project.test.TestDataFactory;
import onepoint.service.XMessage;

import java.util.HashMap;

/**
 * This class contains helper methods for managing project plan data
 *
 * @author lucian.furtos
 */
public class ProjectPlanningTestDataFactory extends TestDataFactory {

   /**
    * Creates a new data factory with the given session
    *
    * @param session session to use for data retrieval.
    */
   public ProjectPlanningTestDataFactory(OpProjectSession session) {
      super(session);
   }

   public static XMessage exportActivitiesMsg(XComponent dataSet, String fileName) {
      XMessage request = new XMessage();
      request.setArgument(OpProjectPlanningService.ACTIVITY_SET, dataSet);
      request.setArgument("file_name", fileName);
      return request;
   }

   public static XMessage importActivitiesMsg(String id, Boolean editMode, byte[] bytes) {
      XMessage request = new XMessage();
      request.setArgument("project_id", id);
      request.setArgument("edit_mode", editMode);
      request.setArgument("bytes_array", bytes);
      return request;
   }

   public static XMessage editActivitiesMsg(String id) {
      XMessage request = new XMessage();
      request.setArgument("project_id", id);
      return request;
   }

   public static XMessage revertActivitiesMsg(String id) {
      XMessage request = new XMessage();
      request.setArgument("project_id", id);
      return request;
   }

   public static XMessage insertCommentMsg(String id, String name, String text) {
      HashMap map = new HashMap();
      map.put(OpProjectPlanningService.ACTIVITY_ID, id);
      map.put(OpActivityComment.NAME, name);
      map.put(OpActivityComment.TEXT, text);
      XMessage request = new XMessage();
      request.setArgument("comment_data", map);
      return request;
   }

   public static XMessage deleteCommentMsg(String id) {
      XMessage request = new XMessage();
      request.setArgument("comment_id", id);
      return request;
   }

}
