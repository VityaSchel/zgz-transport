use zgz_transport::{Chip, Error, Uid};

use crate::hex::array;

#[test]
fn decodes_block_0_by_sak() {
	assert_eq!(
		Uid::decode(&array("1D68C3A9BF880400C8000020000000AB")),
		Ok(Uid::Single(array("1D68C3A9")))
	);
	assert_eq!(
		Uid::decode(&array("0468C3A9BF12341802008100000023AA")),
		Ok(Uid::Double(array("0468C3A9BF1234")))
	);
	assert_eq!(
		Uid::decode(&array("1D68C3A9BF000400C8000020000000AB")),
		Err(Error::Sak)
	);
}

#[test]
fn tells_the_chip_and_prints_upper_case_hex() {
	let single = Uid::Single([0x04, 0x00, 0x0a, 0xff]);
	let double = Uid::Double([0x04, 0x00, 0x0a, 0xff, 0x01, 0x02, 0x03]);
	assert_eq!(single.chip(), Chip::Classic1K);
	assert_eq!(double.chip(), Chip::Classic4K);
	assert_eq!(single.as_bytes(), [0x04, 0x00, 0x0a, 0xff]);
	assert_eq!(double.as_bytes().len(), 7);
	assert_eq!(single.to_string(), "04000AFF");
	assert_eq!(double.to_string(), "04000AFF010203");
}
