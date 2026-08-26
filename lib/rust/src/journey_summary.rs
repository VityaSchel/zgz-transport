use crate::bytes::{Block, check_checksum, in_range, with_checksum};
use crate::card_type::CardType;
use crate::date::Date;
use crate::direction::Direction;
use crate::error::{Error, Result};
use crate::route::Route;

/// A route and the direction travelled along it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct Leg {
	/// Route of the journey.
	pub route: Route,
	/// Direction along the route.
	pub direction: Direction,
}

/// When the last paid journey happened, to the minute.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct LastPaidAt {
	/// Bytes 2 and 3.
	pub date: Date,
	/// `0` to `23`.
	pub hour: u8,
	/// `0` to `59`.
	pub minute: u8,
}

/// Block 10 of a top up card, rewritten on every journey and untouched by top ups.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct JourneySummary {
	/// The journey before the current one; absent on the first ever journey.
	pub previous: Option<Leg>,
	/// Also after a free transfer, which then points at the paid ride it belongs to.
	pub last_paid_at: LastPaidAt,
	/// Same as byte 4 of the current transaction.
	pub consecutive_payments: u8,
	/// Product of the card.
	pub card_type: CardType,
	/// Whether the current journey was a free transfer.
	pub free: bool,
	/// Route of the current journey.
	pub route: Route,
	/// Direction of the current journey.
	pub direction: Direction,
	/// `0x63` after a paid journey, `0x62` after a free transfer.
	pub transfers_left: u8,
}

impl JourneySummary {
	/// The constant block 10 of personal cards, which [`decode`](Self::decode) rejects.
	pub const PERSONAL: Block = [
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x0a,
	];

	/// Decodes block 10 of a top up card.
	///
	/// # Errors
	/// [`Error::Checksum`](crate::Error::Checksum), [`Error::NonZero`](crate::Error::NonZero), [`Error::Range`](crate::Error::Range), [`Error::Direction`](crate::Error::Direction) or [`Error::UnknownCardTypeByte`](crate::Error::UnknownCardTypeByte).
	pub fn decode(block: &Block) -> Result<Self> {
		check_checksum(block)?;
		let [
			previous_route,
			previous_direction,
			d0,
			d1,
			hour,
			minute,
			consecutive_payments,
			card_type,
			free,
			route,
			direction,
			reserved11,
			reserved12,
			transfers_left,
			reserved14,
			_,
		] = *block;
		if reserved11 != 0 || reserved12 != 0 || reserved14 != 0 {
			return Err(Error::NonZero("journey summary bytes 11, 12 and 14"));
		}
		let hour = in_range("hour", hour, 0, 23)?;
		let minute = in_range("minute", minute, 0, 59)?;
		Ok(Self {
			previous: match previous_route {
				0 => None,
				route => Some(Leg {
					route: Route(route),
					direction: Direction::try_from(previous_direction)?,
				}),
			},
			last_paid_at: LastPaidAt {
				date: Date::decode([d0, d1])?,
				hour,
				minute,
			},
			consecutive_payments,
			card_type: CardType::from_byte(card_type)?,
			free: free == 1,
			route: Route(route),
			direction: Direction::try_from(direction)?,
			transfers_left,
		})
	}

	/// Encodes into block 10 with its checksum.
	///
	/// # Errors
	/// [`Error::Range`](crate::Error::Range) for a previous route of zero, an hour above 23 or a minute above 59, or the errors of [`Date::encode`].
	pub fn encode(self) -> Result<Block> {
		let hour = in_range("hour", self.last_paid_at.hour, 0, 23)?;
		let minute = in_range("minute", self.last_paid_at.minute, 0, 59)?;
		let (previous_route, previous_direction) = match self.previous {
			None => (0, 0),
			Some(leg) => (
				in_range("previous route", leg.route.0, 1, 0xff)?,
				u8::from(leg.direction),
			),
		};
		let [d0, d1] = self.last_paid_at.date.encode()?;
		Ok(with_checksum([
			previous_route,
			previous_direction,
			d0,
			d1,
			hour,
			minute,
			self.consecutive_payments,
			self.card_type.byte(),
			u8::from(self.free),
			self.route.0,
			u8::from(self.direction),
			0,
			0,
			self.transfers_left,
			0,
			0,
		]))
	}
}
