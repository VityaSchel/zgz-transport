use zgz_transport::{CardId, Error};

use crate::hex::array;

const CASES: [(&str, &str); 7] = [
	("42453227430000000000000000000051", "BE322743"),
	("42453227420000000000000000000050", "BE322742"),
	("4245739977000000000000000000009A", "BE739977"),
	("42504232450000000000000000000027", "BP423245"),
	("435443048600000000000000000000D6", "CT430486"),
	("42453227431200000000000000000043", "BE32274312"),
	("42450000000000000000000000000007", "BE000000"),
];

#[test]
fn decodes_ids() {
	for (encoded, decoded) in CASES {
		assert_eq!(
			CardId::decode(&array(encoded)).unwrap().to_string(),
			decoded
		);
	}
}

#[test]
fn parses_and_encodes_ids() {
	for (encoded, decoded) in CASES {
		let id: CardId = decoded.parse().unwrap();
		assert_eq!(id.encode(), array::<16>(encoded));
		assert_eq!(id, CardId::decode(&array(encoded)).unwrap());
		assert_eq!(format!("{id:?}"), format!("CardId({decoded})"));
	}
}

#[test]
fn fills_all_thirteen_digit_bytes() {
	let id: CardId = "BE12345678901234567890123456".parse().unwrap();
	let block = id.encode();
	assert_eq!(&block[..15], &array::<15>("424512345678901234567890123456"));
	assert_eq!(
		CardId::decode(&block).unwrap().to_string(),
		"BE12345678901234567890123456"
	);
}

#[test]
fn rejects_malformed_ids() {
	for id in [
		"",
		"BE",
		"BE12345",
		"BE1234567",
		"be322743",
		"B3322743",
		"BE12345A",
		"BE1234567890123456789012345678",
		"ÉE322743",
		"Bé322743",
		"BE١٢٣٤٥٦",
	] {
		assert_eq!(id.parse::<CardId>(), Err(Error::IdFormat));
	}
}

#[test]
fn displays_whatever_a_checksummed_block_holds() {
	let id = CardId::decode(&array("C9004ABF12000000000000000000002E")).unwrap();
	assert_eq!(id.to_string(), "É\u{0}4abf12");
	assert_eq!(id.to_string().parse::<CardId>(), Err(Error::IdFormat));
}

#[test]
fn trims_trailing_zero_bytes_when_displaying() {
	for id in ["BE12000000", "BE1200000000"] {
		let id: CardId = id.parse().unwrap();
		assert_eq!(id.to_string(), "BE120000");
		assert_eq!(id.encode(), array::<16>("42451200000000000000000000000015"));
	}
}

#[test]
fn rejects_a_wrong_checksum() {
	assert_eq!(
		CardId::decode(&array("42453227430000000000000000000050")),
		Err(Error::Checksum {
			expected: 0x51,
			got: 0x50,
		})
	);
}
