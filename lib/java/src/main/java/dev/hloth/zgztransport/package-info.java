/**
 * Decoders and encoders for the Zaragoza and Aragon transport cards, the Avanza
 * Tarjeta Bus and the Lazo card, following the card specification at <a href=
 * "https://git.hloth.dev/hloth/zgz-transport">git.hloth.dev/hloth/zgz-transport</a>.
 *
 * <p>
 * {@link dev.hloth.zgztransport.Card#decode(byte[])} reads a whole dump. Every
 * structure the card holds is a type of its own with a {@code decode} that
 * reads its bytes and an {@code encode} that writes them back; the types check
 * their fields when they are built, so encoding never fails, and decoding
 * throws {@link dev.hloth.zgztransport.CardFormatException} when the bytes do
 * not hold what the specification says.
 */
package dev.hloth.zgztransport;
