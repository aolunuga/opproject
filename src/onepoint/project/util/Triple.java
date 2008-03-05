/*
 * Copyright(c) OnePoint Software GmbH 2007. All Rights Reserved.
 */ 

/**
 * 
 */
package onepoint.project.util;

import java.util.Map;

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

}
