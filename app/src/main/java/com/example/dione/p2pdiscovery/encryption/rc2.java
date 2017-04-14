package com.example.dione.p2pdiscovery.encryption;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class rc2 {
    byte[] text;
    Cipher cipher;
    SecretKeySpec KS;

    public rc2() throws Exception{
        String Key = "pockets.xie.engg";
        byte[] KeyData = Key.getBytes();
        KS = new SecretKeySpec(KeyData, "Blowfish");
        cipher = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
    }

    public byte[] process(byte[] message, String mode) throws Exception{
        if(mode.equals("-e"))
            cipher.init(Cipher.ENCRYPT_MODE, KS);
        else if(mode.equals("-d"))
            cipher.init(Cipher.DECRYPT_MODE, KS);
        text = cipher.doFinal(message);
        return text;
    }
}
