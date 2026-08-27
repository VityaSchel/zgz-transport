package dev.hloth.zgztransport;

import java.math.BigDecimal;

/**
 * The balance of blocks 8 and 9, in thousandths of a euro. Personal cards
 * always hold zero.
 *
 * @param units
 *            the balance in {@link #UNITS_PER_EURO} units
 */
public record Balance(long units) implements Encodable {

	/** Units in one euro. */
	public static final long UNITS_PER_EURO = 1000;

	/**
	 * Largest balance the value block holds. The terminals of the operator stay
	 * well below it; a card carrying more than a few tens of euros has been
	 * tampered with.
	 */
	public static final long MAX_UNITS = 0xffff_ffffL;

	private static final byte[] ADDRESS = {0x02, (byte) 0xfd, 0x02, (byte) 0xfd};

	/**
	 * Checks the range of the balance.
	 *
	 * @throws IllegalArgumentException
	 *             if the balance is negative or above {@link #MAX_UNITS}
	 */
	public Balance {
		Bytes.checkRange("balance", units, 0, MAX_UNITS);
	}

	/**
	 * Decodes the value block of block 8 or 9, checking the complement and the copy
	 * the MIFARE value block format holds.
	 *
	 * @param block
	 *            the sixteen bytes
	 * @return the balance
	 * @throws CardFormatException
	 *             if the block is not sixteen bytes, the complement or the copy do
	 *             not match, or the balance is above {@link #MAX_UNITS}
	 */
	public static Balance decode(byte[] block) {
		Bytes.block(block, "balance block");
		long units = Bytes.u32le(block, 0);
		if (Bytes.u32le(block, 4) != (~units & 0xffff_ffffL)) {
			throw new CardFormatException("balance complement does not match");
		}
		if (Bytes.u32le(block, 8) != units) {
			throw new CardFormatException("balance copy does not match");
		}
		return new Balance(units);
	}

	/**
	 * Encodes this balance into the value block of blocks 8 and 9.
	 *
	 * @return the sixteen bytes
	 */
	public byte[] encode() {
		byte[] block = new byte[Bytes.BLOCK_SIZE];
		Bytes.writeU32le(block, 0, units);
		Bytes.writeU32le(block, 4, ~units);
		Bytes.writeU32le(block, 8, units);
		System.arraycopy(ADDRESS, 0, block, 12, ADDRESS.length);
		return block;
	}

	/**
	 * The balance in euros, exact to the thousandth.
	 *
	 * @return the amount in euros
	 */
	public BigDecimal euros() {
		return BigDecimal.valueOf(units, 3);
	}

	/**
	 * The balance in euros with three decimals.
	 *
	 * @return the printed amount
	 */
	@Override
	public String toString() {
		return euros().toPlainString();
	}
}
