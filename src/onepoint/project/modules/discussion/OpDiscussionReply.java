package onepoint.project.modules.discussion;

public class OpDiscussionReply extends OpDiscussionArticle {
   
   private OpDiscussionArticle article;
   private OpDiscussionThread thread;

   public OpDiscussionArticle getArticle() {
      return article;
   }

   public void setArticle(OpDiscussionArticle article) {
      this.article = article;
   }

   public OpDiscussionThread getThread() {
      return thread;
   }

   public void setThread(OpDiscussionThread thread) {
      this.thread = thread;
   }

}
