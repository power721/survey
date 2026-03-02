package cn.har01d.survey.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GravatarUtil {

    private static final String GRAVATAR_URL = "https://www.gravatar.com/avatar/";

    public static String getAvatarUrl(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String hash = md5Hex(email.trim().toLowerCase());
        return GRAVATAR_URL + hash + "?d=identicon&s=80";
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
