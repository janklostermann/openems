package io.openems.edge.evse.chargepoint.abl.enums;

import io.openems.common.types.OptionsEnum;

/**
 * ABL eMH1 fault state — reported on {@link
 * io.openems.edge.evse.chargepoint.abl.EvseChargePointAbl.ChannelId#FAULT_STATE}
 * when the device is in any F-state.
 *
 * <p>
 * State byte encoding follows the same hex-digit rule as {@link AblEMh1State}:
 * F10→0xFA, F11→0xFB, F14→0xFE.
 */
public enum AblEMh1FaultState implements OptionsEnum {

	UNDEFINED(-1, "No fault"), //
	/** F1 — Unintended closed contact (welding detection tripped). */
	F1(0xF1, "Welding detection tripped"), //
	/** F2 — Internal error. */
	F2(0xF2, "Internal error"), //
	/** F3 — DC residual current detected (RDC-MD tripped). */
	F3(0xF3, "DC residual current detected"), //
	/** F4 — Upstream communication timeout (OpenEMS / SBC lost). */
	F4(0xF4, "Upstream communication timeout"), //
	/** F5 — Lock of socket failed. */
	F5(0xF5, "Lock of socket failed"), //
	/** F6 — CS (Cable Signal) out of range. */
	F6(0xF6, "CS out of range"), //
	/** F7 — State D requested by EV (ventilation required; not supported by eMH1). */
	F7(0xF7, "State D (ventilation) requested"), //
	/** F8 — CP (Control Pilot) out of range. */
	F8(0xF8, "CP out of range"), //
	/** F9 — Overcurrent detected. */
	F9(0xF9, "Overcurrent detected"), //
	/** F10 — Temperature outside limits. */
	F10(0xFA, "Temperature outside limits"), //
	/** F11 — Unintended opened contact. */
	F11(0xFB, "Unintended opened contact"), //
	/** F14 — Upstream timeout (legacy, when system-flags bit15=0). */
	F14(0xFE, "Upstream timeout (legacy F14)"); //

	private final int value;
	private final String name;

	private AblEMh1FaultState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	/**
	 * Returns the {@link AblEMh1FaultState} corresponding to an
	 * {@link AblEMh1State} F-state, or {@link #UNDEFINED} if the state is not a
	 * fault.
	 *
	 * @param state the device state
	 * @return the corresponding fault state
	 */
	public static AblEMh1FaultState fromDeviceState(AblEMh1State state) {
		if (!state.isFault()) {
			return UNDEFINED;
		}
		for (var fs : values()) {
			if (fs.value == state.getValue()) {
				return fs;
			}
		}
		return UNDEFINED;
	}
}
