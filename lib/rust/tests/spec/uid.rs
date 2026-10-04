use zgz_transport::{Chip, Error, Uid};

use crate::hex::array;

const AVANZA: &str = "1D68C3A91F880400C8000020000000AB";
const LAZO_SEVEN_BYTE: &str = "0468C3A9BF12341802008100000023AA";
const LAZO_FOUR_BYTE: &str = "0468C3A906180200800000000000AA23";

#[test]
fn decodes_block_0_by_sak_and_atqa() {
	assert_eq!(
		Uid::decode(&array(AVANZA)),
		Ok((Uid::Single(array("1D68C3A9")), Chip::Classic1K))
	);
	assert_eq!(
		Uid::decode(&array(LAZO_SEVEN_BYTE)),
		Ok((Uid::Double(array("0468C3A9BF1234")), Chip::Classic4K))
	);
	assert_eq!(
		Uid::decode(&array(LAZO_FOUR_BYTE)),
		Ok((Uid::Single(array("0468C3A9")), Chip::Classic4K))
	);
	assert_eq!(
		Uid::decode(&array("1D68C3A91F000400C8000020000000AB")),
		Err(Error::Sak {
			byte_5: 0x00,
			byte_7: 0x00,
		})
	);
}

#[test]
fn rejects_a_four_byte_layout_whose_bcc_does_not_match() {
	let mut block: [u8; 16] = array(AVANZA);
	block[4] ^= 1;
	assert_eq!(
		Uid::decode(&block),
		Err(Error::Sak {
			byte_5: 0x88,
			byte_7: 0x00,
		})
	);
}

#[test]
fn rejects_a_known_sak_under_another_chips_atqa() {
	let mut block: [u8; 16] = array(LAZO_FOUR_BYTE);
	block[6] = 0x04;
	assert_eq!(
		Uid::decode(&block),
		Err(Error::Sak {
			byte_5: 0x18,
			byte_7: 0x00,
		})
	);
}

#[test]
fn counts_the_sectors_of_each_chip() {
	assert_eq!(Chip::Classic1K.sectors(), 16);
	assert_eq!(Chip::Classic4K.sectors(), 40);
}

#[test]
fn prints_upper_case_hex() {
	let single = Uid::Single([0x04, 0x00, 0x0a, 0xff]);
	let double = Uid::Double([0x04, 0x00, 0x0a, 0xff, 0x01, 0x02, 0x03]);
	assert_eq!(single.as_bytes(), [0x04, 0x00, 0x0a, 0xff]);
	assert_eq!(double.as_bytes().len(), 7);
	assert_eq!(single.to_string(), "04000AFF");
	assert_eq!(double.to_string(), "04000AFF010203");
}
