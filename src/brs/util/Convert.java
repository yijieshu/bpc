package brs.util;

import brs.BurstException;
import brs.Constants;
import brs.crypto.Crypto;
import burst.kit.crypto.BurstCrypto;
import burst.kit.entity.BurstAddress;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class Convert {

  private static final BurstCrypto burstCrypto = BurstCrypto.getInstance();

  private static final long[] multipliers = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

  public static final BigInteger two64 = BigInteger.valueOf(2).pow(64);

  private Convert() {} //never

  public static byte[] parseHexString(String hex) {
    if (hex == null) return null;
    try {
      if (hex.length() % 2 != 0) {
        hex = hex.substring(0, hex.length() - 1);
      }
      return Hex.decode(hex);
    } catch (DecoderException e) {
      throw new RuntimeException("Could not parse hex string " + hex, e);
    }
  }

  public static String toHexString(byte[] bytes) {
    if (bytes == null) return null;
    return Hex.toHexString(bytes);
  }

  public static String toUnsignedLong(long objectId) {
    return Long.toUnsignedString(objectId);
  }

  public static long parseUnsignedLong(String number) {
    if (number == null) {
      return 0;
    }
    return Long.parseUnsignedLong(number);
  }

  public static long parseAccountId(String account) {
    BurstAddress address = BurstAddress.fromEither(account);
    return address == null ? 0 : address.getBurstID().getSignedLongId();
  }

  public static String rsAccount(long accountId) {
    return "BPC-" + Crypto.rsEncode(accountId);
  }

  public static long fullHashToId(byte[] hash) {
    return burstCrypto.hashToId(hash).getSignedLongId();
  }

  public static long fullHashToId(String hash) {
    if (hash == null) {
      return 0;
    }
    return fullHashToId(parseHexString(hash));
  }

  public static Date fromEpochTime(int epochTime) {
    return new Date(epochTime * 1000L + Constants.EPOCH_BEGINNING - 500L);
  }

  public static String emptyToNull(String s) {
    return s == null || s.isEmpty() ? null : s;
  }

  public static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  public static long nullToZero(Long l) {
    return l == null ? 0 : l;
  }

  public static byte[] emptyToNull(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    for (byte b : bytes) {
      if (b != 0) {
        return bytes;
      }
    }
    return null;
  }

  public static byte[] toBytes(String s) {
    return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
  }

  public static String toString(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static String readString(ByteBuffer buffer, int numBytes, int maxLength) throws BurstException.NotValidException {
    if (numBytes > 3 * maxLength) {
      throw new BurstException.NotValidException("Max parameter length exceeded");
    }
    byte[] bytes = new byte[numBytes];
    buffer.get(bytes);
    return Convert.toString(bytes);
  }

  public static String truncate(String s, String replaceNull, int limit, boolean dots) {
    return s == null ? replaceNull : s.length() > limit ? (s.substring(0, dots ? limit - 3 : limit) + (dots ? "..." : "")) : s;
  }

  public static long parseNXT(String nxt) {
    return parseStringFraction(nxt, 8, Constants.MAX_BALANCE_BURST);
  }

  private static long parseStringFraction(String value, int decimals, long maxValue) {
    String[] s = value.trim().split("\\.");
    if (s.length == 0 || s.length > 2) {
      throw new NumberFormatException("Invalid number: " + value);
    }
    long wholePart = Long.parseLong(s[0]);
    if (wholePart > maxValue) {
      throw new IllegalArgumentException("Whole part of value exceeds maximum possible");
    }
    if (s.length == 1) {
      return wholePart * multipliers[decimals];
    }
    long fractionalPart = Long.parseLong(s[1]);
    if (fractionalPart >= multipliers[decimals] || s[1].length() > decimals) {
      throw new IllegalArgumentException("Fractional part exceeds maximum allowed divisibility");
    }
    for (int i = s[1].length(); i < decimals; i++) {
      fractionalPart *= 10;
    }
    return wholePart * multipliers[decimals] + fractionalPart;
  }

  // overflow checking based on https://www.securecoding.cert.org/confluence/display/java/NUM00-J.+Detect+or+prevent+integer+overflow
  public static long safeAdd(long left, long right)
    throws ArithmeticException {
    if (right > 0 ? left > Long.MAX_VALUE - right
        : left < Long.MIN_VALUE - right) {
      throw new ArithmeticException("Integer overflow");
    }
    return left + right;
  }

  public static long safeSubtract(long left, long right)
    throws ArithmeticException {
    if (right > 0 ? left < Long.MIN_VALUE + right
        : left > Long.MAX_VALUE + right) {
      throw new ArithmeticException("Integer overflow");
    }
    return left - right;
  }

  public static long safeMultiply(long left, long right)
    throws ArithmeticException {
    if (right > 0 ? left > Long.MAX_VALUE/right
        || left < Long.MIN_VALUE/right
        : (right < -1 ? left > Long.MIN_VALUE/right
           || left < Long.MAX_VALUE/right
           : right == -1
           && left == Long.MIN_VALUE) ) {
      throw new ArithmeticException("Integer overflow");
    }
    return left * right;
  }

  public static long safeDivide(long left, long right)
    throws ArithmeticException {
    if ((left == Long.MIN_VALUE) && (right == -1)) {
      throw new ArithmeticException("Integer overflow");
    }
    return left / right;
  }

  public static long safeNegate(long a) throws ArithmeticException {
    if (a == Long.MIN_VALUE) {
      throw new ArithmeticException("Integer overflow");
    }
    return -a;
  }

  public static long safeAbs(long a) throws ArithmeticException {
    if (a == Long.MIN_VALUE) {
      throw new ArithmeticException("Integer overflow");
    }
    return Math.abs(a);
  }

}
