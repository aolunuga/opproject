package onepoint.project.modules.discussion;

import onepoint.persistence.OpObject;
import onepoint.project.modules.user.OpUser;

public class OpDiscussionReadArticleLink extends OpObject {
   
   private Integer type;

   private OpUser user;
   private OpDiscussionArticle article;
   
   public Integer getType() {
      return type;
   }
   public void setType(Integer type) {
      this.type = type;
   }
   public OpUser getUser() {
      return user;
   }
   public void setUser(OpUser user) {
      this.user = user;
   }
   public OpDiscussionArticle getArticle() {
      return article;
   }
   public void setArticle(OpDiscussionArticle article) {
      this.article = article;
   }

}
