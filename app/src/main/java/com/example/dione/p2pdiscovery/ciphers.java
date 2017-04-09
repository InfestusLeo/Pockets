package com.example.dione.p2pdiscovery;
import java.security.*;
import javax.crypto.*;

public class ciphers {

    KeyPairGenerator kpg;
    KeyPair kp;
    PublicKey publicKey;
    PrivateKey privateKey;
    byte [] encryptedBytes,decryptedBytes;
    Cipher cipher,cipher1;
    String encrypted,decrypted;

    public byte[] RSAEncrypt(final String plain) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();

        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        encryptedBytes = cipher.doFinal(plain.getBytes());
        //System.out.println(new String(encryptedBytes));
        return encryptedBytes;
    }

    public String RSADecrypt(final byte[] encryptedBytes) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        cipher1 = Cipher.getInstance("RSA");
        cipher1.init(Cipher.DECRYPT_MODE, privateKey);
        decryptedBytes = cipher1.doFinal(encryptedBytes);
        decrypted = new String(decryptedBytes);
        //System.out.println("DDecrypted?????" + decrypted);
        return decrypted;
    }

    /*public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        RSA r= new RSA();
        byte[] encrypted = r.RSAEncrypt("hi");
        System.out.println(new String(encrypted));
        String plain = r.RSADecrypt(encrypted);
        System.out.println(plain);
        System.out.println(r.publicKey);
        System.out.print(r.privateKey);
    }*/
}