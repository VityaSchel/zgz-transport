use crate::bytes::{BLOCK_SIZE, Block, check_checksum, is_zero, with_checksum};
use crate::error::{Error, Result};
use crate::uid::Chip;

/// Card types identified by bytes 0 to 2 of block 1.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum CardType {
	/// Balance top-up Avanza card, `02699F`.
	AvanzaTopUp,
	/// Personal expiring Avanza card, `0A9775`.
	AvanzaPersonal,
	/// Personal expiring Avanza card, `0A98DA`, printed "Abono de transporte". What separates the
	/// two personal card types is unknown.
	AvanzaPersonalAbono,
	/// Balance top-up Lazo card, `0D371F`.
	LazoTopUp371F,
	/// Balance top-up Lazo card, `0D375F`. What separates it from
	/// [`LazoTopUp371F`](Self::LazoTopUp371F) is unknown: a card carrying it holds the same keys,
	/// balance, transaction ring and empty product sectors.
	LazoTopUp375F,
}

impl CardType {
	/// Every known card type.
	pub const ALL: [Self; 5] = [
		Self::AvanzaTopUp,
		Self::AvanzaPersonal,
		Self::AvanzaPersonalAbono,
		Self::LazoTopUp371F,
		Self::LazoTopUp375F,
	];

	/// Bytes 0-2 of block 1.
	#[must_use]
	pub const fn value(self) -> u32 {
		match self {
			Self::AvanzaTopUp => 0x02_69_9f,
			Self::AvanzaPersonal => 0x0a_97_75,
			Self::AvanzaPersonalAbono => 0x0a_98_da,
			Self::LazoTopUp371F => 0x0d_37_1f,
			Self::LazoTopUp375F => 0x0d_37_5f,
		}
	}

	/// The chip the card is built on.
	#[must_use]
	pub const fn chip(self) -> Chip {
		match self {
			Self::AvanzaTopUp | Self::AvanzaPersonal | Self::AvanzaPersonalAbono => Chip::Classic1K,
			Self::LazoTopUp371F | Self::LazoTopUp375F => Chip::Classic4K,
		}
	}

	/// Whether the card carries subscription products instead of a balance.
	#[must_use]
	pub const fn is_personal(self) -> bool {
		matches!(self, Self::AvanzaPersonal | Self::AvanzaPersonalAbono)
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
