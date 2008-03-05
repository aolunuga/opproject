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
}
