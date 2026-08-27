package dev.hloth.zgztransport;

import java.util.Objects;
import java.util.Optional;

/**
 * The two keys of one sector. A key is absent when no dump has revealed it yet.
 *
 * @param a
 *            key A, which the operator uses to read
 * @param b
 *            key B, which the operator uses to write
 */
public record SectorKeys(Optional<Key> a, Optional<Key> b) {

	/**
	 * Checks that both fields are present.
	 *
	 * @throws NullPointerException
	 *             if a field is null
	 */
	public SectorKeys {
		Objects.requireNonNull(a, "a");
		Objects.requireNonNull(b, "b");
	}

	static SectorKeys of(String a, String b) {
		return new SectorKeys(Optional.ofNullable(a).map(Key::of), Optional.ofNullable(b).map(Key::of));
	}
}
