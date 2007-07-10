/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.*;

public class OpPrototype extends OpType {

   private String superTypeName;
   private OpPrototype superType; // Resolved on-register
   private Set<OpPrototype> subTypes;
   private Map<String, OpMember> declaredMembers;
   private Map<String, OpMember> members; // Resolved on-register
   private int size; // Calculated on-register

   /**
    * List of prototypes this prototypes depends on for backup.
    */
   private List<OpPrototype> backupDependencies = null;

   public OpPrototype() {
      subTypes = new HashSet<OpPrototype>();
      declaredMembers = new LinkedHashMap<String, OpMember>();
      members = new LinkedHashMap<String, OpMember>();
   }

   public final void setSuperTypeName(String super_type_name) {
      superTypeName = super_type_name;
   }

   public final OpPrototype getSuperType() {
      return superType;
   }

   public final Iterator subTypes() {
      return subTypes.iterator();
   }

   public final int getSize() {
      return size;
   }

   public void addDeclaredMember(OpMember member) {
      // *** Check if name is set and unique
      declaredMembers.put(member.getName(), member);
   }

   public final int getDeclaredSize() {
      // Returns the number of members in this prototype
      return declaredMembers.size();
   }

   public final Iterator<OpMember> getDeclaredMembers() {
      return declaredMembers.values().iterator();
   }

   public final OpMember getDeclaredMember(String name) {
      return (OpMember) declaredMembers.get(name);
   }

   public final Iterator<OpMember> getMembers() {
      return members.values().iterator();
   }

   public final OpMember getMember(String name) {
      return (OpMember) members.get(name);
   }

   // On-register callback
   public void onRegister() {
      super.onRegister();
      size = declaredMembers.size();
      // Resolve super-type
      if (superTypeName != null) {
         OpType super_type = OpTypeManager.getType(superTypeName);
         if (!(super_type instanceof OpPrototype)) {
            ; // TODO - ERROR handling
         }
         superType = (OpPrototype) super_type;
         superType.subTypes.add(this);

         // Add size of super-type to total size
         size += superType.size;

         // First add resolved members of super-type to members (presume order)
         Iterator super_members = superType.getMembers();
         while (super_members.hasNext()) {
            OpMember member = (OpMember) (super_members.next());
            members.put(member.getName(), member);
         }
      }

      // Add declared members of this prototype
      for (OpMember declaredMember : declaredMembers.values()) {
         members.put(declaredMember.getName(), declaredMember);
      }
   }

   /**
    * Returns a list with this prototype's dependencies, in terms of the order in which the backup has to be done.
    *
    * @return a <code>List</code> of <code>XProptotype</code> representing the dependent prototypes.
    */
   public List getBackupDependencies() {
      if (backupDependencies == null) {
         backupDependencies = new ArrayList<OpPrototype>();

         Iterator it = this.getMembers();
         while (it.hasNext()) {
            OpMember member = (OpMember) it.next();
            if (member instanceof OpRelationship) {
               OpRelationship relationship = (OpRelationship) member;
               if (!relationship.getInverse() && !relationship.getRecursive()) {
                  OpPrototype dependentType = OpTypeManager.getPrototype(relationship.getTypeName());
                  backupDependencies.add(dependentType);
               }
            }
         }
      }

      return backupDependencies;
   }

   /**
    * Extends this prototype with the information from the parent prototype (extension means
    * copying all non-conflicting members.
    * @param parentPrototype a <code>OpPrototype</code> representing the parent
    * prototype.
    */
   public void extend(OpPrototype parentPrototype) {
      Iterator<OpMember> parentMemebersIt = parentPrototype.getDeclaredMembers();
      while (parentMemebersIt.hasNext()) {
         OpMember parentMember = parentMemebersIt.next();
         if (this.declaredMembers.get(parentMember.getName()) == null) {
            this.declaredMembers.put(parentMember.getName(), parentMember);
         }
      }
   }
}
