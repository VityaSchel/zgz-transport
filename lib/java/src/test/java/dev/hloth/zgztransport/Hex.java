package dev.hloth.zgztransport;

final class Hex {

	private Hex() {
	}

	static byte[] bytes(String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}

	static String text(byte[] bytes) {
		StringBuilder hex = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			hex.append(String.format("%02X", value));
		}
		return hex.toString();
	}

	static byte[] checksummed(byte[] block) {
		byte checksum = 0;
		for (int i = 0; i < 15; i++) {
			checksum ^= block[i];
		}
		block[15] = checksum;
		return block;
	}
}
