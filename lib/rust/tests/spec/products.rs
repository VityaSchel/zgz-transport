use zgz_transport::{Balance, Card, CardType, Product, Subscription, SubscriptionMetadata};

use crate::card::{AVANZA_BLOCK_0, dump, id};
use crate::hex::array;

pub const METADATA: &str = "1101342F00210000001E002100000015";
pub const SUBSCRIPTION: &str = "342F344E0000010203043441081E0006";

pub fn card_with_products(card_type: CardType, sectors: &[usize]) -> Vec<u8> {
	let balance = Balance(0).encode().unwrap();
	let mut blocks = vec![
		(0, array(AVANZA_BLOCK_0)),
		(1, card_type.encode()),
		(2, id("BP123456")),
		(8, balance),
		(9, balance),
	];
	for &sector in sectors {
		blocks.push((sector * 4, array(METADATA)));
		blocks.push((sector * 4 + 1, array(SUBSCRIPTION)));
		blocks.push((sector * 4 + 2, array(SUBSCRIPTION)));
	}
	dump(&blocks)
}

#[test]
fn keeps_products_in_their_sectors() {
	let product = Some(Product {
		metadata: SubscriptionMetadata::decode(&array(METADATA)).unwrap(),
		subscription: Subscription::decode(&array(SUBSCRIPTION)).unwrap(),
	});
	let products = |card_type, sectors| {
		Card::decode(&card_with_products(card_type, sectors))
			.unwrap()
			.products
	};
	let personal = CardType::AvanzaPersonalUnlimited;
	assert_eq!(products(personal, &[]), [None, None]);
	assert_eq!(products(personal, &[3]), [product, None]);
	assert_eq!(products(personal, &[4]), [None, product]);
	assert_eq!(products(personal, &[3, 4]), [product, product]);
	assert_eq!(products(CardType::AvanzaTopUp, &[3, 4]), [None, None]);
}
