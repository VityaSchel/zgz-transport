package dev.hloth.zgztransport;

import java.util.Objects;

/**
 * What block 10 held when {@link Card#decode(Dump)} read a card whose product
 * maintains it. The whole card is worth reading even when the block is not, so
 * a block that does not decode is kept as {@link Unknown} instead of failing
 * the dump; {@link JourneySummary#decode(byte[])} still throws.
 */
public sealed interface JourneySummarySlot {

	/**
	 * Block 10 read as the specification describes it.
	 *
	 * @param summary
	 *            the summary
	 */
	record Read(JourneySummary summary) implements JourneySummarySlot {
		/**
		 * Checks that the summary is present.
		 *
		 * @throws NullPointerException
		 *             if the summary is null
		 */
		public Read {
			Objects.requireNonNull(summary, "summary");
		}
	}

	/**
	 * Block 10 is not all zero and does not decode, so the card writes a layout
	 * this version does not read.
	 *
	 * @param error
	 *            what stopped it from decoding
	 */
	record Unknown(CardFormatException error) implements JourneySummarySlot {
		/**
		 * Checks that the error is present.
		 *
		 * @throws NullPointerException
		 *             if the error is null
		 */
		public Unknown {
			Objects.requireNonNull(error, "error");
		}
	}
}
