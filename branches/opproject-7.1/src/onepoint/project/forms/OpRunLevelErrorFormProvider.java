package onepoint.project.forms;

import onepoint.express.XComponent;
import onepoint.project.OpProjectSession;
import onepoint.project.util.OpProjectConstants;
import onepoint.resource.XLocaleManager;
import onepoint.resource.XLocalizer;
import onepoint.service.server.XSession;

import java.util.HashMap;

/**
 * Form provider for the run level error message form.
 *
 * @author mihai.costin
 */
public class OpRunLevelErrorFormProvider extends OpErrorFormProvider {

   /**
    * Checks the run level found in the parameters, and if necessary displays a message to the user.
    *
    * @param parameters a <code>HashMap</code> of <code>String,Object</code> pairs, representing form parameters.
    * @param localeId   a <code>String</code> representing the id of the current locale.
    * @param mapId      The error resource map ID.
    * @return A string representing an error message, if any. Null otherwise.
    */
   public static String getErrorResourceFromRunLevel(HashMap parameters, String localeId, String mapId) {
      String runLevelParameter = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
      if (runLevelParameter == null) {
         return null;
      }
      XLocalizer localizer = XLocaleManager.createLocalizer(localeId, mapId);

      int runLevel = Integer.valueOf(runLevelParameter);
      if (runLevel < OpProjectConstants.SUCCESS_RUN_LEVEL) {
         String resourceId = "${" + OpProjectConstants.RUN_LEVEL + runLevelParameter + "}";
         return localizer.localize(resourceId);
      }
      
      return null;
   }


   public void prepareForm(XSession s, XComponent form, HashMap parameters) {
      String localeId = ((OpProjectSession) s).getLocale().getID();
      String errorText = getErrorResourceFromRunLevel(parameters, localeId, "main.levels");
      parameters.put(TEXT_MESSAGE, errorText);
      super.prepareForm(s, form, parameters);
   }


}
