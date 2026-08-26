import { expect, it } from "bun:test";
import { decodeId, encodeId } from "../src/id.ts";

const cases = [
	{ encoded: "42453227430000000000000000000051", decoded: "BE322743" },
	{ encoded: "42453227420000000000000000000050", decoded: "BE322742" },
	{ encoded: "4245739977000000000000000000009A", decoded: "BE739977" },
	{ encoded: "42504232450000000000000000000027", decoded: "BP423245" },
	{ encoded: "435443048600000000000000000000D6", decoded: "CT430486" },
];

it("decodes ids", () => {
	for (const { encoded, decoded } of cases) {
		expect(decodeId(Uint8Array.fromHex(encoded))).toEqual(decoded);
	}
});

it("encodes ids", () => {
	for (const { encoded, decoded } of cases) {
		expect(encodeId(decoded)).toEqual(Uint8Array.fromHex(encoded));
	}
});
