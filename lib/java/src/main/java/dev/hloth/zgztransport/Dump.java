package dev.hloth.zgztransport;

import java.util.Arrays;

/**
 * The blocks of a card, one after another, as MIFARE Classic Tool or a Proxmark
 * writes them.
 *
 * <p>
 * It hides the index arithmetic a raw byte array needs, so a caller can reach
 * for one block without slicing:
 *
 * <pre>{@code
 * Dump dump = Dump.of(Files.readAllBytes(Path.of("dump.bin")));
 * Transaction newest = Transaction.decode(dump.block(TransactionLog.LIVE_BLOCK));
 * }</pre>
 */
public final class Dump {

	/** Bytes one block takes. */
	public static final int BLOCK_SIZE = Bytes.BLOCK_SIZE;

	private final byte[] bytes;

	private Dump(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * The dump those bytes hold.
	 *
	 * @param bytes
	 *            whole blocks, which are copied
	 * @return the dump
	 * @throws CardFormatException
	 *             if the length is not a multiple of the block size
	 */
	public static Dump of(byte[] bytes) {
		if (bytes.length == 0 || bytes.length % BLOCK_SIZE != 0) {
			throw new CardFormatException("dump must be whole blocks of " + BLOCK_SIZE + " bytes, got " + bytes.length);
		}
		return new Dump(bytes.clone());
	}

	/**
	 * An empty dump of the size that chip has, to be filled block by block.
	 *
	 * @param chip
	 *            the chip whose layout the dump follows
	 * @return a builder over a dump of {@link Chip#blocks()} empty blocks
	 */
	public static Builder builder(Chip chip) {
		return new Builder(chip.blocks());
	}

	/**
	 * One block of this dump.
	 *
	 * @param index
	 *            the block number, counting from zero
	 * @return a copy of its sixteen bytes
	 * @throws CardFormatException
	 *             if the dump stops before that block
	 */
	public byte[] block(int index) {
		if (index < 0 || index >= blockCount()) {
			throw new CardFormatException("block " + index + " is outside the " + blockCount() + " blocks of the dump");
		}
		return Arrays.copyOfRange(bytes, index * BLOCK_SIZE, (index + 1) * BLOCK_SIZE);
	}

	/**
	 * How many blocks this dump holds.
	 *
	 * @return the block count
	 */
	public int blockCount() {
		return bytes.length / BLOCK_SIZE;
	}

	/**
	 * The bytes of this dump.
	 *
	 * @return a copy of them
	 */
	public byte[] bytes() {
		return bytes.clone();
	}

	/**
	 * Reads the card this dump holds.
	 *
	 * @return the card
	 * @throws CardFormatException
	 *             if the dump stops before the last block a card uses or a block
	 *             does not decode
	 */
	public Card card() {
		return Card.decode(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Dump dump && Arrays.equals(bytes, dump.bytes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public String toString() {
		return "Dump[" + blockCount() + " blocks]";
	}

	/**
	 * Fills the blocks of a dump one at a time, leaving the untouched ones zeroed.
	 */
	public static final class Builder {

		private final byte[] bytes;

		private Builder(int blocks) {
			this.bytes = new byte[blocks * BLOCK_SIZE];
		}

		/**
		 * Writes a structure into a block.
		 *
		 * @param index
		 *            the block number
		 * @param content
		 *            the structure, which has to encode to a whole block
		 * @return this builder
		 * @throws CardFormatException
		 *             if the block is outside the dump or the structure is not sixteen
		 *             bytes long
		 */
		public Builder block(int index, Encodable content) {
			return block(index, content.encode());
		}

		/**
		 * Writes raw bytes into a block.
		 *
		 * @param index
		 *            the block number
		 * @param content
		 *            the sixteen bytes
		 * @return this builder
		 * @throws CardFormatException
		 *             if the block is outside the dump or the bytes are not sixteen
		 *             long
		 */
		public Builder block(int index, byte[] content) {
			Bytes.block(content, "block " + index);
			if (index < 0 || index * BLOCK_SIZE >= bytes.length) {
				throw new CardFormatException("block " + index + " is outside the dump");
			}
			System.arraycopy(content, 0, bytes, index * BLOCK_SIZE, BLOCK_SIZE);
			return this;
		}

		/**
		 * Builds the dump.
		 *
		 * @return the dump
		 */
		public Dump build() {
			return new Dump(bytes.clone());
		}
	}
}
