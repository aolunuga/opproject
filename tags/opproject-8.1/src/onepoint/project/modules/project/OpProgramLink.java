package onepoint.project.modules.project;

import onepoint.persistence.OpObject;

public class OpProgramLink extends OpObject {
   
   private OpProjectNode subProject = null;
   private OpProjectNode program = null;
 
   private OpProgramLink() {
   }
   
   public OpProgramLink(OpProjectNode program, OpProjectNode subProject) {
      program.addSubProjectLink(this);
      subProject.addProgramLink(this);
   }
   
   public OpProjectNode getSubProject() {
      return subProject;
   }
   public void setSubProject(OpProjectNode subProject) {
      this.subProject = subProject;
   }
   public OpProjectNode getProgram() {
      return program;
   }
   public void setProgram(OpProjectNode program) {
      this.program = program;
   }
}
