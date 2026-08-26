use crate::card_type::CardType;

/// A MIFARE Classic sector key.
pub type Key = [u8; 6];

/// Keys of one sector; a missing key is not known.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct SectorKeys {
	/// Key A.
	pub a: Option<Key>,
	/// Key B.
	pub b: Option<Key>,
}

const AVANZA_OPERATOR: SectorKeys = SectorKeys {
	a: Some([0x04, 0x00, 0x0c, 0x0f, 0x09, 0x03]),
	b: Some([0x0b, 0x02, 0x07, 0x0a, 0x04, 0x09]),
};
const AVANZA_UNUSED: SectorKeys = SectorKeys {
	a: Some([0xa0, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5]),
	b: Some([0xb0, 0xb1, 0xb2, 0xb3, 0xb4, 0xb5]),
};
const FACTORY: SectorKeys = SectorKeys {
	a: Some([0xff; 6]),
	b: Some([0xff; 6]),
};
const LAZO_SHARED: SectorKeys = SectorKeys {
	a: Some([0x4e, 0x30, 0x3d, 0x40, 0x2f, 0x20]),
	b: Some([0x24, 0x33, 0x72, 0x40, 0x7c, 0x2e]),
};

impl CardType {
	/// Keys of a sector, `None` past the chip's last one (15 on Avanza, 39 on Lazo).
	/// Sectors 0 to 8 are the same on both Avanza products, so either opens a card before block 1 is read.
	#[must_use]
	pub const fn keys(self, sector: u8) -> Option<SectorKeys> {
		match (self, sector) {
			(Self::AvanzaTopUp | Self::AvanzaPersonalUnlimited, 0..=8)
			| (Self::AvanzaPersonalUnlimited, 9..=15) => Some(AVANZA_OPERATOR),
			(Self::AvanzaTopUp, 9..=15) => Some(AVANZA_UNUSED),
			(Self::LazoTopUp, 0..=31) => Some(LAZO_SHARED),
			(Self::LazoTopUp, 32) => Some(SectorKeys {
				a: Some([0x21, 0x6f, 0x5b, 0x21, 0x2a, 0x7a]),
				b: Some([0x44, 0x20, 0x2e, 0x47, 0x6e, 0x5b]),
			}),
			(Self::LazoTopUp, 33) => Some(SectorKeys {
				a: Some([0x51, 0x48, 0x75, 0x5c, 0x34, 0x27]),
				b: Some([0x3c, 0x45, 0x20, 0x75, 0x37, 0x58]),
			}),
			(Self::LazoTopUp, 34) => Some(SectorKeys {
				a: None,
				b: Some([0x20, 0x6f, 0x7c, 0x4c, 0x4f, 0x36]),
			}),
			(Self::LazoTopUp, 35) => Some(SectorKeys {
				a: Some([0x52, 0x46, 0x61, 0x2e, 0x7c, 0x4b]),
				b: None,
			}),
			(Self::LazoTopUp, 36) => Some(SectorKeys {
				a: Some([0x35, 0x4b, 0x39, 0x45, 0x48, 0x61]),
				b: Some([0x56, 0x7d, 0x73, 0x4c, 0x40, 0x3c]),
			}),
			(Self::LazoTopUp, 37) => Some(SectorKeys {
				a: Some([0x45, 0x5d, 0x73, 0x2c, 0x38, 0x5f]),
				b: Some([0x24, 0x26, 0x21, 0x7b, 0x3b, 0x3b]),
			}),
			(Self::LazoTopUp, 38..=39) => Some(FACTORY),
			_ => None,
		}
	}
}
