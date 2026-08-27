# Zaragoza & Aragon Transport Card Spec

[![NPM Version](https://img.shields.io/npm/v/zgz-transport?style=flat-square&logo=npm&logoColor=23ffffff%&label=%20&color=%23cd0000)](https://www.npmjs.com/package/zgz-transport) [![JSR Version](https://img.shields.io/jsr/v/%40hloth/zgz-transport?style=flat-square&logo=jsr&logoColor=%23f7df1e&label=%20&color=%23155775)](https://jsr.io/@hloth/zgz-transport) [![Crates.io Version](https://img.shields.io/crates/v/zgz-transport?style=flat-square&logo=rust&label=%20&color=black)
](https://crates.io/crates/zgz-transport) [![Maven Central Version](https://img.shields.io/maven-central/v/dev.hloth/zgz-transport?style=flat-square&logo=apache-maven&logoColor=%23f18900&label=%20&color=%2327282c)
](https://central.sonatype.com/artifact/dev.hloth/zgz-transport)


> [!IMPORTANT]
> ⚠️ **Aviso legal:** Este repositorio es un proyecto de investigación de seguridad independiente. \
> No está destinado a cometer fraude ni a facilitar el uso indebido del transporte público. \
> No se proporcionará asistencia para ningún uso ilícito. \
> Consulta [LEGAL.md](./LEGAL.md) para el descargo de responsabilidad completo.

Zaragoza and Aragon Avanza/Lazo bus & tram public transport card full up-to-date specification, reverse engineered from multiple MIFARE Classic card dumps. This project intends to be a security research paper, publicly available for anyone for free and is not qualified as a guide. There are no step-by-step instructions on how to commit fraud.

- [Zaragoza \& Aragon Transport Card Spec](#zaragoza--aragon-transport-card-spec)
	- [Getting started](#getting-started)
	- [Avanza Tarjeta Bus](#avanza-tarjeta-bus)
		- [Avanza keys](#avanza-keys)
		- [Avanza blocks](#avanza-blocks)
	- [Lazo card](#lazo-card)
		- [Lazo keys](#lazo-keys)
		- [Lazo blocks](#lazo-blocks)
	- [Data structures](#data-structures)
		- [Date](#date)
		- [Card ID](#card-id)
		- [Card type](#card-type)
		- [Balance](#balance)
		- [Transaction log](#transaction-log)
		- [Journey summary (block 10)](#journey-summary-block-10)
		- [Subscription metadata](#subscription-metadata)
		- [Subscription](#subscription)
	- [Unconfirmed](#unconfirmed)
		- [Byte 09 of a transaction](#byte-09-of-a-transaction)
		- [Route ids 152, 169 and 251](#route-ids-152-169-and-251)
		- [Directions](#directions)
		- [Stop ids](#stop-ids)
		- [Block 24](#block-24)
		- [Block 4](#block-4)
		- [Subscription blocks](#subscription-blocks)
		- [Notes](#notes)
	- [Implementations](#implementations)
		- [JavaScript, TypeScript](#javascript-typescript)
		- [Rust](#rust)
		- [Java, Kotlin](#java-kotlin)
	- [Contributing](#contributing)
	- [See also](#see-also)
	- [Acknowledgements](#acknowledgements)
	- [License](#license)
	- [Donate](#donate)

## Getting started

Zaragoza and Aragon public transport uses Avanza and Lazo cards, both of which are MIFARE Classic, meaning they're vulnerable to the Crypto-1 attack. The keys can be dumped with a Proxmark, but since they're static and well-known, you can use the ones listed for each card below.

Every sector has two keys (Key A and Key B) that control access to its blocks, and the last block of every sector is its trailer, holding both keys and the access conditions.

|         | [Avanza Tarjeta Bus](#avanza-tarjeta-bus) | [Lazo card](#lazo-card)                               |
| ------- | ----------------------------------------- | ----------------------------------------------------- |
| Chip    | MIFARE Classic 1K                         | MIFARE Classic 4K                                     |
| SAK     | `88`                                      | `18`                                                  |
| UID     | 4 bytes                                   | 7 bytes                                               |
| Sectors | 16                                        | 40 (sectors 0-31 with 4 blocks, 32-39 with 16 blocks) |
| Blocks  | 64                                        | 256                                                   |

What is stored inside the blocks is the same on both cards and is documented once under [data structures](#data-structures). Block 1 tells the products apart, see [card type](#card-type). Everything that is still unresolved is collected under [unconfirmed](#unconfirmed).

## Avanza Tarjeta Bus

### Avanza keys

| Sectors                         | Key A          | Key B          |
| ------------------------------- | -------------- | -------------- |
| 0-8                             | `04000C0F0903` | `0B02070A0409` |
| 9-15 (unused) on top up cards   | `A0A1A2A3A4A5` | `B0B1B2B3B4B5` |
| 9-15 (unused) on personal cards | `04000C0F0903` | `0B02070A0409` |

On top up cards Key B can rewrite the keys and access conditions of every sector (trailer access bits `011`); on personal cards the trailers are locked (`110`). The access conditions of the data blocks are the same on both.

### Avanza blocks

| Sector | Block | Description                                                                                                                                                                            | Template                           | Access Conditions    |
| ------ | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | -------------------- |
| 0      | 0     | [00-03] RFID's UID<br>[04] BCC (checksum byte)<br>[05] SAK (`88` for MIFARE Classic 1K)<br>[15] last two digits of the manufacturing year (`20` for 2020, `25` for 2025)               | `..,,..,,..880400C8,,0020000000..` | Read-only            |
|        | 1     | [Card type](#card-type)                                                                                                                                                                | `..,,..000000000000000000000000,,` | Only Key B can write |
|        | 2     | [Card ID](#card-id)                                                                                                                                                                    | `42,,..,,..00000000000000000000..` | Read-only            |
|        | 3     | [0th sector's trailer block](https://github.com/andrea-peter/nfc_mifare_classic_notes/blob/main/mifare-classic.md#sector-trailer-block)                                                | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 1      | 4     | Empty on top up cards, see [block 4](#block-4)<br>[15] XOR of all previous bytes                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 5     | Latest [transaction log](#transaction-log) entry                                                                                                                                       | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 6     | _Appears_ to always be empty                                                                                                                                                           | `00000000000000000000000000000000` | No restrictions      |
|        | 7     | 1st sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 2      | 8     | [Balance](#balance)                                                                                                                                                                    | `..,,0000..,,FFFF..,,000002FD02FD` | Value block          |
|        | 9     | Always has the same value as block 8                                                                                                                                                   | `..,,0000..,,FFFF..,,000002FD02FD` | Value block          |
|        | 10    | [Journey summary](#journey-summary-block-10), empty on a new card<br>Always `000000000000000A000000000000000A` on unlimited personal cards                                             | `..,,..,,..,,..02..,,..0000..00..` | No restrictions      |
|        | 11    | 2nd sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 3      | 12    | [Subscription metadata](#subscription-metadata)                                                                                                                                        | `00000000000000000000000000000000` | Only Key B can write |
|        | 13    | [Subscription](#subscription) on personal unlimited cards                                                                                                                              | `00000000000000000000000000000000` | Only Key B can write |
|        | 14    | Copy of block 13                                                                                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 15    | 3rd sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 4      | 16    | [Subscription metadata](#subscription-metadata) of a second product                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 17    | [Subscription](#subscription) of the second product                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 18    | Copy of block 17                                                                                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 19    | 4th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 5      | 20    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 21    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 22    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 23    | 5th sector's trailer blocks                                                                                                                                                            | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 6      | 24    | Empty on most cards, see [block 24](#block-24)                                                                                                                                         | `00000000000000000000000000000000` | Only Key B can write |
|        | 25    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 26    | Empty                                                                                                                                                                                  | `00000000000000000000000000000000` | Only Key B can write |
|        | 27    | 6th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 7      | 28    | [Transaction logs](#transaction-log); value of block 5 right before overwriting it, archived here when its sequence counter is `0` and in blocks 29, 30, 32 and 33 for counters 1 to 4 | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 29    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 30    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 31    | 7th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |
| 8      | 32    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 33    | See block 28                                                                                                                                                                           | `02..,,..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 34    | Expiration date, encoding unknown; always `00000000FFFFFFFF0000000000FF00FF` on top up cards                                                                                           | `00000000FFFFFFFF0000000000FF00FF` | Value block          |
|        | 35    | 8th sector's trailer block                                                                                                                                                             | `04000C0F0903..,,..,,0B02070A0409` | _Trailer_            |

Blocks 8, 9 and 34 are value blocks: a 32-bit integer, its bitwise complement, then the integer again, with value block access conditions (Key A can read, decrement, restore and transfer, Key B can also write and increment).

## Lazo card

### Lazo keys

| Sectors        | Blocks  | Key A          | Key B          |
| -------------- | ------- | -------------- | -------------- |
| 0-31           | 0-127   | `4E303D402F20` | `243372407C2E` |
| 32             | 128-143 | `216F5B212A7A` | `44202E476E5B` |
| 33             | 144-159 | `5148755C3427` | `3C4520753758` |
| 34             | 160-175 | Unknown        | `206F7C4C4F36` |
| 35             | 176-191 | `5246612E7C4B` | Unknown        |
| 36             | 192-207 | `354B39454861` | `567D734C403C` |
| 37             | 208-223 | `455D732C385F` | `2426217B3B3B` |
| 38-39 (unused) | 224-255 | `FFFFFFFFFFFF` | `FFFFFFFFFFFF` |

Sectors 38 and 39 still have the factory default keys and access conditions (`FF0780`).

### Lazo blocks

| Sector | Block                                  | Description                                                                                                                                                                                               | Template                           | Access Conditions             |
| ------ | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | ----------------------------- |
| 0      | 0                                      | [00-06] RFID's UID (7 bytes, so no BCC)<br>[07] SAK (`18` for MIFARE Classic 4K)<br>[08-09] ATQA (`0200`)<br>[10-15] manufacturer data; [15] is most likely the last two digits of the manufacturing year | `..,,..,,..,,..1802008100000023..` | Read-only                     |
|        | 1                                      | [Card type](#card-type)                                                                                                                                                                                   | `..,,..000000000000000000000000,,` | No restrictions               |
|        | 2                                      | [Card ID](#card-id)                                                                                                                                                                                       | `4354..,,..00000000000000000000..` | Only Key B can write          |
|        | 3                                      | [0th sector's trailer block](https://github.com/andrea-peter/nfc_mifare_classic_notes/blob/main/mifare-classic.md#sector-trailer-block)                                                                   | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 1      | 4                                      | _Appears_ to always be empty                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 5                                      | Latest [transaction log](#transaction-log) entry                                                                                                                                                          | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 6                                      | _Appears_ to always be empty                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 7                                      | 1st sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 2      | 8                                      | [Balance](#balance)                                                                                                                                                                                       | `..,,0000..,,FFFF..,,000002FD02FD` | No restrictions               |
|        | 9                                      | Always has the same value as block 8                                                                                                                                                                      | `..,,0000..,,FFFF..,,000002FD02FD` | No restrictions               |
|        | 10                                     | [Journey summary](#journey-summary-block-10), empty on a new card                                                                                                                                         | `..,,..,,..,,..0D..,,..0000..00..` | No restrictions               |
|        | 11                                     | 2nd sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 3-6    | 12, 16, 20, 24                         | Empty on top up cards                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
|        | 13-14, 17-18, 21-22, 25-26             | Empty on top up cards                                                                                                                                                                                     | `00000000000000000000000000000000` | No restrictions               |
|        | 15, 19, 23, 27                         | Trailer blocks of sectors 3-6                                                                                                                                                                             | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 7      | 28-30                                  | [Transaction log](#transaction-log) archive slots for sequence counters 0, 1 and 2                                                                                                                        | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 31                                     | 7th sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 8      | 32-33                                  | [Transaction log](#transaction-log) archive slots for sequence counters 3 and 4                                                                                                                           | `0D..,,..,,..,,..,,..,,..,,..,,..` | No restrictions               |
|        | 34                                     | Always empty                                                                                                                                                                                              | `00000000000000000000000000000000` | No restrictions               |
|        | 35                                     | 8th sector's trailer block                                                                                                                                                                                | `4E303D402F20..,,..,,243372407C2E` | _Trailer_                     |
| 9      | 36-38                                  | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | No restrictions               |
| 10, 15 | 40-42, 60-62                           | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
| 11-14  | data blocks 44-46, 48-50, 52-54, 56-58 | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can read and write |
| 16-31  | data blocks 64-126                     | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Value block                   |
| 32-37  | data blocks 128-222                    | Empty                                                                                                                                                                                                     | `00000000000000000000000000000000` | Only Key B can write          |
| 38-39  | data blocks 224-254                    | Empty, never personalized                                                                                                                                                                                 | `00000000000000000000000000000000` | No restrictions               |

The trailer blocks of sectors 9-39 (39, 43, ..., 127, then 143, 159, 175, 191, 207, 223, 239 and 255) hold the keys from the table above.

Blocks 8 and 9 use the value block format (integer, complement, integer) but their sector is unrestricted, so both keys can write them directly. Only the empty sectors 16-31 carry real value block access conditions.

## Data structures

These are the same on both cards. Where a field differs on personal unlimited cards, the field list says so.

### Date

Two bytes, read as 16 bits:

- 7 bits: year, 2000-based
- 4 bits: month (1-12)
- 5 bits: day (1-31)

`34 4E` = `0011010 0010 01110` = 2026-02-14.

See implementation for [JS](lib/javascript/src/date.ts), [Rust](lib/rust/src/date.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardDate.java).

### Card ID

- [00-01] ASCII prefix: `BE` and `BP` on Avanza cards, `CT` on Lazo cards
- [02-04] Card number, one decimal digit per nibble
- [05-14] zero
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/id.ts), [Rust](lib/rust/src/id.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardId.java).

### Card type

Block 1 says which product a card is:

- [00-02] Card type
- [03-14] zero
- [15] XOR of all previous bytes

| Card type                     | Chip              | Block 1 value                    |
| ----------------------------- | ----------------- | -------------------------------- |
| Balance top-up Avanza card    | MIFARE Classic 1K | `02699F000000000000000000000000` |
| Personal expiring Avanza card | MIFARE Classic 1K | `0A9775000000000000000000000000` |
| Balance top-up Lazo card      | MIFARE Classic 4K | `0D371F000000000000000000000000` |

Top up cards pay for each journey out of a [balance](#balance); personal cards use a [subscription](#subscription) and never change balance.

See implementation for [JS](lib/javascript/src/type.ts), [Rust](lib/rust/src/card_type.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/CardType.java).

### Balance

Blocks 8 and 9, identical, in the MIFARE value block format. €1.00 = 1000 units.

- [00-03] units, little-endian
- [04-07] bitwise complement of [00-03]
- [08-11] [00-03] again
- [12-15] address bytes, always `02FD02FD`

€5.00 is `8813000077ECFFFF8813000002FD02FD`. Personal unlimited cards always hold zero: `00000000FFFFFFFF0000000002FD02FD`.

See implementation for [JS](lib/javascript/src/balance.ts), [Rust](lib/rust/src/balance.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Balance.java).

### Transaction log

Six 16-byte records: block 5 holds the newest, blocks 28, 29, 30, 32 and 33 the five before it. When a new transaction is written, the previous block 5 is copied into the archive slot selected by its sequence byte [15] (`0` to block 28, `1` to 29, `2` to 30, `3` to 32, `4` to 33), so the counter wraps every five transactions. Block 34 is never part of the ring.

- [00] First byte of the [card type](#card-type)
- [01] `00` on top up cards; `01` or `02` on personal cards, matching bit 15 of [05-06]
- [02-03] Amount, big-endian, in [balance](#balance) units; `0000` when free of charge, always `0000` on personal cards
- [04] Consecutive payments of this card at one terminal, counting from 1; `00` on top ups
- [05-06] Stop. Bit 15 set: an urban bus stop, the other 15 bits an internal stop id. Bit 15 clear: the tram, where the value is the stop number × 100 (`0514` = 1300 = Plaza España), or another operator
- [07] Route id of the operator's GTFS feed: numbered buses have their number, Ci1 to Ci4 are 11 to 14, N1 to N7 are 111 to 117, the tram is 210
- [08] `01` or `02`: a journey and its direction; `08`: a top up
- [09] A per-run counter on buses, see [byte 09](#byte-09-of-a-transaction)
- [10-11] [Date](#date)
- [12] Hour, [13] minute, [14] second, plain binary
- [15] Sequence counter, 0 to 4

A journey subtracts the amount from the balance, a top up adds it. A journey with amount `0000` is a free transfer, granted within about half an hour of a paid ride, on bus and tram alike. A top up carries the point of sale id in [05-06] and zeros in [04], [07] and [09], unless it was made on board, where it carries the line and stop like a journey. Top ups do not touch [block 10](#journey-summary-block-10), and the balance a new card is sold with leaves no record.

Known directions: on the tram `01` runs south to Mago de Oz and `02` north to Avenida de la Academia; on bus 31 `02` runs to Puerto Venecia and `01` to Aljafería; on bus 22 `02` runs to Las Fuentes and `01` to Bombarda. Other lines: see [directions](#directions).

See implementation for [JS](lib/javascript/src/transaction.ts), [Rust](lib/rust/src/transaction.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Transaction.java).

### Journey summary (block 10)

Rewritten on every journey, untouched by top ups, so it stays all zero on a card that has only ever been topped up. On personal unlimited cards it is always `000000000000000A000000000000000A`; the layout below is the top up card one.

- [00-01] Line and direction of the previous journey, `0000` on the first; consecutive payments at one terminal count as one journey
- [02-03] [Date](#date) and [04-05] hour and minute of the last journey that was charged; on a free transfer these still point at the paid ride it belongs to
- [06] Consecutive payments counter of the current journey, the same as byte [04] of block 5
- [07] First byte of the [card type](#card-type)
- [08] `01` when the current journey was free, `00` when paid
- [09-10] Line and direction of the current journey
- [11-12] `0000`
- [13] `63` after a paid journey, `62` after a free transfer
- [14] `00`
- [15] XOR of all previous bytes

A free transfer onto the tram 33 minutes after a paid ride on bus 31, block 5 above block 10:

```text
    ty ?1 amt  cp stop ln dr ?9 date HH MM SS sq
 5: 0D 00 0000 01 05DC D2 02 01 3518 16 2C 20 00
10: 1F 01 3518 16 0B 01 0D 01 D2 02 0000 62 00 91
    pl pd date HH MM cp ty fr ln dr 0000 tr 00 xr
    ty = card type byte      cp = consecutive payments     fr = free flag
   amt = amount              ln dr = line, direction       tr = 63 paid / 62 free
  stop = stop id             pl pd = previous line, direction
  date = date                HH MM SS = time               sq = sequence
    ?1 = byte 01             ?9 = byte 09                  xr = XOR
```

See implementation for [JS](lib/javascript/src/journey-summary.ts), [Rust](lib/rust/src/journey_summary.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/JourneySummary.java).

### Subscription metadata

Blocks 12 and 16 on personal cards, one product per sector. Still [work in progress](#subscription-blocks).

- [00] Unknown
- [01] Unknown, appears to always be `01`
- [02-03] Purchase [date](#date)
- [04-07] Unknown, appears to always be `00210000`
- [08-09] Validity in days
- [10-14] Unknown, appears to always be `0021000000`
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/subscription-metadata.ts), [Rust](lib/rust/src/subscription_metadata.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/SubscriptionMetadata.java).

### Subscription

Blocks 13 and 14 (a copy) for the product of block 12, blocks 17 and 18 for the product of block 16. Still [work in progress](#subscription-blocks).

- [00-01] Start [date](#date)
- [02-03] End date
- [04-05] Unknown, appears to always be `0000`
- [06-09] Unknown
- [10-11] [Date](#date) of the last usage, [12] hour, [13] minute, [14] second
- [15] XOR of all previous bytes

See implementation for [JS](lib/javascript/src/subscription.ts), [Rust](lib/rust/src/subscription.rs), [Java](lib/java/src/main/java/dev/hloth/zgztransport/Subscription.java).

## Unconfirmed

This section is just theories and open questions. The Lazo findings come from a single card, the Avanza ones from eight cards.

### Byte 09 of a transaction

On urban buses it behaves like the ordinal of the run the vehicle is on in its service day: 3-4 in the early morning, 31 around 23:30, identical for consecutive payments of one card and for an on-board top up and the ride paid in the same second; and the same value at the same hour on different days (line 31: 23 at 20:41 and 20:43, 31 at 23:31 and 23:37, three weeks apart). Unlikely the stop's position on the route (1st = 11, 5th = 8, 15th = 13) and the passenger's order at the stop (1st = 8, 13, 17, 23; 5th = 18, 23).

On the tram the byte is something else: Avanza cards get a value that steps by one per validation (211, 212, 213 in a four second burst) and spans the whole byte, the Lazo card got 1 on every tram ride and the personal card 0 on its non-Avanza rides.

### Route ids 152, 169 and 251

These three route ids are not in Avanza's feed, whose ids stop at 210. The one paid ride on 169 cost €1.25 instead of €0.55, its stop ids (5, 8963) are in neither the urban nor the tram space, and a second validation twelve minutes later was free with `00` in [04], which looks like a check-out. The interurban CTAZ buses and Cercanías are the candidates, neither publishes an accessible feed.

### Directions

Terminal stations are known for the tram and buses 22 and 31 from stops that only serve one direction. Bus 35 runs Parque Goya to Seminario, but the 2013 route data lists the two rides used to test it in the same half of the route while the card gave them opposite directions, so which end is `01` is open. Bus 51 and the ring buses are untested.

### Stop ids

The urban stop id in [05-06] is a location, e.g. `8180` in both directions for bus 31, unrelated to the `PA` + number in Zaragoza pole GTFS feed. Mapped values so far:

| Line | 05-06 bytes | 09 byte | Pole | Name                            |
| ---- | ----------- | ------- | ---- | ------------------------------- |
| 22   | `81AF`      | `12`    | 676  | P. María Agustín 37             |
| 35   | `804C`      | `11`    | 471  | Fueros de Aragón 15             |
| 31   | `81DB`      | `0B`    | 3071 | Av. de Madrid / Aljafería       |
| 35   | `8099`      | `09`    | 707  | Plaza Aragón 1 in the 2013 dump |
| 31   | `807E`      | `08`    | 147  | Av. Francisco de Goya 83        |
| 22   | `81C8`      | `0D`    | 434  | Duquesa Villahermosa 3          |

### Block 24

Empty on 5 of the 7 Avanza top up cards and on the personal card. On the other two it holds `0200`, a value that looks like [date](#date) at [02-03] repeated at [10-11], zeros, `01` at [14] and an XOR of all previous bytes at [15].

### Block 4

Empty on top up cards. On the personal card it is `0600030A1204` followed by zeros: the product ids of blocks 12 and 16 (`06`, `0A`) next to their sector numbers (`03`, `04`).

### Subscription blocks

The personal card carries two products: sector 3 a 2 day product (id `06`) bought on 2025-12-03 and never used, sector 4 a 365 day pass (id `0A`) bought the same day. The pass's last usage names a ride on route id 251 and was not moved by two later Ci2 rides, so it may only log usage on the non-Avanza network; two rides are thin evidence.

### Notes

- Free journeys might have `00` in [04]
- Top up might have `21` in the sequence byte instead of 0 to 4 in block 33
- Before a ring has wrapped, an unused archive slot can hold `00000000000000000000000000000004` instead of all zeroes

## Implementations

### JavaScript, TypeScript

[lib/javascript](lib/javascript) is the spec implementation library in JavaScript/TypeScript. Zero dependencies, compatible with Node.js >= 25, Bun, Deno and 2025 browsers, MIT License. The package is published on [npm](https://www.npmjs.com/package/zgz-transport) as `zgz-transport` and on [JSR](https://jsr.io/@hloth/zgz-transport) as `@hloth/zgz-transport`.

Start from [src/index.ts](lib/javascript/src/index.ts).

### Rust

[lib/rust](lib/rust) is the spec implementation library in Rust. Zero dependencies, `no_std` with `alloc`, MIT License. The crate is published on [crates.io](https://crates.io/crates/zgz-transport) as `zgz-transport`.

Start from [src/lib.rs](lib/rust/src/lib.rs).

### Java, Kotlin

[lib/java](lib/java) is the spec implementation library in Java 17. Zero dependencies, usable from Java, Kotlin and Android, MIT License. The library is published on [Maven Central](https://central.sonatype.com/artifact/dev.hloth/zgz-transport) as `dev.hloth:zgz-transport`.

Start from [src/main/java/dev/hloth/zgztransport/Card.java](lib/java/src/main/java/dev/hloth/zgztransport/Card.java).

## Contributing

If you'd like to contribute to the project's development, consider the following resources:

- [MifareClassicTool for Android](https://github.com/ikarus23/MifareClassicTool)
- GTFS feeds mirrored by the Mobility Database: [Avanza urban buses](https://files.mobilitydatabase.org/mdb-2773/latest.zip) and [the tram](https://files.mobilitydatabase.org/mdb-2801/latest.zip); the originals are on the [Spanish National Access Point](https://nap.transportes.gob.es/Files/Detail/975), which needs a login
- Zaragoza open data: [bus lines](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/linea-autobus) (routes last updated in 2013, pole numbers have moved since), [bus poles](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/poste) (live, with the lines serving each pole) and [tram stops](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/parada-tranvia)

The spreadsheet with publicly disclosed dumps and highlights:

<a href="https://docs.google.com/spreadsheets/d/1g89saB1URWRZLWsEJm44vJFTosIDfPkh5u8pfxPWGD0/edit">
	<img alt="Spreadsheet" src="https://git.hloth.dev/hloth/zgz-transport/raw/branch/main/docs/spreadsheet.avif" width="600" />
</a>

## See also

- [ZGZ Avanza Card Android App](https://git.hloth.dev/hloth/zgz-transport-card-android)

## Acknowledgements

Huge thanks to [li0ard](https://li0ard.rest/) for help with decoding RFIDs and dates!

## License

[MIT](./LICENSE)

## Donate

[hloth.dev/donate](https://hloth.dev/donate)
