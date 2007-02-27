/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.persistence;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class OpPrototype extends OpType {

   private String _super_type_name;
   private OpPrototype _super_type; // Resolved on-register
   private ArrayList subTypes;
   private ArrayList _declared_members;
   private Hashtable _declared_member_names;
   private ArrayList _members; // Resolved on-register
   private Hashtable _member_names; // Resolved on-register
   private int _size; // Calculated on-register

   /**
    * List of prototypes this prototypes depends on for backup.
    */
   private List backupDependencies = null;

   public OpPrototype() {
      subTypes = new ArrayList();
      _declared_members = new ArrayList();
      _declared_member_names = new Hashtable();
      _members = new ArrayList();
      _member_names = new Hashtable();
   }

   public final void setSuperTypeName(String super_type_name) {
      _super_type_name = super_type_name;
   }

   public final OpPrototype getSuperType() {
      return _super_type;
   }

   public final Iterator subTypes() {
      return subTypes.iterator();
   }

   public final int getSize() {
      return _size;
   }

   public void addDeclaredMember(OpMember member) {
      // *** Check if name is set and unique
      _declared_member_names.put(member.getName(), member);
      _declared_members.add(member);
   }

   public final int getDeclaredSize() {
      // Returns the number of members in this prototype
      return _declared_members.size();
   }

   public final Iterator getDeclaredMembers() {
      return _declared_members.iterator();
   }

   public final OpMember getDeclaredMember(String name) {
      return (OpMember) (_declared_member_names.get(name));
   }

   public final Iterator getMembers() {
      return _members.iterator();
   }

   public final OpMember getMember(int id) {
      return (OpMember) (_members.get(id));
   }

   public final OpMember getMember(String name) {
      return (OpMember) (_member_names.get(name));
      /*
       // Resolves members in prototype hierarchy
       OpMember member = null;
       OpPrototype prototype = this;
       while (prototype != null) {
           member = (OpMember)(prototype._declared_member_names.get(name));
           if (member != null)
         return member;
           prototype = prototype._super_type;
       }
       return null;
       */
   }

   // On-register callback

   public void onRegister() {
      super.onRegister();
      _size = _declared_members.size();
      /*
       if (_super_type_name == null) {
         // Implicit inheritance from OpObject (field "ID")
         _size += 1;
         _members.add(ID_MEMBER);
       }
       */
      // else {
      // Resolve super-type
      if (_super_type_name != null) {
         OpType super_type = OpTypeManager.getType(_super_type_name);
         if (!(super_type instanceof OpPrototype)) {
            ; // ERROR handling
         }
         _super_type = (OpPrototype) super_type;
         _super_type.subTypes.add(this);
         // Add size of super-type to total size
         _size += _super_type._size;
         // First add resolved members of super-type to members (presume order)
         Iterator super_members = _super_type.getMembers();
         while (super_members.hasNext()) {
            OpMember member = (OpMember) (super_members.next());
            _member_names.put(member.getName(), member);
            _members.add(member);
         }
      }
      // }
      // Add declared members of this prototype
      for (int i = 0; i < _declared_members.size(); i++) {
         OpMember member = (OpMember) (_declared_members.get(i));
         _member_names.put(member.getName(), member);
         _members.add(member);
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
