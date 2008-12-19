package onepoint.project.modules.resource;

import onepoint.persistence.OpObject;
import onepoint.project.modules.project.components.OpGanttValidator;

import java.util.HashSet;
import java.util.Set;

public class OpAbsenceReason extends OpObject {

   private String name = null;
   private String description = null;
   
   private int type = 0;
   
   private Set<OpAbsence> absences = null;

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Set<OpAbsence> getAbsences() {
      return absences;
   }

   private void setAbsences(Set<OpAbsence> absences) {
      this.absences = absences;
   }
   
   public void addAbsence(OpAbsence absence) {
      if (getAbsences() == null) {
         setAbsences(new HashSet<OpAbsence>());
      }
      if (getAbsences().add(absence)) {
         absence.setReason(this);
      }
   }
   
   public void removeAbsence(OpAbsence absence) {
      if (getAbsences() == null) {
         return;
      }
      if (getAbsences().remove(absence)) {
         absence.setReason(null);
      }
   }

   public int getType() {
      return type;
   }

   public void setType(int type) {
      this.type = type;
   }

   // FIXME: MS_SQL-SERVER and default values...
   private void setType(Integer type) {
      this.type = type != null ? type.intValue() : OpGanttValidator.DEP_DEFAULT;
   }

   public boolean isInUse() {
      return getAbsences() != null && !getAbsences().isEmpty();
   }
   
}
