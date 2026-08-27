package dev.hloth.zgztransport;

final class Bytes {

	static final int BLOCK_SIZE = 16;

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	private Bytes() {
	}

	static byte[] exact(byte[] bytes, int length, String name) {
		if (bytes.length != length) {
			throw new CardFormatException(name + " must be " + length + " bytes, got " + bytes.length);
		}
		return bytes;
	}

	static byte[] block(byte[] bytes, String name) {
		return exact(bytes, BLOCK_SIZE, name);
	}

	static int checkRange(String name, int value, int min, int max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(name + " must be in " + min + ".." + max + ", got " + value);
		}
		return value;
	}

	static long checkRange(String name, long value, long min, long max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(name + " must be in " + min + ".." + max + ", got " + value);
		}
		return value;
	}

	static int u8(byte value) {
		return value & 0xff;
	}

	static int u16(byte[] bytes, int offset) {
		return (u8(bytes[offset]) << 8) | u8(bytes[offset + 1]);
	}

	static long unsigned(byte[] bytes, int offset, int length) {
		long value = 0;
		for (int i = 0; i < length; i++) {
			value = (value << 8) | u8(bytes[offset + i]);
		}
		return value;
	}

	static byte[] twoBytes(int value) {
		return new byte[]{(byte) (value >>> 8), (byte) value};
	}

	static void write(byte[] bytes, int offset, long value, int length) {
		for (int i = 0; i < length; i++) {
			bytes[offset + i] = (byte) (value >>> (8 * (length - 1 - i)));
		}
	}

	static long u32le(byte[] bytes, int offset) {
		long value = 0;
		for (int i = 3; i >= 0; i--) {
			value = (value << 8) | u8(bytes[offset + i]);
		}
		return value;
	}

	static void writeU32le(byte[] bytes, int offset, long value) {
		for (int i = 0; i < 4; i++) {
			bytes[offset + i] = (byte) (value >>> (8 * i));
		}
	}

	static boolean isZero(byte[] bytes, int from, int to) {
		for (int i = from; i < to; i++) {
			if (bytes[i] != 0) {
				return false;
			}
		}
		return true;
	}

	static byte xor(byte[] bytes, int from, int to) {
		byte result = 0;
		for (int i = from; i < to; i++) {
			result ^= bytes[i];
		}
		return result;
	}

	static void checkChecksum(byte[] block) {
		byte expected = xor(block, 0, 15);
		if (block[15] != expected) {
			throw new CardFormatException(
					"checksum " + u8(block[15]) + " does not match " + u8(expected) + " over bytes 0 to 14");
		}
	}

	static byte[] withChecksum(byte[] block) {
		block[15] = xor(block, 0, 15);
		return block;
	}

	static String hex(byte[] bytes, int from, int to) {
		StringBuilder text = new StringBuilder((to - from) * 2);
		for (int i = from; i < to; i++) {
			text.append(HEX[(bytes[i] >> 4) & 0x0f]).append(HEX[bytes[i] & 0x0f]);
		}
		return text.toString();
	}

	static byte[] fromHex(String hex, String name) {
		if (hex.length() % 2 != 0) {
			throw new IllegalArgumentException(name + " must have an even number of hex digits");
		}
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			int high = Character.digit(hex.charAt(i * 2), 16);
			int low = Character.digit(hex.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				throw new IllegalArgumentException(name + " must be hex digits, got " + hex);
			}
			bytes[i] = (byte) ((high << 4) | low);
		}
		return bytes;
	}
}
