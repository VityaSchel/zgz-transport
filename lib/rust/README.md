# zgz-transport

Encoder and decoder for the Zaragoza and Aragon transport cards (Avanza Tarjeta Bus, Lazo), based on the reverse-engineered card spec at [https://git.hloth.dev/hloth/zgz-transport](https://git.hloth.dev/hloth/zgz-transport).

`no_std` with `alloc`, no dependencies, Rust 1.88 or newer.

## Install

**From [crates.io](https://crates.io/crates/zgz-transport):**

```sh
cargo add zgz-transport
```

## Usage

Decode a dump read with MIFARE Classic Tool or a Proxmark, at least blocks 0 to 33:

```rust,no_run
use zgz_transport::Card;

fn main() -> Result<(), Box<dyn std::error::Error>> {
	let card = Card::decode(&std::fs::read("dump.bin")?)?;
	println!("{} {} {}", card.uid, card.id, card.balance);
	for t in &card.transactions {
		println!("{} {:?} {} {}", t.created_at, t.kind, t.route, t.amount);
	}
	Ok(())
}
```

Every block structure is a type with a `decode`/`encode` pair over fixed-size byte arrays. `encode` returns `Result` where a field can be out of range and a plain block where it cannot (`CardType`, `CardId`):

```rust
use zgz_transport::{
	Balance, Card, CardId, CardType, Date, DateTime, Direction, Route, Stop, Time, Transaction,
	TransactionKind,
};

fn main() -> Result<(), zgz_transport::Error> {
	let ride = Transaction {
		card_type: CardType::AvanzaTopUp,
		network_flag: 0,
		amount: 550,
		consecutive_payments: 1,
		stop: Stop::Urban(500),
		route: Route(31),
		kind: TransactionKind::Journey(Direction::Two),
		run_counter: 7,
		created_at: DateTime {
			date: Date { year: 2026, month: 8, day: 26 },
			time: Time { hour: 9, minute: 41, second: 27 },
		},
		sequence: 1,
	};
	let mut dump = [0; 34 * 16];
	dump[5] = 0x88;
	dump[16..32].copy_from_slice(&CardType::AvanzaTopUp.encode());
	dump[32..48].copy_from_slice(&"BE123456".parse::<CardId>()?.encode());
	dump[80..96].copy_from_slice(&ride.encode()?);
	dump[128..144].copy_from_slice(&Balance(4450).encode()?);
	dump[144..160].copy_from_slice(&Balance(4450).encode()?);

	let card = Card::decode(&dump)?;
	assert_eq!(card.balance, Balance(4450));
	assert_eq!(card.transactions, [ride]);
	assert_eq!(CardType::AvanzaTopUp.keys(0).unwrap().a, Some([0x04, 0x00, 0x0c, 0x0f, 0x09, 0x03]));
	Ok(())
}
```

## License

[MIT](../../LICENSE)

## Donate

[hloth.dev/donate](https://hloth.dev/donate)
