/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */

package onepoint.project.modules.backup;

import java.lang.reflect.Method;

/**
 * Class that represents the backup of a member of a prototype.
 */
public class OpBackupMember {

   /**
    * 
    */
   public OpBackupMember() {
      // TODO Auto-generated constructor stub
   }
   /**
    * The name of the member.
    */
   String name;

   /**
    * The id of the type
    */
   int typeId;

   /**
    * The method that accesses the member.
    */
   Method accessor;

   /**
    * Flag indicating whether the member is a relationship or not.
    */
   boolean relationship;

   /**
    * Flag indicating whether the member is ordered or not.
    */
   boolean ordered;

   /**
    * Flag indicating whether the member is recursive.
    */
   boolean recursive;

   /**
    * The name of the back-relationship (applies only to relationships)
    */
   String backRelationshipName = null;

   public String convertMethod = null;

   public boolean wasDeleted = false;

   public String typeString = null;

//   @Override
//	public int hashCode() {
//	   return name.hashCode();
//	}
//   
//   	@Override
//   	public boolean equals(Object obj) {
//   		OpBackupMember other = (OpBackupMember)obj;
//   		return ()
//   	}
}
