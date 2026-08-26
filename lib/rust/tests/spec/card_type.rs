use zgz_transport::{CardType, Error};

use crate::hex::array;

const CASES: [(CardType, &str); 3] = [
	(CardType::AvanzaTopUp, "02699F000000000000000000000000F4"),
	(
		CardType::AvanzaPersonalUnlimited,
		"0A9775000000000000000000000000E8",
	),
	(CardType::LazoTopUp, "0D371F00000000000000000000000025"),
];

#[test]
fn decodes_card_types() {
	for (card_type, encoded) in CASES {
		assert_eq!(CardType::decode(&array(encoded)), Ok(card_type));
	}
}

#[test]
fn encodes_card_types() {
	for (card_type, encoded) in CASES {
		assert_eq!(card_type.encode(), array::<16>(encoded));
	}
}

#[test]
fn maps_the_first_byte_to_the_type() {
	for (card_type, _) in CASES {
		assert_eq!(CardType::from_byte(card_type.byte()), Ok(card_type));
		assert_eq!(CardType::from_value(card_type.value()), Ok(card_type));
	}
	assert_eq!(
		CardType::from_byte(0x03),
		Err(Error::UnknownCardTypeByte(3))
	);
	assert_eq!(CardType::ALL.map(CardType::byte), [0x02, 0x0a, 0x0d]);
}

#[test]
fn rejects_an_unknown_type() {
	assert_eq!(
		CardType::decode(&array("FFFFFF000000000000000000000000FF")),
		Err(Error::UnknownCardType(0xff_ff_ff))
	);
}

#[test]
fn rejects_a_wrong_checksum() {
	assert_eq!(
		CardType::decode(&array("02699F000000000000000000000000F5")),
		Err(Error::Checksum {
			expected: 0xf4,
			got: 0xf5,
		})
	);
}

#[test]
fn checks_the_checksum_then_the_padding_then_the_type() {
	let mut block: [u8; 16] = array("FFFFFF01000000000000000000000000");
	assert_eq!(
		CardType::decode(&block),
		Err(Error::Checksum {
			expected: 0xfe,
			got: 0,
		})
	);
	block[15] = 0xfe;
	assert_eq!(
		CardType::decode(&block),
		Err(Error::NonZero("card type block bytes 03..14"))
	);
	block[3] = 0;
	block[15] = 0xff;
	assert_eq!(
		CardType::decode(&block),
		Err(Error::UnknownCardType(0xff_ff_ff))
	);
}

#[test]
fn rejects_non_zero_padding() {
	assert_eq!(
		CardType::decode(&array("02699F010000000000000000000000F5")),
		Err(Error::NonZero("card type block bytes 03..14"))
	);
}
