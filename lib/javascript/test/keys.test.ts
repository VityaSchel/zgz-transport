import { expect, it } from "bun:test";
import { avanzaKeys, lazoKeys } from "../src/keys";

it("returns Avanza keys per sector and product", () => {
	expect(avanzaKeys(0).a).toBe("04000C0F0903");
	expect(avanzaKeys(9).a).toBe("A0A1A2A3A4A5");
	expect(avanzaKeys(9, true).a).toBe("04000C0F0903");
});

it("returns Lazo keys per sector", () => {
	expect(lazoKeys(31)).toEqual({ a: "4E303D402F20", b: "243372407C2E" });
	expect(lazoKeys(34)).toEqual({ b: "206F7C4C4F36" });
	expect(lazoKeys(39).a).toBe("FFFFFFFFFFFF");
});
