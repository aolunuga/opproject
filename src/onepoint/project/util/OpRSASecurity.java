/*
 * Copyright(c) OnePoint Software GmbH 2006. All Rights Reserved.
 */

package onepoint.project.util;

import gnu.crypto.key.rsa.GnuRSAPrivateKey;
import gnu.crypto.key.rsa.GnuRSAPublicKey;
import gnu.crypto.sig.rsa.RSA;

import java.math.BigInteger;

/**
 * Class that takes care of security issues using RSA.
 *
 * @author : mihai.costin
 */
public final class OpRSASecurity {

   /**
    * Radix used to represent the signature.
    */
   private static final int REPRESENTATION_RADIX = 16;

   /**
    * Utility class.
    */
   private OpRSASecurity(){
   }

   /**
    * Signs a given message with the given private key (creates a message digest using SHA)
    *
    * @param privateKeyString String representation of the private key
    * @param message          Text message to be signed.
    * @return String representation of the encrypted signature [in hex]
    */
   public static String sign(String privateKeyString, String message) {
      GnuRSAPrivateKey privateKey = OpRSASecurity.privateKeyFromString(privateKeyString);

      //make a message digest (hash is in hex representation)
      OpHashFunction sha1 = new OpSHA1();
      String hash = sha1.calculateHash(message);
      BigInteger numberHash = new BigInteger(hash, 16);

      //encrypt the digest
      BigInteger encrypted = RSA.sign(privateKey, numberHash);
      return encrypted.toString(REPRESENTATION_RADIX);
   }

   /**
    * Verifies a given message against the given signature
    *
    * @param publicKeyString the String representation of the public key to be used for decryption
    * @param message         message to be checked
    * @param signature       signature to check against [radix 16]
    * @return true if signature is valid
    */
   public static boolean verify(String publicKeyString, String message, String signature) {
      GnuRSAPublicKey publicKey = OpRSASecurity.publicKeyFromString(publicKeyString);

      //make a message digest (hash - hex form)
      OpHashFunction sha1 = new OpSHA1();
      String hash = sha1.calculateHash(message);
      BigInteger currentHash = new BigInteger(hash, 16);

      //decrypt the given signature (obtain the hash of the original message)
      BigInteger decrypted = RSA.verify(publicKey, new BigInteger(signature, REPRESENTATION_RADIX));

      //compare the original hash with the given message hash
      return currentHash.equals(decrypted);
   }

   /**
    * Transforms a public key object into its string representatin.
    *
    * @param publicKey public key object
    * @return string representation for the given key
    */
   public static String publicKeyToString(GnuRSAPublicKey publicKey) {
      BigInteger publicExponent = publicKey.getPublicExponent();
      BigInteger publicModulus = publicKey.getModulus();
      return publicModulus + "#" + publicExponent;
   }

   /**
    * Transforms a string representatin of a public key into a key object.
    *
    * @param publicKeyString String representation of the public key
    * @return A key object
    * @throws IllegalArgumentException if the given string is not a valid public key representation.
    */
   public static GnuRSAPublicKey publicKeyFromString(String publicKeyString) {
      String[] parts = publicKeyString.split("#");
      if (parts.length != 2) {
         throw new IllegalArgumentException("String does not represent a valid private key");
      }
      BigInteger modulo;
      BigInteger exp;
      try {
         modulo = new BigInteger(parts[0]);
         exp = new BigInteger(parts[1]);
      }
      catch (NumberFormatException e) {
         throw new IllegalArgumentException("String does not represent a valid private key");
      }
      return new GnuRSAPublicKey(modulo, exp);
   }

   /**
    * Transforms a private key object into its string representatin.
    *
    * @param privateKey Private key object
    * @return String representation for the given key
    */
   public static String privateKeyToString(GnuRSAPrivateKey privateKey) {
      BigInteger privatePrimeP = privateKey.getPrimeP();
      BigInteger privatePrimeQ = privateKey.getPrimeQ();
      BigInteger publicExponent = privateKey.getPublicExponent();
      BigInteger privateExponent = privateKey.getPrivateExponent();
      return privatePrimeP + "#" + privatePrimeQ + "#" + publicExponent + "#" + privateExponent;
   }

   /**
    * Transforms a string representatin of a private key into a key object.
    *
    * @param privateKeyString String representation of the private key
    * @return A key object
    * @throws IllegalArgumentException if the given string is not a valid private key representation.
    */
   public static GnuRSAPrivateKey privateKeyFromString(String privateKeyString) {
      String[] parts = privateKeyString.split("#");
      if (parts.length != 4) {
         throw new IllegalArgumentException("String does not represent a valid private key");
      }
      BigInteger primeP;
      BigInteger primeQ;
      BigInteger publicExponent;
      BigInteger privateExp;
      try {
         primeP = new BigInteger(parts[0]);
         primeQ = new BigInteger(parts[1]);
         publicExponent = new BigInteger(parts[2]);
         privateExp = new BigInteger(parts[3]);
      }
      catch (NumberFormatException e) {
         throw new IllegalArgumentException("String does not represent a valid private key");
      }
      return new GnuRSAPrivateKey(primeP, primeQ, publicExponent, privateExp);
   }

}
