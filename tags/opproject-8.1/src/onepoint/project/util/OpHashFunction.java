package onepoint.project.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is the base class for all hash functions
 *
 * Using the class is quite simple, and is performed as a sequence
 * of certain steps:
 *
 * 1. initialize the hash function using init()
 * 2. feed data into the function with an appropriate update method
 * 3. finish the algorithm with a call to finish()
 * 4. get the hash value using either getHash() or getHashString() wether you
 *    want a byte array or a string containing the hash in hex format
 *
 * There is also a helper method to ease the use of hash functions:
 * You can use calculateHash to perform all the necessary steps for you,
 * as input you can choose either a string or an InputStream.
 */
public abstract class OpHashFunction {

   public OpHashFunction() {}

   public String calculateHash(InputStream s) {
      byte[] b = new byte[512];
      int read = 0;
      try {

         // reset algorithm
         init();

         do {
            read = s.read(b, 0, 512);
            update(b, 0, read);
         } while (read > 0);

         finish();
         return getHashString();
      } catch (IOException e) {
         return null;
      }
   }

   public String calculateHash(String data) {
      init();
      update(data);
      finish();
      return getHashString();
   }

   /**
    * Initializes the algorithm, this method must be called everytime a new hash value has to be computed.
    */
   public abstract void init();

   /**
    * Feed the hash value algorithm with more data.
    * @param data the data to be used for the hash value computation
    * @param offset start index within the array
    * @param len the number of array elements to be used
    */
   public abstract void update(byte[] data, int offset, int len);

   /**
    * Feed the hash value algorithm with more data.
    * @param data the data to be used for the hash value computation
    */
   public abstract void update(byte[] data);

   /**
    * Feed the hash value algorithm with more data.
    * @param data the data to be used for the hash value computation
    */
   public abstract void update(byte data);

   public abstract void update(String data);

   /**
    * Finishes the computation of the hash if no more data is available.
    */
   public abstract void finish();

   /**
    * After the algorithm has finished, this method returns the hash value as a byte array.
    * Be aware of the fact, that bytes in java are signed, so you have to use a proper unsigned conversion.
    *
    * @return the hash value as byte array
    */
   public abstract byte[] getHash();

   /**
    * After the algorithm has finished, this method returns the hash value as hex string
    *
    * @return the hash value in string format
    */
   public abstract String getHashString();

   /**
    * After the algorithm has finished, this method returns the hash value as hex string
    *
    * @param space Wether a space should be inserted after 4 byte (1 word) of the hash value), which increases
    * readability of the hash if it is presented to human beings.
    * @return the hash value in string format
    */
   public abstract String getHashString(boolean space);

}
