package onepoint.project.forms;

import onepoint.error.XRuntimeException;
import onepoint.project.validators.OpProjectValidator;

public class OpProjectInputFormatException extends XRuntimeException {

   private String fieldId = null;
   private String fieldLabelId = null;
   private Integer tableColumnIndex = null;
   private String invalidValue = null;

   public OpProjectInputFormatException(OpProjectValidator.FormatError error) {
      super(error.getFieldId());
      this.fieldId = error.getFieldId();
      this.fieldLabelId = error.getFieldLabelId();
      this.tableColumnIndex = error.getTableColumnIndex();
      this.invalidValue = error.getInvalidValue();
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
