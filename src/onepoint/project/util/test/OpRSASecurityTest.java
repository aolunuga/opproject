/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util.test;

import junit.framework.TestCase;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.util.OpRSASecurity;

/**
 * Test Class for the security implemented usin RSA keys.
 *
 * @author : mihai.costin
 */
public class OpRSASecurityTest extends TestCase {
   private static XLog logger = XLogFactory.getLogger(OpRSASecurityTest.class);

   //string representation of the rsa keys as given by the generator tool
   private String publicKeyString = "116238789113458587215212688017563464591464179143939145689261027347724562289824836899520779190012456394523521549475239340476449277819313343109099805604424660613207490514594673755792395718738298348251524586258683588380365543382224782894410610524513430125076743856000434396817041063418509218852913104423392740361#65537";
   private String privateKeyString = "12710377663624661541841595663675845517878914204794551857034267289603075712537032763526045195115020757699567456891378389100884318569105518986465525187407931#9145187671812292725239982406412068275367158758869161124990576640658229249943246824847773051174774077581937258524941681687551697621109036811810953475259531#65537#15325989543759947085259515039744966927610997939832127773637861624909409078022741590990723606220877301449223335047614983002838064141426775681000525202982030051799530982893491524968887617314716093015013579073155732037144472167319583699203182131907996392779902592084682564065556387283266529077183411329026543173";


   protected void setUp() throws Exception {
      super.setUp();
   }

   protected void tearDown() throws Exception {
      super.tearDown();
   }

   public void testSign() {

      String message =
           "INFO   onepoint.project.module.OpModuleRegistryLoader  -  Loading registered modules...\n" +
                "INFO   onepoint.project.module.OpModuleRegistryLoader  -  ...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading module: onepoint/project/modules/user/module.oxm.xml\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading prototypes of module 'user'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Prototypes of module 'user' loaded.\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading services of module 'user'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Services of module 'user' loaded.\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading language-kits of module 'user'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Language-kits of module 'user' loaded.\n" +
                "INFO   onepoint.project.module.OpModuleRegistryLoader  -  ...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading module: onepoint/project/modules/resource/module.oxm.xml\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading prototypes of module 'resource'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Prototypes of module 'resource' loaded.\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading services of module 'resource'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Services of module 'resource' loaded.\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Loading language-kits of module 'resource'...\n" +
                "INFO   onepoint.project.module.OpModuleLoader  -  Language-kits of module ";

      String signature = OpRSASecurity.sign(privateKeyString, message);
      logger.info("Signature for message = " + signature);

      //check against original message
      boolean validSignature = OpRSASecurity.verify(publicKeyString, message, signature);
      assertTrue("Original message was not changed. Signature should be valid", validSignature);

      //change original message
      String message2 = message + "just some minor change";
      validSignature = OpRSASecurity.verify(publicKeyString, message2, signature);
      assertFalse("Original message WAS changed. Signature should NOT be valid", validSignature);

      //change original message
      message2 = message.replaceFirst("a", "b");
      validSignature = OpRSASecurity.verify(publicKeyString, message2, signature);
      assertFalse("Original message WAS changed. Signature should NOT be valid", validSignature);

      //change the public key
      String damagedKey = publicKeyString.replaceFirst("0","1");
      validSignature = OpRSASecurity.verify(damagedKey, message, signature);
      assertFalse("Public key has been damaged. Signature should NOT be valid", validSignature);

   }
}
