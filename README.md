# Zaragoza & Aragon Transport Card Spec

> [!IMPORTANT]
> ⚠️ **Aviso legal:** Este repositorio es un proyecto de investigación de seguridad independiente.  \
> No está destinado a cometer fraude ni a facilitar el uso indebido del transporte público. \
> No se proporcionará asistencia para ningún uso ilícito. \
> Consulta [LEGAL.md](./LEGAL.md) para el descargo de responsabilidad completo.

Zaragoza and Aragon Avanza/Lazo bus & tram public transport card full up-to-date specification, reverse engineered from multiple MIFARE Classic card dumps. This project intends to be a security research paper, publicly available for anyone for free and is not qualified as a guide. There are no step-by-step instructions on how to commit fraud.

- [Zaragoza \& Aragon Transport Card Spec](#zaragoza--aragon-transport-card-spec)
	- [Getting started](#getting-started)
	- [Keys](#keys)
	- [Sectors](#sectors)
	- [Blocks](#blocks)
	- [Definitions](#definitions)
		- [Card ID](#card-id)
		- [Card type](#card-type)
		- [Balance](#balance)
		- [Transaction log](#transaction-log)
			- [Ride (spend)](#ride-spend)
			- [Top up](#top-up)
			- [All types](#all-types)
		- [Date](#date)
		- [Subscription metadata](#subscription-metadata)
		- [Subscription](#subscription)
		- [Blocks 5 and 10](#blocks-5-and-10)
	- [Implementations](#implementations)
		- [JavaScript/TypeScript](#javascripttypescript)
	- [Contributing](#contributing)
	- [See also](#see-also)
	- [Acknowledgements](#acknowledgements)
	- [License](#license)
	- [Donate](#donate)


## Getting started

Zaragoza and Aragon public transport uses Avanza and Lazo cards, both of which are Mifare Classic, meaning they're vulnerable to Crypto-1 attack. The [keys](#keys) for cards can be dumped using a Proxmark device, but since they're static and well-known, you can use those provided in this repository.



The card has 16 sectors, each with 4 blocks of 16 bytes each. Each sector has two keys (Key A and Key B) that control access to the blocks within that sector.

## Keys

Avanza Tarjeta Bus:

| Sectors       | Key A          | Key B          |
| ------------- | -------------- | -------------- |
| 0-8           | `04000C0F0903` | `0B02070A0409` |
| 9-15 (unused) | `A0A1A2A3A4A5` | `B0B1B2B3B4B5` |
|               |                |                |

Lazo card:

| Sectors        | Key A          | Key B          |
| -------------- | -------------- | -------------- |
| 0-31           | `4E303D402F20` | `243372407C2E` |
| 32             | `216F5B212A7A` | `44202E476E5B` |
| 33             | `5148755C3427` | `3C4520753758` |
| 34             | Unknown        | `206F7C4C4F36` |
| 35             | `5246612E7C4B` | Unknown        |
| 36             | `354B39454861` | `567D734C403C` |
| 37             | `455D732C385F` | `2426217B3B3B` |
| 38-39 (unused) | `FFFFFFFFFFFF` | `FFFFFFFFFFFF` |
|                |                |                |

## Sectors

| Sector | Description                      |
| ------ | -------------------------------- |
| 0      | Manufacturer block, IDs          |
| 1      | Appears to be latest transaction |
| 2      | Balance (blocks 8 and 9)         |
| 3      | Empty on top up cards            |
| 4      | Empty on top up cards            |
| 5      | Empty                            |
| 6      | Empty                            |
| 7      | Appears to be transaction logs   |
| 8      | Appears to be transaction logs   |
| 9      | Unused                           |
| 10     | Unused                           |
| 11     | Unused                           |
| 12     | Unused                           |
| 13     | Unused                           |
| 14     | Unused                           |
| 15     | Unused                           |
|        |                                  |

## Blocks

| Sector | Block | Description                                                                                                                                                                                     | Template                           | Access Conditions    |
| ------ | ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | -------------------- |
| 0      | 0     | [00-03] RFID's UID<br>[04] BCC (checksum byte)<br>[05] SAK (always `88` for MIFARE Classic 1K)<br>[15] last two digits of year the card was manufactured in (i.e. `20` for 2020, `25` for 2025) | `..,,..,,..880400C8,,0020000000..` | Read-only            |
|        | 1     | [Card type](#card-type)                                                                                                                                                                         | `..,,..000000000000000000000000,,` | Only Key B can write |
|        | 2     | [Card ID](#card-id)                                                                                                                                                                             | `42,,..,,..00000000000000000000..` | Read-only            |
|        | 3     | [0th sector's trailer block](https://github.com/andrea-peter/nfc_mifare_classic_notes/blob/main/mifare-classic.md#sector-trailer-block)                                                         | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 1      | 4     | Empty on top-up cards<br>[00-14] unknown<br>[15] XOR of all previous bytes                                                                                                                      | `00000000000000000000000000000000` | Only Key B can write |
|        | 5     | See [Blocks 5 and 10](#blocks-5-and-10)                                                                                                                                                         | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 6     | *Appears* to always be empty                                                                                                                                                                    | `00000000000000000000000000000000` | No restrictions      |
|        | 7     | 1st sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 2      | 8     | [Balance](#balance)                                                                                                                                                                             | `..,,0000..,,FFFF..,,000002FD02FD` | Value block[^1]      |
|        | 9     | Always has the same value as block 8                                                                                                                                                            | `..,,0000..,,FFFF..,,000002FD02FD` | Value block[^1]      |
|        | 10    | Empty if new card, correlates to block 5 (see [Blocks 5 and 10](#blocks-5-and-10))<br />Always `000000000000000A000000000000000A` on unlimited personal cards                                   | `..,,..,,..,,010200..,,00006300..` | No restrictions      |
|        | 11    | 2nd sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 3      | 12    | [Subscription metadata](#subscription-metadata)                                                                                                                                                 | `00000000000000000000000000000000` | Only Key B can write |
|        | 13    | [Subscription](#subscription) on personal unlimited cards                                                                                                                                       | `00000000000000000000000000000000` | Only Key B can write |
|        | 14    | See block 13                                                                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 15    | 3rd sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 4      | 16    | See block 12                                                                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 17    | See block 13                                                                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 18    | See block 13                                                                                                                                                                                    | `00000000000000000000000000000000` | Only Key B can write |
|        | 19    | 4th sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 5      | 20    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 21    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 22    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 23    | 5th sector's trailer blocks                                                                                                                                                                     | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 6      | 24    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 25    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 26    | Empty                                                                                                                                                                                           | `00000000000000000000000000000000` | Only Key B can write |
|        | 27    | 6th sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 7      | 28    | [Transaction logs](#transaction-log); value of block 5 right before overwriting it; each subsequent write adds an entry to blocks 29, then 30, then 32, then 33                                 | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 29    | See block 28                                                                                                                                                                                    | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 30    | See block 28                                                                                                                                                                                    | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 31    | 7th sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
| 8      | 32    | See block 28                                                                                                                                                                                    | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 33    | See block 28                                                                                                                                                                                    | `020002..,,..,,..,,..,,..,,..,,..` | No restrictions      |
|        | 34    | Expiration date, encoding unknown; always `00000000FFFFFFFF0000000000FF00FF` on top up cards                                                                                                    | `00000000FFFFFFFF0000000000FF00FF` | Value block[^1]      |
|        | 35    | 8th sector's trailer block                                                                                                                                                                      | `04000C0F0903..,,..,,0B02070A0409` | *Trailer*            |
|        |       |                                                                                                                                                                                                 |                                    |                      |

[^1]: Value blocks have the following restrictions: Key A can read, decrement, restore and transfer, Key B can also write and increment.

Value blocks 8, 9 and 34 store a 32-bit integer three times for redundancy: the value, its bitwise complement, then the value again.

The balance is stored in blocks 8 and 9 for redundancy.

## Definitions

### Card ID

- [00-01] ASCII prefix (`4245` for `BE`, `4250` for `BP`, `4354` for `CT`)
- [02-04] Zaragoza card ID number (together forming prefix + number)
- [05-14] empty, zeroed
- [15] XOR of all previous bytes

See [src/id.ts](./src/id.ts) for encoder/decoder implementation and [test/id.test.ts](./test/id.test.ts) for test cases in JavaScript/TypeScript.

### Card type

- [00-14] Card type
- [15] XOR of all previous bytes

| Card type                     | Value                            |
| ----------------------------- | -------------------------------- |
| Balance top-up Avanza card    | `02699F000000000000000000000000` |
| Personal expiring Avanza card | `0A9775000000000000000000000000` |
| Balance top-up Lazo card      | `0D371F000000000000000000000000` |
|                               |                                  |

See [src/type.ts](./src/type.ts) for encoder/decoder implementation and [test/type.test.ts](./test/type.test.ts) for test cases in JavaScript/TypeScript.

### Balance

€1.00 = 1000 units. For example, a balance of €5.00 would be stored as `8813000077ECFFFF8813000002FD02FD` in blocks 8 and 9:

1. Convert `5.00` to units: `5000`
2. Convert `5000` to hexadecimal: `1388`
3. Convert to little-endian: `8813`
4. Calculate the complement: `77ec` (flip every bit)
5. Write the value, complement, and value again, then append the static address `02FD02FD` and write to blocks 8 and 9

- Bytes 00-03: `88130000` -> little-endian -> `5000`
- Bytes 04-07: `77ECFFFF` -> complement of `5000`
- Bytes 08-11: `88130000` -> value repeated
- Bytes 12-13: `02FD` -> address bytes (block pointer for transfer operations)
- Bytes 14-15: `02FD` -> address bytes repeated

Balance on personal unlimited cards is always `00000000FFFFFFFF0000000002FD02FD` (zero).

See [src/balance.ts](./src/balance.ts) for encoder/decoder implementation and [test/balance.test.ts](./test/balance.test.ts) for test cases in JavaScript/TypeScript.

### Transaction log

Transaction logs are stored in sectors 1 (block 5), 7 (blocks 28-30) and 8 (blocks 32-33). Each log entry is 16 bytes, so each block can store one log entry.

#### Ride (spend)

- [00] The first byte of [Card type](#card-type), e.g. `02` or `0D`
- [01-02] Unknown; `0002` or sometimes `0000` for Avanza top up cards; `0200` or sometimes `0100` for Avanza personal cards
- [03] Unknown constant, could be a fare id or season id; `88` for Avanza top up cards; `00` for Avanza personal cards
- [04] Consecutive payments counter starting from 1 (for transactions paid in the same terminal in a row)
- [05-06] With 09 most likely stop ID
- [07] Most likely bus line number, but unknown how to decode lines > 255 and trams
- [08] Line direction (always either `01` or `02`)
- [09] With 05-06 most likely stop ID

| Line | 05-06 bytes | 09 byte | Actual [stop ID](https://www.zaragoza.es/sede/servicio/urbanismo-infraestructuras/transporte-urbano/linea-autobus) |
| ---- | ----------- | ------- | ------------------------------------------------------------------------------------------------------------------ |
| 22   | `81AF`      | `12`    | 676                                                                                                                |
| 35   | `804C`      | `11`    | 471                                                                                                                |
| 31   | `81DB`      | `0B`    | 3071                                                                                                               |
| 35   | `8099`      | `09`    | 707                                                                                                                |
| 31   | `807E`      | `08`    | 147                                                                                                                |
| 22   | `81C8`      | `0D`    | 434                                                                                                                |

#### Top up

- [00-02] Always `020013`
- [03-09] Unknown, e.g. `88001F2C000800` (for €5.00 top up)

#### All types

- [10-11] Transaction [date](#dates)
- [12] Hour (0-23)
- [13] Minute (0-59)
- [14] Second (0-59)
- [15] Sequence counter that increments from 0 to 4 (both ends inclusive) and then loops back to 0 correlating with transactions order

See [src/transaction.ts](./src/transaction.ts) for encoder/decoder implementation and [test/transaction.test.ts](./test/transaction.test.ts) for test cases in JavaScript/TypeScript.

### Date

Mad respect to [li0ard](https://li0ard.rest/) for figuring out the date encoding!

Dates are composed of two bytes (16 bits), which when converted to binary:
- the first 7 bits represent the year (based 0 at 2000)
- 4 bits for month (1-12)
- 5 bits for day

Examples:

`34 4E` -> `0011010001001110` -> `0011010 0010 01110`
- Year: `0011010` -> 26 + 2000 -> 2026
- Month: `0010` -> 2 (February)
- Day: `01110` -> 14

See [src/date.ts](./src/date.ts) for encoder/decoder implementation and [test/date.test.ts](./test/date.test.ts) for test cases in JavaScript/TypeScript.

### Subscription metadata

[WIP](#contributing)

- [00] Unknown
- [01] Unknown, appears to always be `01`
- [02-03] Purchase [date](#date)
- [04-07] Unknown, appears to always be `00210000`
- [08-09] Validity days (e.g. `016D` for 365 days)
- [10-14] Unknown, appears to always be `0021000000`
- [15] XOR of all previous bytes

See [src/subscription-metadata.ts](./src/subscription-metadata.ts) for encoder/decoder implementation and [test/subscription-metadata.test.ts](./test/subscription-metadata.test.ts) for test cases in JavaScript/TypeScript.

### Subscription

[WIP](#contributing)

- [00-01] Subscription start [date](#date)
- [02-03] Subscription end date
- [04-05] Unknown, appears to always be `0000`
- [06-09] Unknown
- [10-11] [Date](#date) of the last usage
- [12] Hour of the last usage (0-23)
- [13] Minute of the last usage (0-59)
- [14] Second of the last usage (0-59)
- [15] XOR of all previous bytes

See [src/subscription.ts](./src/subscription.ts) for encoder/decoder implementation and [test/subscription.test.ts](./test/subscription.test.ts) for test cases in JavaScript/TypeScript.

### Blocks 5 and 10

Block 5 appears to be a [transaction log](#transaction-log). Block 10 stores complimentary data to block 5, but unknown how it correlates.

```text
2021 dump:

    someid v1 cp ???? ln dr ?? date HH MM SS sq
 5: 020002 F8 01 0640 D2 02 25 2ADC 09 1B 2F 01
10: 00 00 2ADC 09 1B 01 0200 D2 02 00006300 54
    prJrn date HH MM SS ???? ln dr constant xr

    someid v1 cp ???? ln dr ?? date HH MM SS sq
 5: 020002 F8 01 0320 D2 01 FE 2AD8 13 2D 12 01
10: 00 00 2AD8 13 2D 01 0200 D2 01 00006300 7F
    prJrn date HH MM SS ???? ln dr constant xr

    someid v1 cp ???? ln dr ?? date HH MM SS sq
 5: 020002 F8 01 05DC D2 02 6B 2AD9 0D 33 3B 02
10: D2 01 2AD9 0D 33 01 0200 D2 02 00006300 AE
    prJrn date HH MM SS ???? ln dr constant xr

    someid v1 cp ???? ln dr ?? date HH MM SS sq
 5: 020002 F8 01 02BC D2 01 99 2AD9 10 26 0D 03
10: D2 02 2AD9 10 26 01 0200 D2 01 00006300 A6
    prJrn date HH MM SS ???? ln dr constant xr

2026 dump:

    someid v1 cp ???? ln dr ?? date HH MM SS sq
 5: 020002 26 01 80CE 16 01 0D 3454 10 2D 24 03
10: 23 02 3454 10 2D 01 0200 16 01 00006300 0B
    prJrn date HH MM SS ???? ln dr constant xr

someid = always 020002 for top up cards, always 0A0200 for unlimited cards
    v1 = card-specific constant, could be a fare id or a season id
	cp = consecutive payments counter (same card, same terminal, transactions in a row)
	ln = line number (decimal)
	dr = direction (either `01` or `02`)
  date = binary-encoded date (7 bits year 2000-based, 4 bits month 1-based, 5 bits day)
    HH = BCD hour
    MM = BCD minute
    SS = BCD second
    sq = sequence counter going from 0 
	xr = XOR of all previous bytes
 prJrn = previous journey: line number (decimal) and direction (either `01` or `02`), always 0000 on first ever transaction
```

See [src/block10.ts](./src/block10.ts) for encoder/decoder implementation and [test/block10.test.ts](./test/block10.test.ts) for test cases in JavaScript/TypeScript.

## Implementations

### JavaScript/TypeScript

The JavaScript/TypeScript/Bun library is published in [npm](https://www.npmjs.com/package/zgz-transport):

```bash
bun add zgz-transport
# npm install zgz-transport
# yarn install zgz-transport
# pnpm install zgz-transport
```

And [JSR](https://jsr.io/@hloth/zgz-transport):

```bash
bunx jsr add @hloth/zgz-transport
# npx jsr add @hloth/zgz-transport
# deno add jsr:@hloth/zgz-transport
# yarn add jsr:@hloth/zgz-transport
# pnpm add jsr:@hloth/zgz-transport
```

Use [index.ts](src/index.ts) as a starting point.

## Contributing

If you'd like to contribute to the project's development, consider the following resources:

- [MifareClassicTool for Android](https://github.com/ikarus23/MifareClassicTool)

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
