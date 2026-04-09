package io.openems.edge.evse.chargepoint.abl.enums;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;

/**
 * ABL eMH1 hardware variant — encodes all parameters needed to derive the
 * charge-point abilities and power limits. Product numbers from
 * <em>Technische Daten</em> (ABL_eMH1_MA_RZ_05_D-web.pdf, pp. 81–84).
 */
public enum AblEMh1HardwareType {

	/** 1W36P1 — 3.6 kW, 1-phase, 16 A, Typ-2-Kabel. */
	EMH1_1P_36KW_CABLE("1W36P1", SINGLE_PHASE, 16, 3.6, "Typ-2-Kabel"),
	/** 1W7221 / 1W72P2 — 7.2 kW, 1-phase, 32 A, Typ-2-Steckdose. */
	EMH1_1P_72KW_SOCKET("1W7221", SINGLE_PHASE, 32, 7.2, "Typ-2-Steckdose"),
	/** 1W7201 / 1W72P1 — 7.2 kW, 1-phase, 32 A, Typ-2-Kabel. */
	EMH1_1P_72KW_CABLE("1W7201", SINGLE_PHASE, 32, 7.2, "Typ-2-Kabel"),
	/** 1W7241 — 7.2 kW, 1-phase, 32 A, Typ-1-Kabel. */
	EMH1_1P_72KW_CABLE_T1("1W7241", SINGLE_PHASE, 32, 7.2, "Typ-1-Kabel"),
	/** 1W1121 / 1W11K2 / 1W11N2 — 11 kW, 3-phase, 16 A, Typ-2-Steckdose. */
	EMH1_3P_11KW_SOCKET("1W1121", THREE_PHASE, 16, 11.0, "Typ-2-Steckdose"),
	/** 1W1101 / 1W11K1 / 1W11N1 — 11 kW, 3-phase, 16 A, Typ-2-Kabel. */
	EMH1_3P_11KW_CABLE("1W1101", THREE_PHASE, 16, 11.0, "Typ-2-Kabel"),
	/** 1W2221 — 22 kW, 3-phase, 32 A, Typ-2-Steckdose. */
	EMH1_3P_22KW_SOCKET("1W2221", THREE_PHASE, 32, 22.0, "Typ-2-Steckdose"),
	/** 1W2201 — 22 kW, 3-phase, 32 A, Typ-2-Kabel. */
	EMH1_3P_22KW_CABLE("1W2201", THREE_PHASE, 32, 22.0, "Typ-2-Kabel");

	private final String productNumber;
	private final SingleOrThreePhase phase;
	private final int maxCurrentInAmpere;
	private final double ratedPowerKw;
	private final String connectionType;

	private AblEMh1HardwareType(String productNumber, SingleOrThreePhase phase, int maxCurrentInAmpere,
			double ratedPowerKw, String connectionType) {
		this.productNumber = productNumber;
		this.phase = phase;
		this.maxCurrentInAmpere = maxCurrentInAmpere;
		this.ratedPowerKw = ratedPowerKw;
		this.connectionType = connectionType;
	}

	/**
	 * Gets the primary product number.
	 *
	 * @return the product number string (e.g. "1W2201")
	 */
	public String getProductNumber() {
		return this.productNumber;
	}

	/**
	 * Gets the phase count ({@link SingleOrThreePhase}).
	 *
	 * @return the phase
	 */
	public SingleOrThreePhase getPhase() {
		return this.phase;
	}

	/**
	 * Gets the hardware maximum charging current in Ampere.
	 *
	 * @return the maximum current in A
	 */
	public int getMaxCurrentInAmpere() {
		return this.maxCurrentInAmpere;
	}

	/**
	 * Gets the rated power in kW.
	 *
	 * @return the rated power
	 */
	public double getRatedPowerKw() {
		return this.ratedPowerKw;
	}

	/**
	 * Returns the UI label in the format used by the ABL data sheet.
	 *
	 * <p>
	 * Example: {@code "1W2201 — 22.0 kW, 3-phase, 32 A, Typ-2-Kabel"}
	 *
	 * @return the label
	 */
	public String getLabel() {
		return this.productNumber + " — " + this.ratedPowerKw + " kW, "
				+ this.phase.count + "-phase, " + this.maxCurrentInAmpere + " A, " + this.connectionType;
	}
}
