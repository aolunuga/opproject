package onepoint.project.modules.discussion;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

public abstract class OpDiscussionArticle extends OpObject {

   private OpUser user;
   private String title;
   private String text;

   private Set<OpDiscussionReply> replies;
   private Set<OpDiscussionReadArticleLink> readers;
   
   public OpUser getUser() {
      return user;
   }
   public void setUser(OpUser user) {
      this.user = user;
   }
   public String getTitle() {
      return title;
   }
   public void setTitle(String title) {
      this.title = title;
   }
   public String getText() {
      return text;
   }
   public void setText(String text) {
      this.text = text;
   }
   public Set<OpDiscussionReply> getReplies() {
      return replies;
   }
   private void setReplies(Set<OpDiscussionReply> replies) {
      this.replies = replies;
   }
   
   public void addReply(OpDiscussionReply reply) {
      if (getReplies() == null) {
         setReplies(new HashSet<OpDiscussionReply>());
      }
      if (getReplies().add(reply)) {
         reply.setArticle(this);
      }
   }
   
   public void removeReply(OpDiscussionReply reply) {
      if (getReplies() == null) {
         return;
      }
      if (getReplies().remove(reply)) {
         reply.setArticle(null);
      }
   }
   
   public Set<OpDiscussionReadArticleLink> getReaders() {
      return readers;
   }
   private void setReaders(Set<OpDiscussionReadArticleLink> readers) {
      this.readers = readers;
   }
   
   public void addReader(OpDiscussionReadArticleLink reader) {
      if (getReaders() == null) {
         setReaders(new HashSet<OpDiscussionReadArticleLink>());
      }
      if (getReaders().add(reader)) {
         reader.setArticle(this);
      }
   }
   
   public void removeReader(OpDiscussionReadArticleLink reader) {
      if (getReaders() == null) {
         return;
      }
      if (getReaders().remove(reader)) {
         reader.setArticle(null);
      }
   }
   
}
