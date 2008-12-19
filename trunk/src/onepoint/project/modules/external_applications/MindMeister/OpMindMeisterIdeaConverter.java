/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.external_applications.MindMeister;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import onepoint.express.XComponent;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.persistence.OpBroker;
import onepoint.persistence.OpTypeManager;
import onepoint.project.OpProjectSession;
import onepoint.project.modules.documents.OpContent;
import onepoint.project.modules.documents.OpContentManager;
import onepoint.project.modules.external_applications.MindMeister.generated.Rsp;
import onepoint.project.modules.external_applications.MindMeister.generated.Rsp.Ideas.Idea;
import onepoint.project.modules.external_applications.exceptions.OpExternalApplicationException;
import onepoint.project.modules.project.OpAttachmentDataSetFactory;
import onepoint.project.modules.project.OpProjectPlan;
import onepoint.project.modules.project.OpProjectPlanValidator;
import onepoint.project.modules.project.components.OpActivityLoopException;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.util.Pair;
import onepoint.resource.XLocale;
import onepoint.service.XSizeInputStream;

/**
 * Class that provides methods in order to transform from MsProject structure in OPProject structure
 *
 * @author : mihai.costin
 */
public class OpMindMeisterIdeaConverter {

   private static final XLog logger = XLogFactory.getLogger(OpMindMeisterIdeaConverter.class);

   private static final String MPP_FORMAT = "MPP";
   private static final String MPT_FORMAT = "MPT";
   private static final String MPX_FORMAT = "MPX";
   private static final String MSPDI_FORMAT = "XML";

   //utility class
   private OpMindMeisterIdeaConverter() {
   }

   /**
    * Fills the given data set with infor from the previously loaded ms project file.
    *
    * @param fileName
    * @return a dataset with all the saved activities
    */
   public static XComponent importActivities(OpProjectSession session, OpBroker broker, Rsp.Ideas ideas, OpProjectPlan projectPlan, XLocale xlocale, OpMindMeisterConnection con)
        throws IOException {

      //create validator for the data-set validation
      XComponent dataSet = new XComponent(XComponent.DATA_SET);
      OpGanttValidator validator = OpProjectPlanValidator.getInstance().createValidator(session, broker, projectPlan);
      validator.setDataSet(dataSet);

      //populate the activity set
      Map<Integer, List<Rsp.Ideas.Idea.Attachments.Attachment>> rowToAttachmenstMap = new HashMap<Integer, List<Rsp.Ideas.Idea.Attachments.Attachment>>();
      populateActivitySet(broker, ideas, validator, dataSet, con, rowToAttachmenstMap);

      //validation after import
      if (validator.detectLoops()) {
         throw new OpActivityLoopException(OpGanttValidator.LOOP_EXCEPTION);
      }
      validator.validateEntireDataSet();
      return dataSet;
   }

   private static class MMWood {
      public static class Node {
         String id = null;
         Rsp.Ideas.Idea idea = null;
         SortedMap<String, Node> children = new TreeMap<String, Node>(new Comparator<String>() {
            public int compare(String o1, String o2) {
               return o1.compareTo(o2);
            }});
         
         public Node(String id, Rsp.Ideas.Idea idea) {
            this.id = id;
            this.idea = idea;
         }
         
         public String getId() {
            return id;
         }
         
         public Rsp.Ideas.Idea getIdea() {
            return idea;
         }
         
         public void setIdea(Rsp.Ideas.Idea idea) {
            this.idea = idea;
         }

         public void addChild(Node node) {
            children.put(node.getId(), node);
         }

         public SortedMap<String, Node> getChildren() {
            return children;
         }
      }
      
      private Map<String, Node> allNodes = new HashMap<String, Node>();
      private Map<String, Node> rootNodes = new HashMap<String, Node>();
      
      public MMWood() {
      }
      
      public void addIdea(Rsp.Ideas.Idea idea) {
         String id = idea.getId().toString();
         Node n = allNodes.get(id);
         if (n != null) {
            n.setIdea(idea);
            rootNodes.remove(id);
         }
         else {
            n = new Node(id, idea);
         }
         if (idea.getParent() != null && BigInteger.ZERO.compareTo(idea.getParent()) < 0) {
            String pid = idea.getParent().toString();
            Node parent = allNodes.get(pid);
            if (parent == null) {
               parent = new Node(pid, null);
               allNodes.put(pid, parent);
            }
            parent.addChild(n);
         }
         else {
            rootNodes.put(id, n);
         }
         allNodes.put(id, n);
      }
      
      public Map<String, Node> getRootNodes() {
         return rootNodes;
      }
      
   }
   
   private static void depthFirstIdeas(Map<String, MMWood.Node> nodes, int depth, List<Pair<Integer, Rsp.Ideas.Idea>> ideas) {
      Iterator<String> nit = nodes.keySet().iterator();
      while (nit.hasNext()) {
         String key = nit.next();
         MMWood.Node n = nodes.get(key);
         ideas.add(new Pair<Integer, Idea>(new Integer(depth), n.getIdea()));
         depthFirstIdeas(n.getChildren(), depth + 1, ideas);
      }
   }
   
   private static void populateActivitySet(OpBroker broker, Rsp.Ideas ideas,
        OpGanttValidator validator, XComponent dataSet, OpMindMeisterConnection con, Map<Integer, List<Rsp.Ideas.Idea.Attachments.Attachment>> rowToAttachmenstMap) {
      MMWood wood = new MMWood();
      for (Rsp.Ideas.Idea idea : ideas.getIdea()) {
         wood.addIdea(idea);
      }

      Map<String, MMWood.Node> nodes = null;
      if (wood.getRootNodes().size() == 1) {
         nodes = wood.getRootNodes().get(wood.getRootNodes().keySet().iterator().next()).getChildren();
      }
      else {
         nodes = wood.getRootNodes();
      }
      List<Pair<Integer, Rsp.Ideas.Idea>> depthFirstIdeas = new ArrayList<Pair<Integer, Rsp.Ideas.Idea>>();
      depthFirstIdeas(nodes, 0, depthFirstIdeas);
      
      Map<String, String> contents = new HashMap<String, String>();
      
      for (Pair<Integer, Rsp.Ideas.Idea> leveledIdea : depthFirstIdeas) {
         XComponent activityRow = validator.newDataRow();

         OpGanttValidator.setName(activityRow, leveledIdea.getSecond().getTitle());
         // TODO: feasible???
         validator.updateType(activityRow, OpGanttValidator.STANDARD);
         //category = null
         if (!validator.isProgressTracked()) {
            OpGanttValidator.setComplete(activityRow, leveledIdea.getSecond().isClosed() ? 100d : 0d);
         }
         else {
            OpGanttValidator.setComplete(activityRow, 0);
         }

         String description = leveledIdea.getSecond().getNote();
         if (description.length() > OpTypeManager.MAX_TEXT_LENGTH) {
            description = description.substring(0, OpTypeManager.MAX_TEXT_LENGTH - 1);
         }
         OpGanttValidator.setDescription(activityRow, description);

         activityRow.setOutlineLevel(leveledIdea.getFirst().intValue());

         // Attachments:
         if (leveledIdea.getSecond().getAttachments() != null
               && leveledIdea.getSecond().getAttachments().getAttachment() != null
               && leveledIdea.getSecond().getAttachments().getAttachment()
                     .size() > 0) {
            // do we have an attachment here?!?
            List<List> attachments = new ArrayList<List>();
            Iterator<Rsp.Ideas.Idea.Attachments.Attachment> ait = leveledIdea.getSecond().getAttachments().getAttachment().iterator();
            while (ait.hasNext()) {
               Rsp.Ideas.Idea.Attachments.Attachment a = ait.next();
               try {
                  InputStream is = con.loadAttachment(a.getId());
                  
                  XSizeInputStream stream = new XSizeInputStream(is, a.getSize().longValue());
                  String mimeType = a.getContenttype();
                  OpContent content = OpContentManager.newContent(stream, mimeType, 0);

                  broker.makePersistent(content);

                  Object[] attachmentParameters = {OpAttachmentDataSetFactory.ATTACHMENT_DOCUMENT, "",  a.getFilename(), a.getFilename(), content.locator()};
                  attachments.add(Arrays.asList(attachmentParameters));

               } catch (OpExternalApplicationException e) {
                  logger.error("Cannot load attachment " + a.getUrl() + " for Idea " + leveledIdea.getSecond().getTitle() + " Msg: " + e.getMessage());
               }
            }
            if (!attachments.isEmpty()) {
               OpGanttValidator.setAttachments(activityRow, attachments);
            }
         }
         
         //add to data set
         dataSet.addChild(activityRow);
      }
   }

}
