package network.p2p.Fixtures;

import java.util.Random;

/**
 * Byte Fixture class that creates random byte objects to be used in testing.
 */
public class Bytes {
  private static final Random random = new Random();

  /**
   * Generates a random byte array.
   *
   * @param length length of byte array.
   * @return random byte array.
   */
  public static byte[] byteArrayFixture(int length) {
    byte[] arr = new byte[length];
    random.nextBytes(arr);
    return arr;
  }

  /**
   * Returns a byte fixture. Note that in Java an 8-bit byte represents
   * a value between -128 (10000000) to 127 (01111111).
   *
   * @return a randomly generated byte.
   */
  public static byte byteFixture() {
    StringBuilder byteString = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      if (random.nextBoolean()) {
        byteString.append("1");
      } else {
        byteString.append("0");
      }
    }
    return Byte.parseByte(byteString.toString(), 2);
  }
}