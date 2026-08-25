import { expect, it } from "bun:test";
import {
	decodeJourneySummary,
	encodeJourneySummary,
	PERSONAL_JOURNEY_SUMMARY,
} from "../src/journey-summary";
import { journeySummaries } from "./fixtures/journey-summaries";

it("decodes journey summaries", () => {
	for (const { encoded, decoded } of journeySummaries) {
		expect(decodeJourneySummary(Uint8Array.fromHex(encoded))).toEqual(decoded);
	}
});

it("encodes journey summaries", () => {
	for (const { encoded, decoded } of journeySummaries) {
		expect(encodeJourneySummary(decoded)).toEqual(Uint8Array.fromHex(encoded));
	}
});

it("rejects the personal card constant", () => {
	expect(() => decodeJourneySummary(PERSONAL_JOURNEY_SUMMARY)).toThrow();
});
