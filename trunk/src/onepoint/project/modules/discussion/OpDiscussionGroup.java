package onepoint.project.modules.discussion;

import java.util.HashSet;
import java.util.Set;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.OpProjectNode;

public class OpDiscussionGroup extends OpObject {
   
   private String name;
   private String description;
   private Set<OpDiscussionThread> threads;
   
   private OpProjectNode project;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public Set<OpDiscussionThread> getThreads() {
      return threads;
   }

   private void setThreads(Set<OpDiscussionThread> threads) {
      this.threads = threads;
   }

   public void addThread(OpDiscussionThread thread) {
      if (getThreads() == null) {
         setThreads(new HashSet<OpDiscussionThread>());
      }
      if (getThreads().add(thread)) {
         thread.setGroup(this);
      }
   }
   
   public void removeThread(OpDiscussionThread thread) {
      if (getThreads() == null) {
         return;
      }
      if (getThreads().remove(thread)) {
         thread.setGroup(null);
      }
   }
   
   
   public OpProjectNode getProject() {
      return project;
   }

   public void setProject(OpProjectNode project) {
      this.project = project;
   }

}
