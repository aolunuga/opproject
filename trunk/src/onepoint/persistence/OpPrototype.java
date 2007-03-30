/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class OpPrototype extends OpType {

   private String superTypeName;
   private OpPrototype superType; // Resolved on-register
   private ArrayList subTypes;
   private ArrayList declaredMembers;
   private Hashtable declaredMemberNames;
   private ArrayList members; // Resolved on-register
   private Hashtable memberNames; // Resolved on-register
   private int size; // Calculated on-register

   /**
    * List of prototypes this prototypes depends on for backup.
    */
   private List backupDependencies = null;

   public OpPrototype() {
      subTypes = new ArrayList();
      declaredMembers = new ArrayList();
      declaredMemberNames = new Hashtable();
      members = new ArrayList();
      memberNames = new Hashtable();
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
      declaredMemberNames.put(member.getName(), member);
      declaredMembers.add(member);
   }

   public final int getDeclaredSize() {
      // Returns the number of members in this prototype
      return declaredMembers.size();
   }

   public final Iterator getDeclaredMembers() {
      return declaredMembers.iterator();
   }

   public final OpMember getDeclaredMember(String name) {
      return (OpMember) (declaredMemberNames.get(name));
   }

   public final Iterator getMembers() {
      return members.iterator();
   }

   public final OpMember getMember(int id) {
      return (OpMember) (members.get(id));
   }

   public final OpMember getMember(String name) {
      return (OpMember) (memberNames.get(name));
   }

   // On-register callback

   public void onRegister() {
      super.onRegister();
      size = declaredMembers.size();
      // Resolve super-type
      if (superTypeName != null) {
         OpType super_type = OpTypeManager.getType(superTypeName);
         if (!(super_type instanceof OpPrototype)) {
            ; // ERROR handling
         }
         superType = (OpPrototype) super_type;
         superType.subTypes.add(this);
         // Add size of super-type to total size
         size += superType.size;
         // First add resolved members of super-type to members (presume order)
         Iterator super_members = superType.getMembers();
         while (super_members.hasNext()) {
            OpMember member = (OpMember) (super_members.next());
            memberNames.put(member.getName(), member);
            members.add(member);
         }
      }
      // }
      // Add declared members of this prototype
      for (int i = 0; i < declaredMembers.size(); i++) {
         OpMember member = (OpMember) (declaredMembers.get(i));
         memberNames.put(member.getName(), member);
         members.add(member);
      }
   }

   /**
    * Returns a list with this prototype's dependencies, in terms of the order in which the backup has to be done.
    * @return a <code>List</code> of <code>XProptotype</code> representing the dependent prototypes.
    */
   public List getBackupDependencies() {
      if (backupDependencies == null) {
         backupDependencies = new ArrayList();
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
}
