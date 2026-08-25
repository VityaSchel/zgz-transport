/** Keys of one sector as upper case hex; a missing key is not known. */
export type SectorKeys = { a?: string; b?: string };

const AVANZA_OPERATOR: SectorKeys = { a: "04000C0F0903", b: "0B02070A0409" };
const FACTORY: SectorKeys = { a: "FFFFFFFFFFFF", b: "FFFFFFFFFFFF" };
const LAZO_SHARED: SectorKeys = { a: "4E303D402F20", b: "243372407C2E" };

const LAZO_OWN: Record<number, SectorKeys> = {
	32: { a: "216F5B212A7A", b: "44202E476E5B" },
	33: { a: "5148755C3427", b: "3C4520753758" },
	34: { b: "206F7C4C4F36" },
	35: { a: "5246612E7C4B" },
	36: { a: "354B39454861", b: "567D734C403C" },
	37: { a: "455D732C385F", b: "2426217B3B3B" },
};

/**
 * Keys of an Avanza Tarjeta Bus sector.
 * @param sector `0` to `15`.
 * @param personal Whether the card is a personal one, which keeps the operator keys on every sector.
 */
export function avanzaKeys(sector: number, personal = false): SectorKeys {
	if (sector <= 8 || personal) return AVANZA_OPERATOR;
	return { a: "A0A1A2A3A4A5", b: "B0B1B2B3B4B5" };
}

/**
 * Keys of a Lazo card sector.
 * @param sector `0` to `39`.
 */
export function lazoKeys(sector: number): SectorKeys {
	if (sector < 32) return LAZO_SHARED;
	return LAZO_OWN[sector] ?? FACTORY;
}
