# zgz-transport

Encoder and decoder for the Zaragoza and Aragon transport cards (Avanza Tarjeta Bus, Lazo), based on the reverse-engineered card spec at [https://git.hloth.dev/hloth/zgz-transport](https://git.hloth.dev/hloth/zgz-transport).

Zero dependencies, TypeScript definitions, 3.4KB (gzipped), MIT License. Supports Node.js >= 25.0, Bun, Deno and 2025 browsers.

## Install

**From [npm](https://www.npmjs.com/package/zgz-transport):**

```sh
# npm:
npm install zgz-transport

# Bun:
bun add zgz-transport

# pnpm:
pnpm add zgz-transport

# Yarn:
yarn add zgz-transport
```

**From [JSR](https://jsr.io/@hloth/zgz-transport):**

```sh
# Deno:
deno add jsr:@hloth/zgz-transport

# Bun:
bun x jsr add @hloth/zgz-transport

# npm:
npx jsr add @hloth/zgz-transport

# pnpm:
pnpm add jsr:@hloth/zgz-transport

# Yarn:
yarn add jsr:@hloth/zgz-transport
```

## Usage

Decode a dump read with MIFARE Classic Tool or a Proxmark from `Uint8Array` or `Buffer`:

```ts
import fs from "node:fs/promises";
import { decodeCard, routeName } from "zgz-transport";

// Bun:
const card = decodeCard(await Bun.file("dump.bin").bytes());
// Node.js:
const card = decodeCard(await fs.readFile("dump.bin"));

console.log(card.id, card.balance / 1000);
for (const t of card.transactions) {
	console.log(t.createdAt, t.kind, routeName(t.route), t.amount);
}
```

Every block structure has its own `decode*`/`encode*` pair, see [src/index.ts](https://git.hloth.dev/hloth/zgz-transport/src/branch/main/lib/javascript/src/index.ts):

```ts
import { encodeBalance, encodeTransaction } from "zgz-transport";

const block8 = encodeBalance(4450);
const block5 = encodeTransaction({
	cardType: "AvanzaTopUp",
	networkFlag: 0,
	amount: 550,
	consecutivePayments: 1,
	stop: { network: "urban", id: 500 },
	route: 31,
	kind: "journey",
	direction: 2,
	dutyTrip: 7,
	createdAt: { year: 2026, month: 8, day: 26, hour: 9, minute: 41, second: 27 },
	sequence: 1,
});
```

## License

[MIT](../../LICENSE)

## Donate

[hloth.dev/donate](https://hloth.dev/donate)
