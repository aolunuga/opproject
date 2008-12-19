package onepoint.project.validators;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import onepoint.express.XComponent;
import onepoint.express.XValidator;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.forms.OpProjectInputFormatException;
import onepoint.project.util.OpProjectConstants;

public abstract class OpProjectValidator extends XValidator {

   private static final XLog logger = XLogFactory.getLogger(OpProjectValidator.class);

   protected final static String ELEMENT_PERMISSIONS_DATA_SET_ID = "FormElementPermissions";
   protected final static String ELEMENT_PERMISSIONS_MAP_ID = "FormElementPermissionsMap";
   
   protected static final String MULTIUSER_INDICATOR_ID = "MultiUserIndicator";

   protected final static String DEFAULT_PERMISSION = "wwrr";
   protected final static String PERMISSION_INVISIBLE = "-";
   protected final static String PERMISSION_HIDDEN = "h";
   protected final static String PERMISSION_READ = "r"; 
   protected final static String PERMISSION_WRITE = "w";
   
   public final static byte ADMINISTRATOR = 64;
   public final static byte MANAGER = 16;
   public final static byte CONTRIBUTOR = 4;
   public final static byte OBSERVER = 2;
   public final static byte EXTERNAL = 1;
   public final static byte DEFAULT_ACCESS_LEVEL = CONTRIBUTOR;

   protected final static Byte USER_LEVEL_DEFAULT = new Byte(Byte.MIN_VALUE);
   protected final static Byte USER_LEVEL_OBSERVER = new Byte(OpProjectValidator.OBSERVER);
   protected final static Byte USER_LEVEL_CONTRIBUTOR = new Byte(OpProjectValidator.CONTRIBUTOR);
   protected final static Byte USER_LEVEL_MANAGER = new Byte(OpProjectValidator.MANAGER);
   protected final static Byte USER_LEVEL_ADMINISTRATOR = new Byte(OpProjectValidator.ADMINISTRATOR);
   
   protected final static Pattern PERMISSION_STRING_PATTERN = Pattern.compile("([rwh\\-])([rwh\\-])([rwh\\-])([rwh\\-])");

   public static final String OBJECT_LOCATOR_ARGUMENT = "objectLocator";
   public static final String DIALOG_CONTENT_ARGUMENT = "dialogContent";
   public static final String DIALOG_CONTENT_MAPPINGS_DATA_SET_ARGUMENT = "mappingsDataSet";

   public static final String INVALID_FIELD_ID = "invalidFieldId";
   public static final String INVALID_FIELD_LABEL_ID = "invalidFieldLabelId";
   public static final String INVALID_TABLE_COLUMN = "invalidTableColumn";
   public static final String INVALID_VALUE = "invalidValue";
   public static final String DIALOG_TITLE_ARGUMENT = "dialogTitle";
   
   private static final Map/*<String, Byte>*/ ACCESSLEVEL_MAP = new HashMap/*<String, Byte>*/() {
      {
         put("administrators", new Byte(OpProjectValidator.ADMINISTRATOR));
         put("managers", new Byte(OpProjectValidator.MANAGER));
         put("observers", new Byte(OpProjectValidator.OBSERVER));
      }
   };
   
   private static final Map/*<Byte, Byte>*/ USER_LEVEL_MAP = new HashMap/*<Byte, Byte>*/() {
      {
         put(new Byte(OpProjectConstants.MANAGER_USER_LEVEL), new Byte(MANAGER));
         put(new Byte(OpProjectConstants.CONTRIBUTOR_USER_LEVEL), new Byte(CONTRIBUTOR));
         put(new Byte(OpProjectConstants.OBSERVER_USER_LEVEL), new Byte(OBSERVER));
      }
   };

   public static byte mapUserLevelToAccessLevel(byte userLevel) {
      Byte accessLevel = (Byte) USER_LEVEL_MAP.get(new Byte(userLevel));
      if (accessLevel == null) {
         return OBSERVER;
      }
      return accessLevel.byteValue();
   }
   
   private final static String ADMINISTRATOR_STRING = "administrator";
   private final static String EVERYONE_STRING = "everyone";
   private final static String USER_STRING = "user";
   
   public static void adjustFormElements(XComponent form,
         int mode) {
      adjustFormElements(form, ELEMENT_PERMISSIONS_DATA_SET_ID,
            null, mode, OpProjectValidator.ADMINISTRATOR);
   }

   public static void adjustFormElements(XComponent form,
         int mode, byte userAccessLevel) {
      adjustFormElements(form, ELEMENT_PERMISSIONS_DATA_SET_ID,
            null, mode, userAccessLevel);
   }

   public static void adjustFormElements(XComponent form,
         String elementPermissionsMapDataSet, int mode) {
      adjustFormElements(form, elementPermissionsMapDataSet,
            null, mode, OpProjectValidator.ADMINISTRATOR);
   }

   public static void adjustFormElements(XComponent form,
         String elementPermissionsMapDataSet, int mode, byte userAccessLevel) {
      adjustFormElements(form, elementPermissionsMapDataSet,
            null, mode, userAccessLevel);
   }

   public static void adjustFormElements(XComponent form,
         String formElementPermissionsDataSetId, String permissionsMapId, int mode) {
      adjustFormElements(form,
            formElementPermissionsDataSetId, permissionsMapId, mode,
            OpProjectValidator.ADMINISTRATOR);
   }
   
   private static final Byte[] PERMISSION_STRING_ACCESS_LEVELS = {USER_LEVEL_ADMINISTRATOR, USER_LEVEL_MANAGER, USER_LEVEL_CONTRIBUTOR, USER_LEVEL_OBSERVER};

   /**
    *  how those data-sets might look like:
    *  
    *    <data-set id="ArticleViewElementPermissions">
    *       <data-row string-value="ArticleTitle"><data-cell string-value="wwwr" /></data-row>
    *       <data-row string-value="ArticleContent"><data-cell string-value="wwwr" /></data-row>
    *       <data-row string-value="Created"><data-cell string-value="rrrr" /></data-row>
    *       <data-row string-value="User"><data-cell string-value="rrrr" /></data-row>
    *       <data-row string-value="OkButton"><data-cell string-value="wwwr" /></data-row>
    *       <data-row string-value="CancelButton"><data-cell string-value="wwwr" /></data-row>
    *    </data-set>
    *
    * @param session
    * @param form
    * @param multiUserOnlyDataSetId       Elements only active in multiuser mode
    * @param editModeOnlyDataSetId        Elements for editMode only
    * @param formElementPermissionsDataSetId    Depending on the permissions of the user (userAccessLevel)
    * @param editMode                     editMode flag
    * @param userAccessLevel              access level of the user which should be applied to the elements in the permissions data set
    */
   public static void adjustFormElements(XComponent form,
         String formElementPermissionsDataSetId, String permissionsMapId,
         int mode, byte userAccessLevel) {

      if (permissionsMapId == null) {
         permissionsMapId = formElementPermissionsDataSetId + MAP_POSTFIX;
      }
      
      XComponent elementPermissionsDataSet = null;
      elementPermissionsDataSet = form.findComponent(formElementPermissionsDataSetId);
      
      Map/*<String, Map<Byte, String>>*/ permissionsMap = null;
      permissionsMap = setupPermissionsMap(elementPermissionsDataSet, mode);
      Iterator/*<String>*/ elemIterator = permissionsMap.keySet().iterator();
      while (elemIterator.hasNext()) {
         String elemName = (String) elemIterator.next();

         XComponent elem = form.findComponent(elemName);
         XComponent label = form.findComponent(elemName + LABEL_POSTFIX);
         Map/*<Byte, String>*/ accessMap = (Map) permissionsMap.get(elemName);

         // This requires a certain order:
         String accessLevel = (String) accessMap.get(new Byte(userAccessLevel));

         boolean visible = false;
         boolean hidden = false;
         boolean edit = false;
         boolean enabled = false;
         if (PERMISSION_INVISIBLE.equals(accessLevel)) {
         }
         else if (PERMISSION_HIDDEN.equals(accessLevel)) {
            hidden = true;
            visible = true;
         }
         else if (PERMISSION_READ.equals(accessLevel)) {
            visible = true;
            enabled = false;
         }
         else if (PERMISSION_WRITE.equals(accessLevel)) {
            visible = true;
            edit = true;
            enabled = true;
         }
         if (elem != null) {
            elem.setEditMode(edit);
            elem.setEnabled(enabled);
            if (!hidden) {
               elem.setVisible(visible);
            }            
            elem.setHidden(hidden);
            logger.warn("Element: " + (visible ? "V" : "-") + (edit ? "E" : "-") + (hidden ? "H" : "-") + " (" + elemName + ")");
         }
         if (label != null) {
            label.setVisible(visible);
         }
      }
      if (form.findComponent(permissionsMapId) != null) {
         form.findComponent(permissionsMapId).setValue(permissionsMap);
      }
      form.update();
   }

   private static Map/*<String, Map<Byte, String>>*/ setupPermissionsMap(
         XComponent elementPermissionsDataSet, int mode) {
      Map/*<String, Map<Byte, String>>*/ permissionsMap;
      permissionsMap = new HashMap/*<String, Map<Byte, String>>*/();
      if (elementPermissionsDataSet != null) {
         for (int i = 0; i < elementPermissionsDataSet.getChildCount(); i++) {
            XComponent permissionsRow = (XComponent) elementPermissionsDataSet.getChild(i);
            if (permissionsRow != null) {
               String elementId = permissionsRow.getStringValue();
               Map/*<Byte, String>*/ accessMap = new HashMap/*<Byte, String>*/();
               permissionsMap.put(elementId, accessMap);
               String permissionString = null;
               if (permissionsRow.getChildCount() > mode) {
                  permissionString = ((XComponent) permissionsRow.getChild(mode)).getStringValue();
               }
               if (permissionString == null && permissionsRow.getChildCount() > 0) {
                  permissionString = ((XComponent) permissionsRow.getChild(0)).getStringValue();
               }
               if (permissionString != null) {
                  Matcher m = PERMISSION_STRING_PATTERN.matcher(permissionString);
                  if (!m.matches()) {
                     logger.error("PermissionString invalid for field: " + elementId);
                     continue;
                  }
                  for (int j = 0; j < PERMISSION_STRING_ACCESS_LEVELS.length; j++) {
                     // groups in matchers start with 1 (0 is the whole matching thing)
                     accessMap.put(PERMISSION_STRING_ACCESS_LEVELS[j], m.group(j + 1));
                  }
               }
            }
         }
      }
      return permissionsMap;
   }

   private static final String MAP_POSTFIX = "Map";
   
   public static void setupPermissionMaps(XComponent form, String[] mapDataSetNames, int[] modes) {
      for (int i = 0; i < mapDataSetNames.length; i++) {
         for (int j = 0; j < modes.length; j++) {
            XComponent mapField = form.findComponent(mapDataSetNames[i]
                  + MAP_POSTFIX
                  + (modes[j] != 0 ? Integer.toString(modes[j]) : ""));
            XComponent mapDataSet = form.findComponent(mapDataSetNames[i]);
            if (mapField != null && mapDataSet != null) {
               Map/* <String, Map<Byte, String>> */permissionsMap = setupPermissionsMap(
                     mapDataSet, modes[j]);
               mapField.setValue(permissionsMap);
            }
         }
      }
   }

   static public String getStringFromComponent(XComponent form, String id) {
      return form.findComponent(id).getStringValue();
   }

   static public double getDoubleFromComponent(XComponent form, String id) {
      return form.findComponent(id).getDoubleValue();
   }

   static public int getIntFromComponent(XComponent form, String id) {
      return form.findComponent(id).getIntValue();
   }

   static public boolean getBooleanFromComponent(XComponent form, String id) {
      return form.findComponent(id).getBooleanValue();
   }

   static public void setValueOfComponent(XComponent form, String id, Object value) {
      form.findComponent(id).setValue(value);
   }

   /**
    * transform a mapping data set for mapping object to dialog maps to a data set
    * @param source
    * @param dataRowMapDataSet
    */
   public static void transformDialogMapToDataSet(String[][] source, 
         XComponent dataRowMapDataSet) {
      transformMapToDataSet(source, dataRowMapDataSet, false);
   }
   public static void transformRowMapToDataSet(String[][] source, 
         XComponent dataRowMapDataSet) {
      transformMapToDataSet(source, dataRowMapDataSet, true);
   }

   private static void transformMapToDataSet(String[][] source, 
         XComponent dataRowMapDataSet, boolean isTableRowMapping) {
      int fieldCount = source.length;
      for (int f = 0; f < fieldCount; f++) {
         XComponent dataRow = new XComponent(XComponent.DATA_ROW);
         int specCount = source[f].length;
         for (int i = 0; i < specCount; i++) {
            XComponent dataCell = null;
            Object value = null;
            switch (i) {
            case 0:
               dataRow.setValue(source[f][i]);
               break;
            case ELEMENT_INDEX:
               if (isTableRowMapping) {
                  value = new Integer(Integer.parseInt(source[f][i]));
               } else {
                  value = source[f][i];
               }
               dataCell = new XComponent(XComponent.DATA_CELL);
               break;
            default:
               value = source[f][i];
               dataCell = new XComponent(XComponent.DATA_CELL);
               break;
            }
            if (dataCell != null) {
               dataCell.setValue(value);
               dataRow.addChild(dataCell);
            }
         }
         dataRowMapDataSet.addChild(dataRow);
      }
   }
   
   public static String[][] transformMappingDataSet(
         XComponent dataRowMapDataSet) {
      int fieldCount = dataRowMapDataSet.getChildCount();
      String[][] dataRowMap = new String[fieldCount][];
      for (int f = 0; f < fieldCount; f++) {
         XComponent dataRow = (XComponent) dataRowMapDataSet.getChild(f);
         int specCount = dataRow.getChildCount();
         dataRowMap[f] = new String[specCount + 1];
         dataRowMap[f][0] = dataRow.getValue() == null ? null : dataRow.getValue().toString();
         for (int s = 0; s < specCount; s++) {
            XComponent dataCell = (XComponent) dataRow.getChild(s);
            dataRowMap[f][s + 1] = dataCell.getValue() == null ? null : dataCell.getValue().toString();
         }
      }
      return dataRowMap;
   }
   
   private final static Pattern PATH_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)((\\.[a-zA-Z0-9_]+)*)");
   private final static int NAME_GROUP = 1;
   private final static int ELEMENT_GROUP = 2;

   private final static int ELEMENT_PATH = 0;   // name of the dialog field
   private final static int ELEMENT_NAME = 1;   // name of the dialog field
   private final static int ELEMENT_INDEX = 1;  // index of cell in datarow
   private final static int ELEMENT_PATTERN = 2;
   private final static int ELEMENT_VISIBILITY = 3;
   private final static int ELEMENT_LABEL = 4; // currently used to indicate second part of choice fields

   private final static Pattern DIRECTION_PATTERN = Pattern.compile("(in|out|inout|hidden)");
   private final static int DIRECTION_GROUP = 1;
   private final static String DIRECTION_HIDDEN = "hidden";
   private final static String DIRECTION_IN = "in";
   private final static String DIRECTION_OUT = "out";
   private final static String DIRECTION_INOUT = "inout";
   
   private static final String LABEL_POSTFIX = "Label";

   // value for this index is placed into the row itself (row.setValue()).
   private static final int ROW_VALUE_INDEX = -1;
   private static final int ROW_OUTLINE_LEVEL_INDEX = -2;
   private static final int ROW_EXPANDED_INDEX = -3;

   /**
    * @author peter
    * cannot throw exceptions here...
    */
   public static class FormatError {
      private String fieldId = null;
      private String fieldLabelId = null;
      private Integer tableColumnIndex = null;
      private String invalidValue = null;
      
      public FormatError(String fieldId, String fieldLabelId, String invalidValue) {
         this.fieldId = fieldId;
         this.fieldLabelId = fieldLabelId;
         this.invalidValue = invalidValue;
      }

      public FormatError(String fieldLabelId, String invalidValue) {
         this.fieldLabelId = fieldLabelId;
         this.invalidValue = invalidValue;
      }

      public FormatError(Integer tableColumnIndex, String invalidValue) {
         this.tableColumnIndex = tableColumnIndex;
         this.invalidValue = invalidValue;
      }

      public String getFieldId() {
         return fieldId;
      }

      public String getFieldLabelId() {
         return fieldLabelId;
      }

      public Integer getTableColumnIndex() {
         return tableColumnIndex;
      }

      public String getInvalidValue() {
         return invalidValue;
      }
   }
   
   /**
    * Populates  certain fields of a given form with elements from the given objects as described by the provided Map
    * Example:
    *    <data-set id="CustomerObjectMap">
    *      <data-row string-value="[customer]locator"><data-cell string-value="objectLocator"/><data-cell /><data-cell string-value="out"/></data-row>
    *    </data-set>
    * 
    * The string value if the row describes the path to the field placed into the element named in the first [0] data cell. The second data
    * cell contains a regex describing possible field contents, the third data cell describes the direction the value is passed (in|out|inout)
    * The path to the element is formatted as follows:
    * [<map-element-name>]<path.inside.this.elements.by.getters>
    * 
    * Example: [project]status.name
    * 
    * @param form       Form to populate
    * @param fieldMapId Map of object fields to form field ids to use
    * @param objects    Map of Source object
    */
   public static void populateDialogFieldsFromMap(XComponent form, XComponent dialogFieldsMapDataSet, Map/*<String, Object>*/ objects) {
      String[][] dialogFieldsMap = transformMappingDataSet(dialogFieldsMapDataSet);
      populateDialogFieldsFromMap(form, dialogFieldsMap, objects);
   }
   
   public static void populateDialogFieldsFromMap(XComponent form, String[][] dialogFieldsMap, Map/*<String, Object>*/ objects) {
      for (int i = 0; i < dialogFieldsMap.length; i++) {
         String[] fieldMapping = dialogFieldsMap[i];

         boolean enableField = true;
         boolean writeValue = true;
         boolean showField = true;
         if (fieldMapping.length > ELEMENT_VISIBILITY && fieldMapping[ELEMENT_VISIBILITY] != null) {
            Matcher mv = DIRECTION_PATTERN.matcher(fieldMapping[ELEMENT_VISIBILITY]);
            if (mv.matches()) {
               if (DIRECTION_OUT.equals(mv.group(DIRECTION_GROUP))) {
                  enableField = false;
               }
               else if (DIRECTION_IN.equals(mv.group(DIRECTION_GROUP))) {
                  writeValue = false;
               }
               else if (DIRECTION_HIDDEN.equals(mv.group(DIRECTION_GROUP))) {
                  showField = false;
               }
            }
         }
         boolean mandatory = false;
         if (fieldMapping.length > ELEMENT_PATTERN) {
            String pattern = fieldMapping[ELEMENT_PATTERN];
            if (pattern != null && pattern.length() > 0) {
               if (!Pattern.matches(pattern, "")) {
                  mandatory = true;
               }
            }
         }
         if (fieldMapping.length > ELEMENT_NAME) {
            String fieldId = fieldMapping[ELEMENT_NAME];
            XComponent field = form.findComponent(fieldId);
            if (field == null) {
               continue;
            }
            if (mandatory) {
               String labelId = fieldId + LABEL_POSTFIX;
               XComponent label = form.findComponent(labelId);
               if (label != null) {
                  label.setStyle(XComponent.DEFAULT_LABEL_EMPHASIZED_STYLE);
               }
            }

            String mappingInfo = fieldMapping[ELEMENT_PATH];
            Object value = buildElementValue(objects, mappingInfo);

            if (writeValue) {
               if (field.getComponentType() == XComponent.CHOICE_FIELD) {
                  initChoiceField(field, value != null ? value.toString() : null);
               }
               else {
                  field.setValue(value);
               }
            }
            if (!enableField) {
               field.setEnabled(false);
            }
            if (!showField) {
               field.setVisible(false);
            }
         }
      }
   }
   
   /**
    * takes a valueMap as passed from jes to populate an Object by reflection
    * @param valueMap Map of values as passed by means of commonDialogs.jes
    * @param objects Object for content deposit
    * @throws OpProjectInputFormatException 
    */
   public static FormatError readDialogFieldsIntoMap(XComponent dialogFieldsMapDataSet, Map/*<String, Object>*/ valueMap,
         Map/*<String, Object>*/ objects) {
      String[][] dialogFieldsMap = transformMappingDataSet(dialogFieldsMapDataSet);
      return readDialogFieldsIntoMap(dialogFieldsMap, valueMap, objects);
   }
   
   public static FormatError readDialogFieldsIntoMap(String[][] dialogFieldsMap, Map/*<String, Object>*/ valueMap,
            Map/*<String, Object>*/ objects) {
      
      FormatError error = null;

      for (int i = 0; i < dialogFieldsMap.length; i++) {
         String[] fieldMapping = dialogFieldsMap[i];
         String mappingInfo = fieldMapping[ELEMENT_PATH];

         boolean doReadValue = true;
         if (fieldMapping.length > ELEMENT_VISIBILITY && fieldMapping[ELEMENT_VISIBILITY] != null) {
            Matcher mv = DIRECTION_PATTERN.matcher(fieldMapping[ELEMENT_VISIBILITY]);
            if (mv.matches()) {
               if (DIRECTION_OUT.equals(mv.group(DIRECTION_GROUP))) {
                  doReadValue = false;
               }
            }
         }
         if (!doReadValue) {
            // Always read value (often used for transfer only)
            // continue;
         }
         if (fieldMapping.length > ELEMENT_PATTERN) {
            Object value = valueMap.get(mappingInfo);
            String pattern = fieldMapping[ELEMENT_PATTERN];
            if (pattern != null && pattern.length() > 0) {
               value = value != null ? value : ""; 
               String stringValue = value.toString();
               Matcher mv = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE).matcher(stringValue);
               if (!mv.matches()) {
                  String fieldId = fieldMapping[ELEMENT_NAME];
                  error = new FormatError(fieldId, fieldId
                        + LABEL_POSTFIX, stringValue);
               }
            }
         }
         
         Matcher m = PATH_PATTERN.matcher(mappingInfo);
         if (m.matches()) {
            String mapEntryName = m.group(NAME_GROUP);
            String elementPath = m.group(ELEMENT_GROUP);
            if (elementPath != null && elementPath.length() > 0) {
               elementPath = elementPath.substring(1);
            }
            
            if (elementPath == null || elementPath.length() == 0) {
               objects.put(mapEntryName, valueMap.get(mappingInfo));
            }
            else {
               Object object = objects.get(mapEntryName);
               if (object == null) {
                  // FIXME: this code exists twice...
                  object = new HashMap/*<String, Object>*/();
                  objects.put(mapEntryName, object);
               }
               try {
                  setElementInObject(object, elementPath, valueMap.get(mappingInfo));
               } catch (NoSuchMethodException e) {
                  logger.warn("could not set Element: " + mappingInfo + " - " + e.getMessage());
               }
            }
         }
      }
      return error;
   }
   
   public static XComponent createDataRowFromMap(XComponent dataRowMapDataSet, Map/*<String, Object>*/ objects, boolean editMode) {
      XComponent row = new XComponent(XComponent.DATA_ROW);
      populateDataRowFromMap(row, dataRowMapDataSet, objects, editMode);
      return row;
   }

   public static XComponent createDataRowFromMap(String[][] dataRowMap, Map/*<String, Object>*/ objects, boolean editMode) {
      XComponent row = new XComponent(XComponent.DATA_ROW);
      populateDataRowFromMap(row, dataRowMap, objects, editMode);
      return row;
   }

   /**
    * fill a data row according to the description given by mappingsDataSet with content from objects
    * @param row              the row to fill (this might be done iteratively from different sources)
    * @param dataRowMapDataSet  a dataset containing mappings for elements to rows
    * @param objects          the objects to be mapped to the row
    */
   public static void populateDataRowFromMap(XComponent row, XComponent dataRowMapDataSet, Map/*<String, Object>*/ objects, boolean editMode) {
      String[][] dataRowMap = transformMappingDataSet(dataRowMapDataSet);
      populateDataRowFromMap(row, dataRowMap, objects, editMode);
   }

   public static void populateDataRowFromMap(XComponent row, String[][] dataRowMap, Map/*<String, Object>*/ objects, boolean editMode) {
      for (int i = 0; i < dataRowMap.length; i++) {
         String[] fieldMapping = dataRowMap[i];
         String fieldName = fieldMapping[ELEMENT_PATH];
         if (fieldName == null) {
            continue;
         }
         boolean enableField = editMode;
         boolean writeValue = true;
         boolean showField = true;
         if (fieldMapping.length > ELEMENT_VISIBILITY && fieldMapping[ELEMENT_VISIBILITY] != null) {
            Matcher mv = DIRECTION_PATTERN.matcher(fieldMapping[ELEMENT_VISIBILITY]);
            if (mv.matches()) {
               if (DIRECTION_OUT.equals(mv.group(DIRECTION_GROUP))) {
                  enableField = false;
               }
               else if (DIRECTION_IN.equals(mv.group(DIRECTION_GROUP))) {
                  writeValue = false;
               }
               else if (DIRECTION_HIDDEN.equals(mv.group(DIRECTION_GROUP))) {
                  showField = false;
               }
            }
         }
         if (fieldMapping.length > ELEMENT_INDEX) {
            int cellIdx = Integer.parseInt(fieldMapping[ELEMENT_INDEX]);

            Object value = buildElementValue(objects, fieldName);
            
            if (writeValue) {
               switch (cellIdx) {
               case ROW_VALUE_INDEX:
                  row.setValue(value);
                  break;
               case ROW_OUTLINE_LEVEL_INDEX:
                  row.setOutlineLevel(value == null ? 0 : ((Integer)value).intValue());
                  break;
               case ROW_EXPANDED_INDEX:
                  row.setExpanded(value == null ? false : ((Boolean)value).booleanValue());
                  break;
               default:
                  {
                     if (cellIdx >= row.getChildCount()) {
                        for (int k = row.getChildCount() - 1; k < cellIdx; k++) {
                           row.addChild(new XComponent(XComponent.DATA_CELL));
                        }
                     }
                     XComponent cell = (XComponent) row.getChild(cellIdx);
                     cell.setValue(showField ? value : null);
                     cell.setEnabled(showField && enableField);
                     cell.setEditMode(editMode);
                  }
               }
            }
         }
      }
   }

   /**
    * 
    * @param row
    * @param dataRowMap
    * @param object
    * @param editMode
    * @throws NoSuchMethodException
    * @deprecated ? Not used anyway...
    */
   public static void populateDataRowFromObject(XComponent row, String[][] dataRowMap, Object object, boolean editMode) throws NoSuchMethodException {
      for (int i = 0; i < dataRowMap.length; i++) {
         String[] fieldMapping = dataRowMap[i];
         String fieldName = fieldMapping[ELEMENT_PATH];
         if (fieldName == null) {
            continue;
         }
         boolean enableField = editMode;
         boolean writeValue = true;
         boolean showField = true;
         if (fieldMapping.length > ELEMENT_VISIBILITY) {
            Matcher mv = DIRECTION_PATTERN.matcher(fieldMapping[ELEMENT_VISIBILITY]);
            if (mv.matches()) {
               if (DIRECTION_OUT.equals(mv.group(DIRECTION_GROUP))) {
                  enableField = false;
               }
               else if (DIRECTION_IN.equals(mv.group(DIRECTION_GROUP))) {
                  writeValue = false;
               }
               else if (DIRECTION_HIDDEN.equals(mv.group(DIRECTION_GROUP))) {
                  showField = false;
               }
            }
         }
         if (fieldMapping.length > ELEMENT_INDEX) {
            int cellIdx = Integer.parseInt(fieldMapping[ELEMENT_INDEX]);

            Object value = buildElementValue(object, fieldName);
            
            if (writeValue) {
               switch (cellIdx) {
               case ROW_VALUE_INDEX:
                  row.setValue(value);
                  break;
               case ROW_OUTLINE_LEVEL_INDEX:
                  row.setOutlineLevel(value == null ? 0 : ((Integer)value).intValue());
                  break;
               case ROW_EXPANDED_INDEX:
                  row.setExpanded(value == null ? false : ((Boolean)value).booleanValue());
                  break;
               default:
                  {
                     if (cellIdx >= row.getChildCount()) {
                        for (int k = row.getChildCount() - 1; k < cellIdx; k++) {
                           row.addChild(new XComponent(XComponent.DATA_CELL));
                        }
                     }
                     ((XComponent) row.getChild(cellIdx))
                           .setValue(showField ? value : null);
                     ((XComponent) row.getChild(cellIdx)).setEnabled(showField && enableField);
                  }
               }
            }
         }
      }
   }

   private static Object buildElementValue(Map objects, String fieldName) {
      String idFieldName = choiceID(fieldName);
      String captionFieldName = choiceCaption(fieldName);
      String iconIndexFieldName = choiceIconIndexField(fieldName);

      Object value = getElementFromObjectsMap(objects, idFieldName);
      String caption = null;
      int iconIndex = -1;
      if (captionFieldName != null) {
         Object captionValue = null;
         captionValue = getElementFromObjectsMap(objects, captionFieldName);
         if (captionValue != null) {
             caption = captionValue.toString();
         }
         else {
            // used for "dummy-choices", like indicator stuff...
            caption = "";
         }
      }
      if (iconIndexFieldName != null) {
         Object iconIndexValue = getElementFromObjectsMap(objects, iconIndexFieldName);
         if (iconIndexValue != null && iconIndexValue instanceof Integer) {
            iconIndex = ((Integer) iconIndexValue).intValue();
         }
      }
      if (caption != null || iconIndex != -1) {
         value = choice(value == null ? "" : value.toString(), caption, iconIndex);
      }
      return value;
   }

   /**
    * 
    * @param object
    * @param fieldName
    * @return
    * @throws NoSuchMethodException
    * @deprecated ?
    */
   private static Object buildElementValue(Object object, String fieldName) throws NoSuchMethodException {
      String idFieldName = choiceID(fieldName);
      String captionFieldName = choiceCaption(fieldName);
      String iconIndexFieldName = choiceIconIndexField(fieldName);

      Object value = getElementFromObject(object, fieldName);

      String caption = null;
      int iconIndex = -1;
      if (captionFieldName != null && captionFieldName.length() > 0) {
         Object captionValue = null;
         try {
            captionValue = getElementFromObject(object, captionFieldName);
            if (captionValue != null) {
               caption = captionValue.toString();
            }
         } 
         catch (NoSuchMethodException  exc) {
         }
      }
      if (iconIndexFieldName != null && iconIndexFieldName.length() > 0) {
         try {
            Object iconIndexValue = getElementFromObject(object, iconIndexFieldName);
            if (iconIndexValue != null && iconIndexValue instanceof Integer) {
               iconIndex = ((Integer) iconIndexValue).intValue();
            }
         }
         catch (NoSuchMethodException  exc) {
         }
      }
      if (caption != null || iconIndex != -1) {
         value = choice(value == null ? "" : value.toString(), caption, iconIndex);
      }
      return value;
   }

   private static Object getElementFromObjectsMap(Map objects,
         String fieldName) {
      Object value = objects.get(fieldName);
      if (value != null) {
         return value;
      }
      Matcher m = PATH_PATTERN.matcher(fieldName);
      if (m.matches()) {
         String mapEntryName = m.group(NAME_GROUP);
         String elementPath = m.group(ELEMENT_GROUP);

         if (elementPath != null && elementPath.length() > 0) {
            elementPath = elementPath.substring(1);
         }

         if (elementPath == null || elementPath.length() == 0) {
            value = objects.get(mapEntryName);
         } else {
            try {
               value = getElementFromObject(objects.get(mapEntryName),
                     elementPath);
            } catch (NoSuchMethodException e) {
               logger
                     .warn("could not get " + fieldName + " from objects");
            }
         }
      }
      return value;
   }
   
   /**
    * @param row
    * @param dataRowMap
    * @param key
    * @return
    * @pre
    * @post
    */
   public static Object getValueFromDataRow(XComponent row, XComponent dataRowMapDataSet, String key) {
      String[][] dataRowMap = transformMappingDataSet(dataRowMapDataSet);
      return getValueFromDataRow(row, dataRowMap, key);
   }
   
   public static Object getValueFromDataRow(XComponent row,
         String[][] dataRowMap, String key) {
      String[] fieldMapping = findMappingInfo(dataRowMap, key);
      if (fieldMapping != null) {
         return readDataFieldValue(row, fieldMapping);
      }
      return null;
   }

   /**
    * @param row
    * @param dataRowMap
    * @param key
    * @pre
    * @post
    */
   private static String[] findMappingInfo(String[][] dataRowMap,
         String key) {
      for (int i = 0; i < dataRowMap.length; i++) {
         String[] fieldMapping = dataRowMap[i];
         String mappingInfo = fieldMapping[ELEMENT_PATH];
         String dataKey = choiceID(mappingInfo);

         if (key.equals(dataKey)) {
            return fieldMapping;
         }
      }
      return null;
   }

   /**
    * reads a datarow from a dataset into objects provided in objects
    * @param row                             the row to read from
    * @param dataRowMapDataSet               the mapping to use
    * @param objects                         objects to be filled
    * @throws OpProjectInputFormatException  if the format does not match the specified regex
    */
   public static FormatError readDataRowIntoMap(XComponent row, XComponent dataRowMapDataSet, 
         Map/*<String, Object>*/ objects) {
      String[][] dataRowMap = transformMappingDataSet(dataRowMapDataSet);
      return readDataRowIntoMap(row, dataRowMap, objects);
   }
   
   public static FormatError readDataRowIntoMap(XComponent row, String[][] dataRowMap, 
         Map/*<String, Object>*/ objects) {
      
      for (int i = 0; i < dataRowMap.length; i++) {
         FormatError e = null;
         String[] fieldMapping = dataRowMap[i];

         Object value = readDataFieldValue(row, fieldMapping);
         String dataRowValue = fieldMapping[ELEMENT_PATH];
         String choiceIdFieldName = choiceID(dataRowValue);
         String captionFieldName = choiceCaption(dataRowValue);
         String iconIndexFieldName = choiceIconIndexField(dataRowValue);
         if (captionFieldName != null || iconIndexFieldName != null) {
            String pattern = fieldMapping.length > ELEMENT_PATTERN ? fieldMapping[ELEMENT_PATTERN] : "";
            String label = fieldMapping.length > ELEMENT_LABEL ? fieldMapping[ELEMENT_LABEL] : null;
            String[] choiceFieldNameInfo = new String[] { choiceIdFieldName, pattern, null, null, label };
            e = addValueToMap(objects, choiceFieldNameInfo, XValidator.choiceCaption((String)value));
            if (e != null) {
               return e;
            }
            if (captionFieldName != null) {
               String[] captionFieldNameInfo = new String[] { captionFieldName };
               e = addValueToMap(objects, captionFieldNameInfo, XValidator.choiceCaption((String)value));
               if (e != null) {
                  return e;
               }
            }
            if (iconIndexFieldName != null) {
               String[] iconIndexFieldNameInfo = new String[] { iconIndexFieldName };
               Integer iconIntValue;
               boolean valueSet = false;
               try {
                  if (value instanceof Integer) {
                     iconIntValue = (Integer)value;
                  }
                  else {
                     String iconValue = XValidator.choiceIconIndexField((String)value);
                     iconIntValue = new Integer(iconValue);
                  }
                  e = addValueToMap(objects, iconIndexFieldNameInfo, iconIntValue);
               } catch (NumberFormatException exc) {}
               if (!valueSet){
                  e = addValueToMap(objects, iconIndexFieldNameInfo, XValidator.choiceIconIndexField((String)value));
               }
               if (e != null) {
                  return e;
               }
            }
         }
         else {
            e = addValueToMap(objects, fieldMapping, value);
            if (e != null) {
               return e;
            }
         }
      }
      return null;
   }

   /**
    * @param objects
    * @param fieldMapping
    * @param value
    * @return 
    * @pre
    * @post
    */
   private static FormatError addValueToMap(Map objects, String[] fieldMapping,
         Object value) {
      if (fieldMapping.length > ELEMENT_PATTERN) {
         String pattern = fieldMapping[ELEMENT_PATTERN];
         if (pattern != null && pattern.length() > 0) {
            value = value != null ? value : ""; 
            String stringValue = value.toString();
            Matcher mv = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL | Pattern.UNICODE_CASE).matcher(stringValue);
            if (!mv.matches()) {
               if (fieldMapping.length > ELEMENT_LABEL && fieldMapping[ELEMENT_LABEL] != null)
                  return new FormatError(fieldMapping[ELEMENT_LABEL], stringValue);
                  
               int cellIdx = Integer.parseInt(fieldMapping[ELEMENT_INDEX]);
               return new FormatError(new Integer(cellIdx), stringValue);
            }
         }
      }
      if (fieldMapping.length <= ELEMENT_PATH) {
         return null;
      }
      String mappingInfo = fieldMapping[ELEMENT_PATH];
      if (mappingInfo == null) {
         return null;
      }

      // honour (?) choices:
      String idFieldName = choiceID(mappingInfo);
      Matcher m = PATH_PATTERN.matcher(idFieldName);
      if (m.matches()) {
         String mapEntryName = m.group(NAME_GROUP);
         String elementPath = m.group(ELEMENT_GROUP);
         if (elementPath != null && elementPath.length() > 0) {
            elementPath = elementPath.substring(1);
         }
         
         if (elementPath == null || elementPath.length() == 0) {
            objects.put(mapEntryName, value);
         }
         else {
            Object object = objects.get(mapEntryName);
            if (object == null) {
               // FIXME: this code exists twice...
               object = new HashMap/*<String, Object>*/();
               objects.put(mapEntryName, object);
            }
            try {
               setElementInObject(object, elementPath, value);
            } catch (NoSuchMethodException e) {
               logger.warn("could not set Element: " + mappingInfo + " - " + e.getMessage());
            }
         }
      }
      return null;
   }

   /**
    * @param row
    * @param objects
    * @param fieldMapping
    * @pre
    * @post
    */
   private static Object readDataFieldValue(XComponent row, String[] fieldMapping) {
      boolean doReadValue = true;
      if (fieldMapping.length > ELEMENT_VISIBILITY && fieldMapping[ELEMENT_VISIBILITY] != null) {
         Matcher mv = DIRECTION_PATTERN.matcher(fieldMapping[ELEMENT_VISIBILITY]);
         if (mv.matches()) {
            if (DIRECTION_OUT.equals(mv.group(DIRECTION_GROUP))) {
               doReadValue = false;
            }
         }
      }
      if (!doReadValue) {
         // Always read value (often used for transfer only)
         // return null;
      }
      if (fieldMapping.length <= ELEMENT_INDEX) {
         return null;
      }
      int cellIdx = Integer.parseInt(fieldMapping[ELEMENT_INDEX]);
      Object value = null;
      switch (cellIdx) {
      case ROW_VALUE_INDEX:
         value = row.getValue();
         break;
      case ROW_OUTLINE_LEVEL_INDEX:
         value = new Integer(row.getOutlineLevel());
         break;
      case ROW_EXPANDED_INDEX:
         value = new Boolean(row.getExpanded());
         break;
      default:
         value = ((XComponent)row.getChild(cellIdx)).getValue();
      }

      return value;
   }
   
   private static String[] GETTER_PREFIXES = {"get", "is", null};
   
   public static void setElementInObject(Object object, String path,
         Object value) throws NoSuchMethodException {
      String[] st = path.split("\\.");

      for (int i = 0; i < st.length - 1; i++) {
         boolean success = false;
         if (object instanceof Map) {
            Map objectMap = (Map) object;
            Object tmp = objectMap.get(st[i]);
            // 
            if (tmp == null) {
               tmp = new HashMap();
               objectMap.put(st[i], tmp);
            }
            object = tmp;
            success = true;
         }
         else {
            String token = st[i];
            for (int k = 0; k < GETTER_PREFIXES.length; k++) {
               String methodName = "";
               if (GETTER_PREFIXES[k] == null) {
                  methodName = token;
               }
               else {
                  methodName = GETTER_PREFIXES[k]
                     + token.replaceFirst(token.substring(0, 1),
                           token.substring(0, 1).toUpperCase());
               }
               try {
                  Method method = object.getClass().getDeclaredMethod(methodName, new Class[0]);
                  method.setAccessible(true); // allow colling of private methods
                  object = method.invoke(object, new Object[0]);
                  success = true;
                  break;
               } catch (Exception e) {
               }
            }
         }
         if (!success) {
            throw new NoSuchMethodException("Getter not found: " + st[i]);
         }
      }
      if (st.length > 0) {
         if (object instanceof Map) {
            Map objectMap = (Map) object;
            objectMap.put(st[st.length - 1], value);
         }
         else {
            String methodName = st[st.length - 1];
            methodName = "set"
                  + methodName.replaceFirst(methodName.substring(0, 1),
                        methodName.substring(0, 1).toUpperCase());
            if (value != null) {
               Class c = value.getClass();
               try {
                  try {
                     Class[] parameterTypes = {c};
                     Method method = findMethod(object.getClass(), methodName, parameterTypes);
                     if (method != null) {
                        method.setAccessible(true);
                        Object[] parameters = {value};
                        method.invoke(object, parameters);
                        return;
                     }
                  } catch (NoSuchMethodException e) {
                     Class primitive = getPrimitive(c);
                     if (c != primitive) {
                        Class[] parameterTypes = {primitive};
                        Method method = findMethod(object.getClass(), methodName, parameterTypes);
                        if (method != null) {
                           method.setAccessible(true);
                           Object[] parameters = {value};
                           method.invoke(object, parameters);
                           return;
                        }
                     }
                     throw new NoSuchMethodException("Setter not found: " + methodName);
                  }
               }
               catch (Exception exc) {
                  throw new NoSuchMethodException("Setter not found: " + methodName);
               }
            }
            else {
               // TODO: check!
            }
         }
      }
   }
   
   private static Class getPrimitive(Class c) {
       if (c ==Boolean.class) {
          return Boolean.TYPE;
       }
       if (c == Character.class) {
          return Character.TYPE;
       }
       if (c == Byte.class) {
          return Byte.TYPE;
       }
       if (c == Short.class) {
          return Short.TYPE;
       }
       if (c == Integer.class) {
          return Integer.TYPE;
       }
       if (c == Long.class) {
          return Long.TYPE;
       }
       if (c == Float.class) {
          return Float.TYPE;
       }
       if (c == Double.class) {
          return Double.TYPE;
       }
       return c;
   }

   // copied from XReflectionProxy, improvements done there should be copied
   // here and vice versa... -> ALT: move to express4j utils...
   private static Method findMethod(Class c, String methodName,
         Class[] argumentTypes) throws NoSuchMethodException {
      try {
         return c.getDeclaredMethod(methodName, argumentTypes);
      } catch (NoSuchMethodException exc) {
      }
      Method match;
      // try my superclass
      Class superClass = c.getSuperclass();
      if (superClass != null) {
         match = findMethod(superClass, methodName, argumentTypes);
         if (match != null) {
            return match;
         }
      }
      throw new NoSuchMethodException("No method " + methodName
            + " defined in class " + c.getName());
   }

   public static Object getElementFromObject(Object object, String path)
         throws NoSuchMethodException {

      Object result = object;
      StringTokenizer st = new StringTokenizer(path, ".");

      while (st.hasMoreTokens() && result != null) {
         String token = st.nextToken();
         boolean success = false;
         if (result instanceof Map) {
            Map resultMap = (Map) result;
            result = resultMap.get(token);
            success = true;
         }
         else {
            for (int i = 0; i < GETTER_PREFIXES.length; i++) {
               String methodName = "";
               if (GETTER_PREFIXES[i] == null) {
                  methodName = token;
               }
               else {
                  methodName = GETTER_PREFIXES[i]
                     + token.replaceFirst(token.substring(0, 1),
                           token.substring(0, 1).toUpperCase());
               }
               try {
                  Method method = result.getClass().getMethod(methodName, new Class[0]);
                  result = method.invoke(result, new Object[0]);
                  success = true;
                  break;
               } catch (Exception e) {
               }
            }
         }
         if (!success) {
            throw new NoSuchMethodException("Getter not found: " + token);
         }
      }
      return result;
   }
   
   
   public static void setError(XComponent form, String errorTextId) {
      XComponent errorLabel = form.findComponent(form.getErrorLabel());
      errorLabel.setText(form.findComponent(errorTextId).getText());
      errorLabel.setVisible(true);
   }
   
   public static void insertParameterValues(XComponent form, HashMap parameters) {
      if (parameters == null) {
         return;
      }
      Iterator pit = parameters.keySet().iterator();
      while (pit.hasNext()) {
         Object k = pit.next();
         if (k instanceof String) {
            String key = (String) k;
            XComponent component = form.findComponent(key);
            if (component != null) {
               component.setValue(parameters.get(key));
            }
         }
      }
   }
   
   static public String getDialogString(Map/*<String, Object>*/ dialogContent, String elementName) {
      return (String)getDialogObject(dialogContent, elementName);
   }
   
   static public Double getDialogDouble(Map/*<String, Object>*/ dialogContent, String elementName) {
      return (Double)getDialogObject(dialogContent, elementName);
   }
   
   static public Integer getDialogInteger(Map/*<String, Object>*/ dialogContent, String elementName) {
      return (Integer)getDialogObject(dialogContent, elementName);
   }
   
   static public Boolean getDialogBoolean(Map/*<String, Object>*/ dialogContent, String elementName) {
      return (Boolean)getDialogObject(dialogContent, elementName);
   }
   
   static public Object getDialogObject(Map/*<String, Object>*/ dialogContent, String elementName) {
      if (dialogContent == null) {
         return null;
      }
      return dialogContent.get(elementName);
   }

}
