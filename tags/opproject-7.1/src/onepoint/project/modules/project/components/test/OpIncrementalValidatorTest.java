/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.project.components.test;

import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project.components.OpIncrementalValidator;

/**
 * @author mihai.costin
 */
public class OpIncrementalValidatorTest extends OpGanttValidatorTest {

   protected OpGanttValidator getValidator() {
      return new OpIncrementalValidator();
   }

}
