use core::fmt;

/// Result of every decoder and encoder.
pub type Result<T> = core::result::Result<T, Error>;

/// Why a block or a value could not be decoded or encoded.
#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Error {
	/// A field is outside the range its bits or the spec allow.
	Range {
		/// Which field.
		name: &'static str,
		/// The value given.
		value: u32,
		/// Smallest allowed value.
		min: u32,
		/// Largest allowed value.
		max: u32,
	},
	/// Byte 15 is not the XOR of bytes 0 to 14.
	Checksum {
		/// XOR of bytes 0 to 14.
		expected: u8,
		/// Byte 15.
		got: u8,
	},
	/// Bytes the spec keeps at zero are set.
	NonZero(&'static str),
	/// Bytes 4 to 7 of a value block are not the complement of bytes 0 to 3.
	BalanceComplement,
	/// Bytes 8 to 11 of a value block differ from bytes 0 to 3.
	BalanceCopy,
	/// Blocks 8 and 9 of a dump differ.
	BalanceBlocksDiffer,
	/// The id is not two capital letters and an even count of 6 to 26 digits.
	IdFormat,
	/// Bytes 0 to 2 of block 1 are no known product.
	UnknownCardType(u32),
	/// The first byte of block 1 matches no known product.
	UnknownCardTypeByte(u8),
	/// A direction byte is neither 1 nor 2.
	Direction(u8),
	/// Byte 8 of a transaction is neither a direction nor a top up.
	TransactionKind(u8),
	/// Block 0 carries neither a 1K nor a 4K SAK.
	Sak,
	/// The dump is not whole blocks or ends before block 33.
	DumpSize {
		/// Bytes up to and including block 33.
		minimum: usize,
		/// Bytes given.
		got: usize,
	},
}

impl fmt::Display for Error {
	fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
		match self {
			Self::Range {
				name,
				value,
				min,
				max,
			} => write!(f, "{name} must be in {min}..={max}, got {value}"),
			Self::Checksum { expected, got } => {
				write!(f, "checksum {got} does not match {expected}")
			}
			Self::NonZero(bytes) => write!(f, "{bytes} must be zero"),
			Self::BalanceComplement => f.write_str("balance complement does not match"),
			Self::BalanceCopy => f.write_str("balance copy does not match"),
			Self::BalanceBlocksDiffer => f.write_str("balance blocks 8 and 9 differ"),
			Self::IdFormat => {
				f.write_str("id must be two capital letters and an even count of 6 to 26 digits")
			}
			Self::UnknownCardType(value) => write!(f, "unknown card type {value:x}"),
			Self::UnknownCardTypeByte(byte) => write!(f, "unknown card type byte {byte}"),
			Self::Direction(byte) => write!(f, "direction must be 1 or 2, got {byte}"),
			Self::TransactionKind(byte) => {
				write!(f, "transaction kind byte must be 1, 2 or 8, got {byte}")
			}
			Self::Sak => f.write_str("block 0 carries neither a 1K nor a 4K SAK"),
			Self::DumpSize { minimum, got } => write!(
				f,
				"dump must be whole blocks and at least {minimum} bytes, got {got}"
			),
		}
	}
}

impl core::error::Error for Error {}
