use zgz_transport::{BLOCK_SIZE, Balance, Card, CardType, Chip, Error, Uid};

use crate::card::{TRAILING_4K_BLOCKS, dump, id, top_up_dump};
use crate::hex::array;
use crate::products::card_with_products;

const SEVEN_BYTE_UID_HOLDING_A_SAK_AT_BYTE_5: &str = "0468C3A9BF88341802008100000023AA";

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
	assert_eq!(
		Card::decode(&unknown_sak),
		Err(Error::Sak {
			byte_5: 0x00,
			byte_7: 0x00,
		})
	);
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
fn reads_the_seven_byte_layout_before_the_four_byte_one() {
	let block: [u8; 16] = array(SEVEN_BYTE_UID_HOLDING_A_SAK_AT_BYTE_5);
	assert_eq!(block[5], 0x88);
	assert_ne!(block[4], block[0] ^ block[1] ^ block[2] ^ block[3]);
	assert_eq!(
		Uid::decode(&block),
		Ok((Uid::Double(array("0468C3A9BF8834")), Chip::Classic4K))
	);
	let balance = Balance(600).encode().unwrap();
	let mut dump = dump(&[
		(0, block),
		(1, CardType::LazoTopUp371F.encode()),
		(2, id("CT123456")),
		(8, balance),
		(9, balance),
	]);
	dump.extend(vec![0; TRAILING_4K_BLOCKS * BLOCK_SIZE]);
	let card = Card::decode(&dump).unwrap();
	assert_eq!(card.uid, Uid::Double(array("0468C3A9BF8834")));
	assert_eq!(card.chip, Chip::Classic4K);
}

#[test]
fn reports_the_first_fault_in_block_order() {
	let mut dump = top_up_dump();
	dump[5] = 0;
	dump[9 * BLOCK_SIZE] ^= 1;
	dump[BLOCK_SIZE] = 0x0b;
	dump[2 * BLOCK_SIZE - 1] ^= 0x02 ^ 0x0b;
	dump[3 * BLOCK_SIZE - 1] ^= 1;
	assert_eq!(
		Card::decode(&dump),
		Err(Error::Sak {
			byte_5: 0x00,
			byte_7: 0x00,
		})
	);
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
	let mut dump = card_with_products(CardType::AvanzaPersonal, &[3]);
	dump[14 * BLOCK_SIZE - 1] ^= 1;
	assert!(matches!(Card::decode(&dump), Err(Error::Checksum { .. })));
}
