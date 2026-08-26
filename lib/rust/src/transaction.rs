use crate::bytes::Block;
use crate::card_type::CardType;
use crate::date_time::DateTime;
use crate::direction::Direction;
use crate::error::{Error, Result};
use crate::route::Route;
use crate::stop::Stop;

const TOP_UP_KIND: u8 = 8;

/// Byte 8 of a transaction: a journey with its direction, or a top up.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TransactionKind {
	/// Byte 8 is `01` or `02`.
	Journey(Direction),
	/// Byte 8 is `08`.
	TopUp,
}

impl TransactionKind {
	fn from_byte(byte: u8) -> Result<Self> {
		match byte {
			TOP_UP_KIND => Ok(Self::TopUp),
			1 | 2 => Direction::try_from(byte).map(Self::Journey),
			_ => Err(Error::TransactionKind(byte)),
		}
	}

	fn byte(self) -> u8 {
		match self {
			Self::TopUp => TOP_UP_KIND,
			Self::Journey(direction) => direction.into(),
		}
	}
}

/// One 16-byte record of the transaction log.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Transaction {
	/// Product of the card that made the transaction.
	pub card_type: CardType,
	/// Byte 1: `0` on top up cards, `1` or `2` on personal cards.
	pub network_flag: u8,
	/// Money moved in [`Balance::UNITS_PER_EURO`](crate::Balance::UNITS_PER_EURO) units, `0` on free transfers.
	pub amount: u16,
	/// Payments of this card at one terminal in a row, counting from `1`; `0` on top ups.
	pub consecutive_payments: u8,
	/// Bytes 5 and 6.
	pub stop: Stop,
	/// Byte 7.
	pub route: Route,
	/// Byte 8.
	pub kind: TransactionKind,
	/// Byte 9, on buses most likely the ordinal of the vehicle run that day.
	pub run_counter: u8,
	/// Bytes 10 to 14.
	pub created_at: DateTime,
	/// `0` to `4`, selects the archive block, see [`Transaction::archive_block`].
	pub sequence: u8,
}

impl Transaction {
	/// Decodes one record of the transaction log.
	///
	/// # Errors
	/// [`Error::UnknownCardTypeByte`](crate::Error::UnknownCardTypeByte), [`Error::TransactionKind`](crate::Error::TransactionKind) or the errors of [`DateTime::decode`].
	pub fn decode(block: &Block) -> Result<Self> {
		let [
			card_type,
			network_flag,
			a0,
			a1,
			consecutive_payments,
			s0,
			s1,
			route,
			kind,
			run_counter,
			d0,
			d1,
			hour,
			minute,
			second,
			sequence,
		] = *block;
		let route = Route(route);
		let card_type = CardType::from_byte(card_type)?;
		let created_at = DateTime::decode([d0, d1, hour, minute, second])?;
		let kind = TransactionKind::from_byte(kind)?;
		Ok(Self {
			card_type,
			network_flag,
			amount: u16::from_be_bytes([a0, a1]),
			consecutive_payments,
			stop: Stop::decode([s0, s1], route),
			route,
			kind,
			run_counter,
			created_at,
			sequence,
		})
	}

	/// Encodes into one record of the transaction log.
	///
	/// # Errors
	/// The errors of [`Stop::encode`] and [`DateTime::encode`].
	pub fn encode(self) -> Result<Block> {
		let [a0, a1] = self.amount.to_be_bytes();
		let [s0, s1] = self.stop.encode()?;
		let [d0, d1, hour, minute, second] = self.created_at.encode()?;
		Ok([
			self.card_type.byte(),
			self.network_flag,
			a0,
			a1,
			self.consecutive_payments,
			s0,
			s1,
			self.route.0,
			self.kind.byte(),
			self.run_counter,
			d0,
			d1,
			hour,
			minute,
			second,
			self.sequence,
		])
	}

	/// Whether the transaction is a journey that cost nothing.
	#[must_use]
	pub const fn is_free_transfer(self) -> bool {
		matches!(self.kind, TransactionKind::Journey(_)) && self.amount == 0
	}
}
