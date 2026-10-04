package dev.hloth.zgztransport;

/**
 * A MIFARE Classic variant one of these cards is built on, which the SAK and
 * the ATQA of block 0 tell apart. The UID length is a property of block 0
 * rather than of the chip, see {@link Uid#decode(byte[])}.
 */
public enum Chip {
	/** MIFARE Classic 1K, which the Avanza cards use. */
	CLASSIC_1K(64, 16, 0x88, 0x04_00),
	/** MIFARE Classic 4K, which the Lazo card uses. */
	CLASSIC_4K(256, 40, 0x18, 0x02_00);

	private final int blocks;
	private final int sectors;
	private final int sak;
	private final int atqa;

	Chip(int blocks, int sectors, int sak, int atqa) {
		this.blocks = blocks;
		this.sectors = sectors;
		this.sak = sak;
		this.atqa = atqa;
	}

	/**
	 * Blocks a card with this chip has.
	 *
	 * @return the block count
	 */
	public int blocks() {
		return blocks;
	}

	/**
	 * Sectors a card with this chip has.
	 *
	 * @return the sector count
	 */
	public int sectors() {
		return sectors;
	}

	boolean answersAt(byte[] blockZero, int sakOffset) {
		return Bytes.u8(blockZero[sakOffset]) == sak && Bytes.u16(blockZero, sakOffset + 1) == atqa;
	}
}
