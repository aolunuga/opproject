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
public class Triple<First, Second, Third> {
   private First first;
   private Second second;
   private Third third;

   /**
    * 
    */
   public Triple(First first, Second second, Third third) {
      this.first = first;
      this.second = second;
      this.third = third;
   }

   public First getFirst() {
      return first;      
   }

   public Second getSecond() {
      return second;
   }

   public Third getThird() {
      return third;
   }

   /**
    * @param oldState
    * @pre
    * @post
    */
   public void setFirst(First first) {
      this.first = first;
   }
   /**
    * @param oldState
    * @pre
    * @post
    */
   public void setSecond(Second second) {
      this.second = second;
   }
   /**
    * @param oldState
    * @pre
    * @post
    */
   public void setThird(Third third) {
      this.third = third;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
	   if (this == obj) {
		   return true;
	   }
      Triple<?,?,?> other = (Triple<?,?,?>) obj;
      return (first.equals(other.first) && second.equals(other.second) && third.equals(other.third));
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
	   int hFirst = first == null ? 0 : first.hashCode();
       int hSecond = second == null ? 0 : second.hashCode();
       int hThird = third == null ? 0 : third.hashCode();
       return hFirst + 31 * hSecond + 31 * hThird;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      // TODO Auto-generated method stub
      return "Triple["+","+first+","+second+","+third+"]";
   }
}
