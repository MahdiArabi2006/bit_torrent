package common.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash {
    public static String HashFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            return "hash-error";
        }
    }
}
