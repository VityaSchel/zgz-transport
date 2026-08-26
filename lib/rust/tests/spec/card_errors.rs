use zgz_transport::{BLOCK_SIZE, Balance, Card, CardType, Error, Uid};

use crate::card::{dump, id, top_up_dump};
use crate::hex::array;
use crate::products::card_with_products;

#[test]
fn rejects_malformed_dumps() {
	let dump = top_up_dump();
	assert_eq!(
		Card::decode(&dump[..33 * BLOCK_SIZE]),
		Err(Error::DumpSize {
			minimum: 544,
			got: 528,
		})
	);
	assert_eq!(
		Card::decode(&dump[..=35 * BLOCK_SIZE]),
		Err(Error::DumpSize {
			minimum: 544,
			got: 561,
		})
	);
	assert!(Card::decode(&dump[..34 * BLOCK_SIZE]).is_ok());
	let mut unknown_sak = dump.clone();
	unknown_sak[5] = 0;
	assert_eq!(Card::decode(&unknown_sak), Err(Error::Sak));
	let mut differing = dump.clone();
	differing[9 * BLOCK_SIZE] ^= 1;
	assert_eq!(Card::decode(&differing), Err(Error::BalanceBlocksDiffer));
	let mut wrong_type = dump;
	wrong_type[BLOCK_SIZE] = 0x0b;
	wrong_type[2 * BLOCK_SIZE - 1] ^= 0x02 ^ 0x0b;
	assert_eq!(
		Card::decode(&wrong_type),
		Err(Error::UnknownCardType(0x0b_69_9f))
	);
}

#[test]
fn reads_the_sak_at_the_4k_offset_before_the_1k_one() {
	let balance = Balance(600).encode().unwrap();
	let dump = dump(&[
		(0, array("0468C3A9BF88341802008100000023AA")),
		(1, CardType::LazoTopUp.encode()),
		(2, id("CT123456")),
		(8, balance),
		(9, balance),
	]);
	assert_eq!(
		Uid::decode(&array("0468C3A9BF88341802008100000023AA")),
		Ok(Uid::Double(array("0468C3A9BF8834")))
	);
	assert_eq!(
		Card::decode(&dump).unwrap().uid,
		Uid::Double(array("0468C3A9BF8834"))
	);
}

#[test]
fn reports_the_first_fault_in_block_order() {
	let mut dump = top_up_dump();
	dump[5] = 0;
	dump[9 * BLOCK_SIZE] ^= 1;
	dump[BLOCK_SIZE] = 0x0b;
	dump[2 * BLOCK_SIZE - 1] ^= 0x02 ^ 0x0b;
	dump[3 * BLOCK_SIZE - 1] ^= 1;
	assert_eq!(Card::decode(&dump), Err(Error::Sak));
	dump[5] = 0x88;
	assert_eq!(Card::decode(&dump), Err(Error::BalanceBlocksDiffer));
	dump[9 * BLOCK_SIZE] ^= 1;
	assert_eq!(Card::decode(&dump), Err(Error::UnknownCardType(0x0b_69_9f)));
	dump[BLOCK_SIZE] = 0x02;
	dump[2 * BLOCK_SIZE - 1] ^= 0x02 ^ 0x0b;
	assert!(matches!(Card::decode(&dump), Err(Error::Checksum { .. })));
}

#[test]
fn propagates_product_block_errors() {
	let mut dump = card_with_products(CardType::AvanzaPersonalUnlimited, &[3]);
	dump[14 * BLOCK_SIZE - 1] ^= 1;
	assert!(matches!(Card::decode(&dump), Err(Error::Checksum { .. })));
}
