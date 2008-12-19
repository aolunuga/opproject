package onepoint.project.modules.discussion;

import java.util.HashSet;
import java.util.Set;

public class OpDiscussionThread extends OpDiscussionArticle {
   
   private OpDiscussionGroup group;
   Set<OpDiscussionReply> followUps;

   public OpDiscussionGroup getGroup() {
      return group;
   }

   public void setGroup(OpDiscussionGroup group) {
      this.group = group;
   }

   public Set<OpDiscussionReply> getFollowUps() {
      return followUps;
   }
   
   private void setFollowUps(Set<OpDiscussionReply> followUps) {
      this.followUps = followUps;
   }
   
   public void addFollowup(OpDiscussionReply reply) {
      if (getFollowUps() == null) {
         setFollowUps(new HashSet<OpDiscussionReply>());
      }
      if (getFollowUps().add(reply)) {
         reply.setThread(this);
      }
   }
   
   public void removeFollowup(OpDiscussionReply reply) {
      if (getFollowUps() == null) {
         return;
      }
      if (getFollowUps().remove(reply)) {
         reply.setThread(null);
      }
   }
   
}
