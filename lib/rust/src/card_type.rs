use crate::bytes::{BLOCK_SIZE, Block, check_checksum, is_zero, with_checksum};
use crate::error::{Error, Result};

/// Card types identified by bytes [0-2] of block 1.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum CardType {
	/// Balance top-up Avanza card, `02699F`.
	AvanzaTopUp,
	/// Personal expiring Avanza card, `0A9775`.
	AvanzaPersonalUnlimited,
	/// Balance top-up Lazo card, `0D371F`.
	LazoTopUp,
}

impl CardType {
	/// Every known card type.
	pub const ALL: [Self; 3] = [
		Self::AvanzaTopUp,
		Self::AvanzaPersonalUnlimited,
		Self::LazoTopUp,
	];

	/// Bytes 0-2 of block 1.
	#[must_use]
	pub const fn value(self) -> u32 {
		match self {
			Self::AvanzaTopUp => 0x02_69_9f,
			Self::AvanzaPersonalUnlimited => 0x0a_97_75,
			Self::LazoTopUp => 0x0d_37_1f,
		}
	}

	/// First byte of the card.
	#[must_use]
	pub const fn byte(self) -> u8 {
		self.value().to_be_bytes()[1]
	}

	/// Finds the product with the given bytes 0 to 2.
	///
	/// # Errors
	/// [`Error::UnknownCardType`](crate::Error::UnknownCardType).
	pub fn from_value(value: u32) -> Result<Self> {
		Self::ALL
			.into_iter()
			.find(|card_type| card_type.value() == value)
			.ok_or(Error::UnknownCardType(value))
	}

	/// Finds the product whose first byte is `byte`.
	///
	/// # Errors
	/// [`Error::UnknownCardTypeByte`](crate::Error::UnknownCardTypeByte).
	pub fn from_byte(byte: u8) -> Result<Self> {
		Self::ALL
			.into_iter()
			.find(|card_type| card_type.byte() == byte)
			.ok_or(Error::UnknownCardTypeByte(byte))
	}

	/// Decodes block 1.
	///
	/// # Errors
	/// [`Error::Checksum`](crate::Error::Checksum), [`Error::NonZero`](crate::Error::NonZero) or [`Error::UnknownCardType`](crate::Error::UnknownCardType).
	pub fn decode(block: &Block) -> Result<Self> {
		check_checksum(block)?;
		if !is_zero(&block[3..15]) {
			return Err(Error::NonZero("card type block bytes 03..14"));
		}
		Self::from_value(u32::from_be_bytes([0, block[0], block[1], block[2]]))
	}

	/// Encodes into block 1 with its checksum.
	#[must_use]
	pub fn encode(self) -> Block {
		let mut block = [0; BLOCK_SIZE];
		block[..3].copy_from_slice(&self.value().to_be_bytes()[1..]);
		with_checksum(block)
	}
}
