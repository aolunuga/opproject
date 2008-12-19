/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.util;


/**
 * @author dfreis
 *
 */
public class Pair<First, Second> {
   private First first;
   private Second second;

   /**
    * 
    */
   public Pair(First first, Second second) {
      this.first = first;
      this.second = second;
   }

   public First getFirst() {
      return first;      
   }

   public Second getSecond() {
      return second;
   }

   public void setFirst(First first) {
      this.first = first;      
   }

   public void setSecond(Second second) {
      this.second = second;
   }

   public String toString() {
      StringBuffer b = new StringBuffer("(");
      b.append(first == null ? "<null>" : first.toString());
      b.append(",");
      b.append(second == null ? "<null>" : second.toString());
      b.append(")");
      return b.toString();      
   }
   
   @Override
	public boolean equals(Object obj) {
	   Pair<First, Second> other = (Pair<First, Second>)obj;
	   if (first == null) {
		   if (other.first != null) {
			   return false;
		   }
		   if (second == null) {
			   return other.second == null;
		   }
	   }
	   if (!first.equals(other.first)) {
		   return false;
	   }
	   if (second == null) {
		   return other.second == null;
	   }
	   return second.equals(other.second);
	}
}
