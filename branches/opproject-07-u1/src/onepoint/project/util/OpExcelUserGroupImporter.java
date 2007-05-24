/*
 * Copyright(c) Onepoint Software GmbH 2007. All Rights Reserved.
 *
 */ 

/**
 * 
 */
package onepoint.project.util;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpQuery;
import onepoint.project.OpInitializer;
import onepoint.project.OpProjectSession;
import onepoint.project.configuration.OpConfigurationLoader;
import onepoint.project.modules.resource.OpResource;
import onepoint.project.modules.resource.OpResourcePool;
import onepoint.project.modules.resource.OpResourceService;
import onepoint.project.modules.user.*;
import onepoint.project.test.Constants;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocaleMap;
import onepoint.resource.XLocaleMapLoader;
import onepoint.resource.XResourceBroker;
import onepoint.service.XMessage;
import onepoint.service.server.XLocalServer;
import onepoint.service.server.XServiceManager;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Script used to import user, groups, resources and pools from an predefined xml file.
 * 
 * @author dfreis
 *
 */

public class OpExcelUserGroupImporter {

   private static final XLog logger = XLogFactory.getLogger(OpExcelUserGroupImporter.class, true);

   // Query for retrieving subjects by name
   private static final String SELECT_SUBJECT_BY_NAME_QUERY = "select subject from OpSubject as subject where subject.Name = ?";
   private static final String SELECT_RESOURCE_BY_NAME_QUERY = "select resource from OpResource as resource where resource.Name = ?";
   private static final String SELECT_RESOURCEPOOL_BY_NAME_QUERY = "select pool from OpResourcePool as pool where pool.Name = ?";
   private static final String SELECT_GROUP_BY_NAME_QUERY = SELECT_SUBJECT_BY_NAME_QUERY; //"select group from OpGroup as group where group.Name = ?";

   private static final Short DEPARTMENT_POS = new Short((short)-1);
   private static final Short USERS_POS = new Short((short)-2);
   
   /**
    * Header field section - firstname
    */
   private static final String FIRSTNAME = "Vorname";
   /**
    * Header field section - lastname
    */
   private static final String LASTNAME = "Nachname";
   /**
    * Header field section - email
    */
   private static final String EMAIL = "email Firma";
   /**
    * Header field section - username
    */
   private static final String USERNAME = "K\u00fcrzel";
   
   /**
    * indicates the end of the data section within the xls sheet. 
    */
   private static final String END_OF_ROW_DELIMITER = "* RM = Ressourcen Manager";

   private static final String LANGUAGE = "de";
   
   /**
    * the title of the sheet
    */
   private String title = null;

   /**
    * list storing all user data 
    */
   private List userData = new LinkedList();

   /**
    * list storing all department data 
    */
   private List departmentData = new LinkedList();
   
   /**
    * map storing the header fields and there corresponding column positions
    */
   private HashMap headerData = null;
   
   /**
    * the user service
    */
   private OpUserService userService;

   /**
    * the resource service
    */
   private OpResourceService resourceService;

   /**
    * the express server
    */
   private XLocalServer server;
   
   /**
    * the onepoint project session 
    */
   private OpProjectSession session;

   /**
    * flag indicating whether existing database objects should be replaced or not. 
    */
   private boolean replace = false;

	 /**
	  * the current department for internal use during parser only
	  */
	 private TreeMap currentDepartment;
   
   /**
    * Default constructor, setting up the onepoint system.
    */
   public OpExcelUserGroupImporter() {
      //all tests must use GMT dates (same as the application)
      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
      
      String projectHome = OpEnvironmentManager.getOnePointHome();
      //if not found among OS environment variables, set it to the working dir.
      if (projectHome == null) {
         OpEnvironmentManager.setOnePointHome(new File("").getAbsolutePath());
      }

      XResourceBroker.setResourcePath(Constants.RESOURCE_PATH);
      XLocaleMap locale_map = new XLocaleMapLoader().loadLocaleMap(Constants.LOCALES_OLM_XML);
      XLocaleManager.setLocaleMap(locale_map);

      // check for config file = 
      File configFile = new File (OpEnvironmentManager.getOnePointHome(), OpConfigurationLoader.CONFIGURATION_FILE_NAME);
      if (!configFile.exists())
      {
         printUsageAndExit("ERROR: Onepoint Project connection config file not found (at: "+configFile.getPath()+")!\n"
               +" please start tomcat and connect to onepoint project with your browser and "+
               "fill in the connection settings\n"
               +" do not forget to stop tomcat afterwards!");
      }

      OpInitializer.init(OpProjectConstants.TEAM_EDITION_CODE);

      this.server = new XLocalServer();
      this.session = new OpProjectSession();
      this.session.setServer(this.server);

      userService = (OpUserService) XServiceManager.getService("UserService");  
      resourceService = (OpResourceService) XServiceManager.getService("ResourceService");
   }

   /**
    * starts the uploading process using the given {@link String filename}, administrator 
    * user and password ({@link String adminuser} and {@link String adminpwd}) and whether 
    * existing database objects should be replaced or not.
    * @param filename the name of the xls file to parse.
    * @param adminuser the administrator user, default is 'administrator'
    * @param adminpwd the administrators password in clear text, default is '' (empty password). 
    * @param replace whether existing database object are to be replaced or not.
    * @return 0 if everything went OK or an error code.
    */

   public int start(final String filename, final String adminuser, 
         final String adminpwd, final boolean replace) {
      this.replace = replace;
      // sign on as admin
      final XMessage request = new XMessage();
      request.setArgument(OpUserService.LOGIN, adminuser);
      request.setArgument(OpUserService.PASSWORD, adminpwd);
      userService.signOn(session, request);

      try {
         // prepare data
         parse(new FileInputStream(filename));
         // process data
         processData();
         return 0;
      }
      catch (final FileNotFoundException exc) {
         System.err.println("Error: file '"+filename+"' not found!");
         return(-1);
      }
      catch (final IOException exc) {
         System.err.println("Error: during read of file '"+filename+"'!");
         exc.printStackTrace();
         return -2;
      }
   }


   /**
    * Parses the given {@link InputStream stream} and creates an internal data representation.
    *
    * @param stream the stream to parse.
    * @throws IOException in case of an I/O error.
    */
   private void parse(InputStream stream) throws IOException {
      HSSFWorkbook workbook = new HSSFWorkbook(stream);
      logger.info("number of sheets: "+workbook.getNumberOfSheets());
      logger.info("sheet name at 0 is: "+workbook.getSheetName(0));
      HSSFSheet sheet = workbook.getSheetAt(0);
      logger.info("sheet name at 0 has '"+(sheet.getLastRowNum()-sheet.getFirstRowNum()+1)+"' rows");
      //HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(sheet, workbook);
      
      int rowPos = sheet.getFirstRowNum();
      while (rowPos <= sheet.getLastRowNum()) {
         HSSFRow row = sheet.getRow(rowPos++);
         if (row == null) {
            continue;
         }
         
         Iterator cellIter = row.cellIterator();
         HSSFCell cell;
         TreeMap cellData = new TreeMap();
         while (cellIter.hasNext()) {
            cell = (HSSFCell) cellIter.next();            
            int cellType = cell.getCellType();
            switch (cellType) {
            case HSSFCell.CELL_TYPE_BLANK:
               break;
            case HSSFCell.CELL_TYPE_ERROR:
               break;
            case HSSFCell.CELL_TYPE_FORMULA:
               cellData.put(new Short(cell.getCellNum()), cell);
               break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
               cellData.put(
                     new Short(cell.getCellNum()), new Boolean(cell.getBooleanCellValue()));
               break;
            case HSSFCell.CELL_TYPE_NUMERIC:
               // try as double:
               cellData.put(
                     new Short(cell.getCellNum()), new Double(cell.getNumericCellValue()));
               break;
            case HSSFCell.CELL_TYPE_STRING:
               if (END_OF_ROW_DELIMITER.equals(cell.getStringCellValue())) {
                  return;
               }
               cellData.put(new Short(cell.getCellNum()), cell.getStringCellValue());
               break;
            default:
//               System.err.println("DEF: "+cell);
               break;
            }
         }
         handleRow(cellData);
      }
   }

   /**
    * handles one row of data.
    * @param rowData the map representing all the rows data.
    */
   private void handleRow(TreeMap rowData) {
      if (rowData.size() < 1)
         return;
      if (rowData.size() < 4) {// either title or department and possibly resourcemanager
         if (rowData.size() == 1) { 
            Object cell = rowData.values().iterator().next();
            if (cell instanceof String) { // either title or department
              if (title == null) {
                 title  = (String)cell;
                 return;
              }
              departmentData.add(rowData);
              currentDepartment = rowData;
              return;
            }
            return;
         }

         // 1 < cells < 4 -> assuming department
         departmentData.add(rowData);
         currentDepartment = rowData;
         return;
      }
      // got at least 4 cells assuming user data
      if (headerData == null) {
         try {
            Iterator headerIter = rowData.entrySet().iterator();
            headerData = new HashMap();   
            Entry headerEntry;
            while (headerIter.hasNext()) {
               headerEntry = (Entry)headerIter.next();
               String value = (String)headerEntry.getValue();
               value = value.replace('\n', ' ');
               value = value.replace('\r', ' ');
               value = value.replace('\t', ' ');
               value = value.replaceAll("  ", " ");
               headerData.put(value, headerEntry.getKey());
            }
//            System.out.println("Header: "+headerData.keySet());
         } 
         catch (ClassCastException exc) {
            logger.warn("wrong type (non String) in header: ", exc);
            headerData = null;
         }
         return;
      }
      // add department to user
      if (currentDepartment != null)
      {
         rowData.put(DEPARTMENT_POS, currentDepartment);
         // add user to department

         LinkedList users = 
            (LinkedList)currentDepartment.get(USERS_POS);
         if (users == null) {
            users = new LinkedList();
            currentDepartment.put(USERS_POS, users);
         }
         users.add(rowData);
      }
      userData.add(rowData);
      return;
   }

   /**
    * Returns the string value for the given {@link String key} within {@link Map data}.
    * @param data the data to search for.
    * @param key the key to search.
    * @return the String representation of the found data or <code>null</code> if no data was 
    * found.
    */
   private String getStringValue(Map data, String key) {
      if (data == null) {
         return null;
      }
      Short pos = (Short) headerData.get(key);
      if (pos == null) {
         System.err.println("Internal Error: no such header field: '"+key+"'!");
         System.exit(-17);
      }
      Object ret = data.get(pos);
      if (ret == null) {
         return(null);
      }
         
      // cut leading and trailing spaces
      if (ret instanceof String) {
         String val = (String) ret;
         while (val.charAt(0) == ' ') {
            val = val.substring(1);
         }
         while (val.charAt(val.length()-1) == ' ') {
            val = val.substring(0, val.length()-1);
         }
         return val;
      }
      return ((HSSFCell) ret).getStringCellValue();
   }

   /**
    * Returns the Double value for the given {@link String key} within {@link Map data}.
    * @param data the data to search for.
    * @param key the key to search.
    * @return the Double representation of the found data or <code>null</code> if no data was 
    * found.
    */
   private Double getDoubleValue(Map data, String key) {
      if (data == null) {
         return null;
      }
      Short pos = (Short) headerData.get(key);
      if (pos == null) {
         System.err.println("Internal Error: no such header field: '"+key+"'!");
         System.exit(-17);
      }
      Object ret = data.get(pos);
      if (ret == null) {
         return(null);
      }
  
      // cut leading and trailing spaces
      if (ret instanceof Double) {
         return (Double) ret;
      }
      return new Double(((HSSFCell) ret).getNumericCellValue());
   }

   /**
    * Processes all previosly pardes data.
    */

   private void processData() {
      createGroups();
      // create users within groups
      createUsers();
      createPools();
      // create resources within pool and assign managers
      createResources(); 
   }

   /**
    * Creates all resources and adds them to the corresponding pools. 
    * Assigns responsible user and adds manager role permissions. 
    */
   private void createResources() {
      System.out.println("creating resources...");

      Iterator iter = userData.iterator();
      
      Map user;
      while (iter.hasNext()) {
         user = (Map) iter.next();
         String userName = getStringValue(user, USERNAME);
         Double inthourlyrate = getDoubleValue(user, "Stundensatz Intern");
         Double exthourlyrate = getDoubleValue(user, "Stundensatz extern");

         String departmentName = getStringValue((Map) user.get(DEPARTMENT_POS), "KSt");
         String poolid = null;
         OpBroker broker = session.newBroker();
         try {
            OpQuery query = broker.newQuery(SELECT_RESOURCEPOOL_BY_NAME_QUERY);
            query.setString(0, departmentName);
            Iterator departmentIt = broker.iterate(query);
            if (departmentIt.hasNext()) {
               OpResourcePool department = (OpResourcePool) departmentIt.next();
               poolid = department.locator();
            }
            else {
               System.err.println("WARNING: no resource pool found for resource '"+userName+
                                  "' Resource will be created within root resource pool!");  
               OpResourcePool rootPool = OpResourceService.findRootPool(broker);
               if (rootPool != null) {
                  poolid = rootPool.locator();
               }
            }
         }
         finally {
            broker.close();
         }
         
         String managerName = getStringValue((Map) user.get(DEPARTMENT_POS), LASTNAME);
         OpUser manager = getUserForName(managerName);
         XComponent permSet = new XComponent(XComponent.DATA_SET);
         if (manager != null) {
            // create permissions
            XComponent row = new XComponent(XComponent.DATA_ROW);
            XComponent cell = new XComponent(XComponent.DATA_CELL);
            // first level
            row.setOutlineLevel(0);
            row.addChild(cell);
            cell.setByteValue(OpPermission.MANAGER);
            permSet.addChild(row);
            // user level
            row = new XComponent(XComponent.DATA_ROW);
            row.setOutlineLevel(1);
            row.setValue(XValidator.choice(manager.locator(), managerName));
            permSet.addChild(row);
         }         
         
         OpUser responsible = getUserForName(userName);
         createResource(userName, "", 100d, 
               inthourlyrate == null ? 0d : inthourlyrate.doubleValue(),
               exthourlyrate == null ? 0d : exthourlyrate.doubleValue(), 
               false, poolid, null, 
               responsible == null ? null : responsible.locator(),
               permSet);
      }
      System.out.println("all resources created");
   }

   /**
    * Looks up a {@link OpUser} object for a given user name ({@link String userName})
    * @param userName the user name to get the {@link OpUser} object for.
    * @return the found {@link OpUser} object or null if no such object exists.
    */
   private OpUser getUserForName(String userName) {
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(SELECT_SUBJECT_BY_NAME_QUERY);
         query.setString(0, userName);
         Iterator subjectsIt = broker.iterate(query);
         if (subjectsIt.hasNext()) {
            return (OpUser) subjectsIt.next();
         }
      }
      finally {
         broker.close();
      }
      return null;
   }

   /**
    * create all pools and assigns manager permissions.
    */
   private void createPools() {
      System.out.println("creating pools...");
      // find root pool
      String rootPoolId = null;
      OpBroker broker = session.newBroker();
      try {
         OpResourcePool rootPool = OpResourceService.findRootPool(broker);
         if (rootPool != null) {
            rootPoolId = rootPool.locator();
         }
      }
      finally {
         broker.close();
      }
      
      Iterator iter = departmentData.iterator();
      
      Map department;
      while (iter.hasNext()) {
         department = (Map) iter.next();
         String name = getStringValue(department, "KSt");

         String managerName = getStringValue(department, LASTNAME);
         OpUser manager = getUserForName(managerName);
         XComponent permSet = new XComponent(XComponent.DATA_SET);
         if (manager != null) {
            // create permissions
            XComponent row = new XComponent(XComponent.DATA_ROW);
            XComponent cell = new XComponent(XComponent.DATA_CELL);
            // first level
            row.setOutlineLevel(0);
            row.addChild(cell);
            cell.setByteValue(OpPermission.MANAGER);
            permSet.addChild(row);
            // user level
            row = new XComponent(XComponent.DATA_ROW);
            row.setOutlineLevel(1);
            row.setValue(XValidator.choice(manager.locator(), managerName));
            permSet.addChild(row);
         }         
  
         
         createPool(name, "", 0d, 0d, rootPoolId, permSet);
      }
      System.out.println("all pools created");
   }

   
   /**
    * creates all users within there corresponding groups.
    * Each user is assigned an empty password.
    * The users language will be English. 
    */
   private void createUsers() {
      System.out.println("creating users...");

      // first create all users
      Iterator iter = userData.iterator();
      Map user;
      while (iter.hasNext()) {
         user = (Map) iter.next();
         String firstName = getStringValue(user, FIRSTNAME);
         String lastName = getStringValue(user, LASTNAME);
         String email = getStringValue(user, EMAIL);
         String userName = getStringValue(user, USERNAME);
         String passwordHash = new OpSHA1().calculateHash("");
         Byte userLevel = new Byte((byte)0); 
         if (getStringValue(user, "Projekt- Manager") != null) {
            userLevel = new Byte(OpUser.MANAGER_USER_LEVEL); 
         }
         else if (getStringValue(user, "Standard (Lesen)") != null) {
            userLevel = new Byte(OpUser.STANDARD_USER_LEVEL); 
         }
         String departmentName = getStringValue((Map) user.get(DEPARTMENT_POS), "KSt");
         LinkedList groups = new LinkedList();
         OpBroker broker = session.newBroker();
         try {
            OpQuery query = broker.newQuery(SELECT_GROUP_BY_NAME_QUERY);
            query.setString(0, departmentName);
            Iterator groupIt = broker.iterate(query);
            while (groupIt.hasNext()) {
               OpGroup group = (OpGroup) groupIt.next();
               groups.add(group.locator());
            }
         }
         finally {
            broker.close();
         }
         if (userLevel.byteValue() > 0) {
            createUser(userName, passwordHash, userLevel, 
                  "", LANGUAGE, firstName, lastName, email, "", "", "", groups);
         }
      }
      System.out.println("all users created");
   }

   /**
    * create all groups as top level groups.
    */
   private void createGroups() {
      // first create all groups and pools
      System.out.println("creating groups...");
      Iterator iter = departmentData.iterator();
      Map department;
      while (iter.hasNext()) {
         department = (Map) iter.next();
         String title = getStringValue(department, "KSt");
         
         createGroup(title, "", null);
      }
      System.out.println("all groups created");
   }
       
   /**
    * creates a group.
    * @param groupName the name of the group
    * @param description the description of the group
    * @param groups the parent groups or null if the group to create should be a top 
    * level group.
    */
   private void createGroup(String groupName, String description, List groups) {
      String msg = groupName;
      if (description != null && description.length() > 0) {
         msg = description+" ("+msg+")";
      }
      // first delete any existing users with the given userName
      OpGroup group = null;
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(SELECT_SUBJECT_BY_NAME_QUERY);
         query.setString(0, groupName);
         Iterator subjectsIt = broker.iterate(query);
         if (subjectsIt.hasNext()) {
            if (!replace) {
               System.out.println(" Group: "+msg+" already exists, skipped!");
               return;
            }
            group =  (OpGroup) subjectsIt.next();
         }
      } 
      finally {
         broker.close();
      }
      if (group != null) {
         // Now delete user.
         XMessage req = new XMessage();
         req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(new String[]{group.locator()}));
         XMessage res = userService.deleteSubjects(session, req);
         if (res != null)
         {
            System.err.println("Error: while creating group '"+msg+"' - "+res.getError().toString());
         }
      }
      Map groupData = new HashMap();
      
      groupData.put(OpGroup.NAME, groupName);
      groupData.put(OpGroup.DESCRIPTION, description);
      
      groupData.put(OpUserService.ASSIGNED_GROUPS, groups);

      XMessage request = new XMessage();
      request.setArgument(OpUserService.GROUP_DATA, groupData);
      XMessage response = userService.insertGroup(session, request);
      if (response.getError() != null)
      {
         System.err.println("Error: while creating group '"+msg+"' - "+response.getError().toString());
      }
      else {
         System.out.println(" Group: "+msg+" created!");
      }
   }

   /**
    * create a resource.
    * @param name the name of the resource.
    * @param description the description of the resource.
    * @param available the availability rate of the resource.
    * @param inthourlyrate the internal hourly rate.
    * @param exthourlyrate the external hourly rate.
    * @param inheritrate whether rates should be inherited.
    * @param poolid the parent pool id. 
    * @param projects the project of the resource, may be null.
    * @param respondible the locator of the resource responsible user 
    * @param permSet the permission set.
    */
   private void createResource(String name, String description, 
                               double available, double inthourlyrate, double exthourlyrate, 
                               boolean inheritrate, String poolid, ArrayList projects, 
                               String respondible, XComponent permSet) {
      String msg = name;
      if (description != null && description.length() > 0) {
         msg = description+" ("+name+")";
      }

      // first delete any existing users with the given userName
      OpResource resource = null;
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(SELECT_RESOURCE_BY_NAME_QUERY);
         query.setString(0, name);
         Iterator resourceIt = broker.iterate(query);
         if (resourceIt.hasNext()) {
            if (!replace) {
               System.out.println(" Resource: "+msg+" already exists, skipped!");
               return;
            }
            resource =  (OpResource) resourceIt.next();
         }
      } 
      finally {
         broker.close();
      }
      if (resource != null) {
         // Now delete pool.
         XMessage req = new XMessage();
         req.setArgument(OpResourceService.POOL_IDS, Arrays.asList(new String[]{resource.locator()}));
         XMessage res = resourceService.deletePools(session, req);
         if (res != null)
         {
            System.err.println("Error: while deleting resource '"+msg+"' - "+res.getError().toString());
         }
      }
      HashMap resourceData = new HashMap();
      resourceData.put(OpResource.NAME, name);
      resourceData.put(OpResource.DESCRIPTION, description);
      resourceData.put(OpResource.AVAILABLE, new Double(available));
      resourceData.put(OpResource.HOURLY_RATE, new Double(exthourlyrate));
      resourceData.put(OpResource.INHERIT_POOL_RATE, Boolean.valueOf(inheritrate));
      if (respondible != null) {
         resourceData.put("UserID", respondible);
      }
      resourceData.put("PoolID", poolid);
      resourceData.put(OpResourceService.PROJECTS, projects);
      resourceData.put(OpPermissionSetFactory.PERMISSION_SET, permSet);
      
      XMessage request = new XMessage();
      request.setArgument(OpResourceService.RESOURCE_DATA, resourceData);
      
      XMessage response = resourceService.insertResource(session, request);
      if (response.getError() != null)
      {
         System.err.println("Error: while creating resource '"+msg+"' - "+response.getError().toString());
         if (response.getError().getCode() == OpUserError.PERMISSION_LEVEL_ERROR) {
            // create pool without permissions
            System.err.println("Note: trying to create resource without permissions");
            resourceData.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
            response = resourceService.insertResource(session, request);
            if (response.getError() != null)
            {
               System.err.println("Error: while creating resource '"+msg+"' - "+response.getError().toString());
            }
            else {
               System.out.println(" Resource: "+msg+" created!");
            }
         }

      }
      else {
         System.out.println(" Resource: "+msg+" created!");
      }
   }
      
   /**
    * create a pool.
    * @param name the name of the pool.
    * @param description the description of the pool.
    * @param inthourlyrate the internal hourly rate.
    * @param exthourlyrate the external hourly rate.
    * @param superid the locator of the parent pool.
    * @param permSet the permissions set
    */
   private void createPool(String name, String description, 
                           double inthourlyrate, double exthourlyrate, 
                           String superid, XComponent permSet) {
      // first delete any existing users with the given userName
      OpResourcePool pool = null;
      OpBroker broker = session.newBroker();
      try {
         OpQuery query = broker.newQuery(SELECT_RESOURCEPOOL_BY_NAME_QUERY);
         query.setString(0, name);
         Iterator resourceIt = broker.iterate(query);
         if (resourceIt.hasNext()) {
            if (!replace) {
               String msg = name;
               if (description != null && description.length() > 0) {
                  msg = description+" ("+name+")";
               }

               System.out.println(" Pool: "+msg+" already exists, skipped!");
               return;
            }
            pool =  (OpResourcePool) resourceIt.next();
         }
      } 
      finally {
         broker.close();
      }
      if (pool != null) {
         // Now delete pool.
         XMessage req = new XMessage();
         req.setArgument(OpResourceService.POOL_IDS, Arrays.asList(new String[]{pool.locator()}));
         XMessage res = resourceService.deletePools(session, req);
         if (res != null)
         {
            String msg = name;
            if (description != null && description.length() > 0) {
               msg = description+" ("+name+")";
            }
            System.err.println("Error: while deleting pool '"+msg+"' - "+res.getError().toString());
         }
      }
      HashMap poolData = new HashMap();
      poolData.put(OpResourcePool.NAME, name);
      poolData.put(OpResourcePool.DESCRIPTION, description);
      poolData.put(OpResourcePool.HOURLY_RATE, new Double(exthourlyrate));
      poolData.put("SuperPoolID", superid);
      poolData.put(OpPermissionSetFactory.PERMISSION_SET, permSet);

      XMessage request = new XMessage();
      request.setArgument(OpResourceService.POOL_DATA, poolData);
      XMessage response = resourceService.insertPool(session, request);
      String msg = name;
      if (description != null && description.length() > 0) {
         msg = description+" ("+name+")";
      }
      if (response.getError() != null)
      {
         System.err.println("Error: while creating pool '"+msg+"' - "+response.getError().toString());

         if (response.getError().getCode() == OpUserError.PERMISSION_LEVEL_ERROR) {
            // create pool without permissions
            System.err.println("Note: trying to create pool without permissions");
            poolData.put(OpPermissionSetFactory.PERMISSION_SET, new XComponent(XComponent.DATA_SET));
            response = resourceService.insertPool(session, request);
            if (response.getError() != null)
            {
               System.err.println("Error: while creating pool '"+msg+"' - "+response.getError().toString());
            }
            else {
               System.out.println(" Pool: "+msg+" created!");
            }
         }
      }
      else {
         System.out.println(" Pool: "+msg+" created!");
      }
   }

   /**
    * create a user.
    * @param userName the name of the user to create.
    * @param passwordHash the hashed password of the user to create.
    * @param userLevel the users level.
    * @param description the description of the user.
    * @param language the users preffered language
    * @param firstName the first name of the user.
    * @param lastName the last name of the user.
    * @param email the email of the user.
    * @param fax the fax of the user.
    * @param mobile the mobile of the user.
    * @param phone the phone of the user.
    * @param groups the groups the user belongs to.
    */
   private void createUser(String userName, String passwordHash, 
                           Byte userLevel, String description, 
                           String language, 
                           String firstName, String lastName,                           
                           String email, String fax, String mobile, String phone, List groups)
   {
      // first delete any existing users with the given userName
      OpUser user = null;
      OpBroker broker = session.newBroker();
      String msg = userName;
      if (lastName != null && lastName.length() > 0) {
         if (firstName != null && firstName.length() > 0) {
            msg = lastName+" "+firstName+" ("+msg+")";
         } 
         else {
            msg = lastName+" ("+msg+")";
         }
      }
      try {
         OpQuery query = broker.newQuery(SELECT_SUBJECT_BY_NAME_QUERY);
         query.setString(0, userName);
         Iterator subjectsIt = broker.iterate(query);
         if (subjectsIt.hasNext()) {
            if (!replace) {
               System.out.println(" User: "+msg+" already exists, skipped!");
               return;
            }
            user =  (OpUser) subjectsIt.next();
         }
      }
      finally {
         broker.close();
      }
      if (user != null) {
         // Now delete user.
         XMessage req = new XMessage();
         req.setArgument(OpUserService.SUBJECT_IDS, Arrays.asList(new String[]{user.locator()}));
         XMessage res = userService.deleteSubjects(session, req);
         if (res != null)
         {
            System.err.println("Error: while deleting user '"+msg+"' - "+res.getError().toString());
         }
      }
      Map userData = new HashMap();
      userData.put(OpUser.NAME, userName);
      userData.put(OpUser.PASSWORD, passwordHash);
      userData.put(OpUserService.PASSWORD_RETYPED, passwordHash);

      userData.put(OpUserService.USER_LEVEL, Byte.toString(userLevel.byteValue()));
      userData.put(OpUser.DESCRIPTION, description);
      userData.put(OpUserService.LANGUAGE, language);

      userData.put(OpContact.FIRST_NAME, firstName);
      userData.put(OpContact.LAST_NAME, lastName);
      userData.put(OpContact.EMAIL, email);
      userData.put(OpContact.FAX, fax);
      userData.put(OpContact.MOBILE, mobile);
      userData.put(OpContact.PHONE, phone);

      userData.put(OpUserService.ASSIGNED_GROUPS, groups);
      XMessage request = new XMessage();
      request.setArgument(OpUserService.USER_DATA, userData);
      XMessage response = userService.insertUser(session, request);
      if (response.getError() != null)
      {
         System.err.println("Error: while creating user '"+msg+"' - "+response.getError().toString());
      }
      else {
         System.out.println(" User: "+msg+" created!");
      }
   }
   
   /**
    * Main method
    * @param args the command line args.
    */
   public static void main(String[] args) {
      if (args.length < 1) {
         printUsageAndExit(null);
      }
      String adminuser = OpUser.ADMINISTRATOR_NAME;
      String adminpwd = OpUserService.BLANK_PASSWORD;
      String filename = args[0];
      
      boolean replace = false;
      if (args.length > 1) {
         if (args.length > 2) {
            adminuser = args[0];
            adminpwd = new OpSHA1().calculateHash(args[1]);
            if (args.length > 3) {
               filename = args[3];
               replace = true;
            }
            else {
               filename = args[2];            
            }
         }
         else { // #args = 2
            replace = true;
            filename = args[1];            
         }
      }

      
      OpExcelUserGroupImporter importer = new OpExcelUserGroupImporter();
      int ret = importer.start(filename, adminuser, adminpwd, replace);
      if (ret != 0) {
         System.exit(ret);
      }
      
   }

   /**
    * Prints usage information and exits.
    */
   private static void printUsageAndExit(String msg) {
      if (msg != null) {
         System.out.println(msg);
      }
      System.out.println("Usage: java -jar excelscript.jar"+
                         "[admin_user admin_pwd] [replace] file_to_import.xls");
      System.out.println("       admin_user defaults to administrator");
      System.out.println("       admin_pwd defaults to ''");
      
      System.out.println(" IMPORTANT: make sure that you successfully configured "+
                         "Onepoint Project (by starting the applet and filling out the "+
                         "connection settings form) and that tomcat is *NOT* running!");
      System.exit(-1);
   }
}
