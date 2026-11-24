package com.lanchat.database;

import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "p2$120000$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + derive(password, salt, 120000);
  }

  private static String derive(String p, byte[] salt, int n) {
    try {
      PBEKeySpec spec = new PBEKeySpec(p.toCharArray(), salt, n, 256);
      try {
        return Base64.getEncoder()
            .encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded());
      } finally {
        spec.clearPassword();
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean verify(String p, String stored) {
    if (stored == null) return false;
    if (!stored.startsWith("p2$"))
      return MessageDigest.isEqual(
          p.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    try {
      String[] a = stored.split("\\$");
      return MessageDigest.isEqual(
          derive(p, Base64.getDecoder().decode(a[2]), Integer.parseInt(a[1]))
              .getBytes(java.nio.charset.StandardCharsets.UTF_8),
          a[3].getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } catch (Exception e) {
      return false;
    }
  }
}
