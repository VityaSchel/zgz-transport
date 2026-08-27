package dev.hloth.zgztransport;

import java.util.Objects;

/**
 * One subscription product of a personal card, which lives in a sector of its
 * own.
 *
 * @param sector
 *            the sector holding it, {@code 3} or {@code 4}
 * @param metadata
 *            block 12 or 16, which describes the product
 * @param subscription
 *            block 13 or 17, which holds its validity
 */
public record Product(int sector, SubscriptionMetadata metadata, Subscription subscription) {

	/**
	 * Checks that the sector is one a product lives in and that the blocks are
	 * present.
	 *
	 * @throws IllegalArgumentException
	 *             if the sector is outside {@code 3} to {@code 4}
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public Product {
		Objects.requireNonNull(metadata, "metadata");
		Objects.requireNonNull(subscription, "subscription");
		Bytes.checkRange("sector", sector, 3, 4);
	}
}
