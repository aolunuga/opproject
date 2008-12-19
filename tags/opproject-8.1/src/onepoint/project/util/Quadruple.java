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
public class Quadruple<First, Second, Third, Fourth> {
   private First first;
   private Second second;
   private Third third;
   private Fourth fourth;

   /**
    * 
    */
   public Quadruple(First first, Second second, Third third, Fourth fourth) {
      this.first = first;
      this.second = second;
      this.third = third;
      this.fourth = fourth;
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

   public Fourth getFourth() {
      return fourth;
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

   public void setFourth(Fourth fourth) {
      this.fourth = fourth;
   }
   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      Quadruple<?,?,?,?> other = (Quadruple<?,?,?,?>) obj;
      return (first.equals(other.first) && second.equals(other.second) && third.equals(other.third) && fourth.equals(other.fourth));
   }
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return first.hashCode()+second.hashCode()+third.hashCode()+fourth.hashCode();
   }
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      // TODO Auto-generated method stub
      return "Triple["+","+first+","+second+","+third+fourth+"]";
   }
}
