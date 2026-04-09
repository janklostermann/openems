package io.openems.edge.evse.chargepoint.abl.enums;

import io.openems.common.types.OptionsEnum;

/**
 * ABL eMH1 device state.
 *
 * <p>
 * The state byte in registers R5 (0x002E low byte) and R3 (0x0034 high byte)
 * directly encodes the state name: A1→0xA1, B2→0xB2, C2→0xC2, F4→0xF4.
 * For states F10 and above, the sub-code uses hexadecimal digits:
 * F10→0xFA (A=10), F11→0xFB, F14→0xFE.
 *
 * <p>
 * Spec reference: ABL Modbus-ASCII interface description, §3.4 "Definition of states".
 */
public enum AblEMh1State implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	/** E0 — Outlet physically disabled. */
	E0(0xE0, "Outlet disabled"), //
	/** E1 — Production test mode (not reachable in normal field operation). */
	E1(0xE1, "Production test"), //
	/** E2 — Outlet enabled, EVCC setup mode; no EV connected. */
	E2(0xE2, "Outlet enabled (EVCC setup)"), //
	/** E3 — Bus idle (not reachable in normal field operation). */
	E3(0xE3, "Bus idle"), //
	/** A1 — Outlet enabled; waiting for EV to connect. */
	A1(0xA1, "Waiting for EV"), //
	/** B1 — EV connected; charge permission not yet granted. */
	B1(0xB1, "EV requesting charge"), //
	/** B2 — Permission granted; EV not yet drawing current (I = 0). */
	B2(0xB2, "Permission granted, not charging"), //
	/** C2 — Charging; EV drawing current. */
	C2(0xC2, "Charging"), //
	/** C3 — Charging at reduced current due to error F16/F17. */
	C3(0xC3, "Charging, current reduced (F16/F17)"), //
	/** C4 — Charging at reduced current due to load imbalance F15. */
	C4(0xC4, "Charging, current reduced (imbalance F15)"), //
	/** F1 — Unintended closed contact (welding detection tripped). */
	F1(0xF1, "Welding detection tripped"), //
	/** F2 — Internal error. */
	F2(0xF2, "Internal error"), //
	/** F3 — DC residual current detected (RDC-MD tripped). */
	F3(0xF3, "DC residual current detected"), //
	/** F4 — Upstream communication timeout. */
	F4(0xF4, "Upstream communication timeout"), //
	/** F5 — Lock of socket failed. */
	F5(0xF5, "Lock of socket failed"), //
	/** F6 — CS (Cable Signal) out of range. */
	F6(0xF6, "CS out of range"), //
	/** F7 — State D requested by EV (ventilation required; not supported). */
	F7(0xF7, "State D (ventilation) requested"), //
	/** F8 — CP (Control Pilot) out of range. */
	F8(0xF8, "CP out of range"), //
	/** F9 — Overcurrent detected. */
	F9(0xF9, "Overcurrent detected"), //
	/** F10 — Temperature outside limits. Sub-code byte = 0xFA (A=10 in hex). */
	F10(0xFA, "Temperature outside limits"), //
	/** F11 — Unintended opened contact. Sub-code byte = 0xFB (B=11 in hex). */
	F11(0xFB, "Unintended opened contact"), //
	/** F14 — Upstream timeout (legacy fault code when system-flags bit15=0). Sub-code = 0xFE. */
	F14(0xFE, "Upstream timeout (legacy F14)"); //

	private final int value;
	private final String name;

	private AblEMh1State(int value, String name) {
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
	 * Returns true if an EV cable is connected in this state (IEC 61851 B or C
	 * states).
	 *
	 * @return true if EV is connected
	 */
	public boolean isEvConnected() {
		return switch (this) {
		case B1, B2, C2, C3, C4 -> true;
		default -> false;
		};
	}

	/**
	 * Returns true if the charge-point is ready to accept a charging setpoint.
	 *
	 * @return true if ready for charging
	 */
	public boolean isReadyForCharging() {
		return switch (this) {
		case B2, C2, C3, C4 -> true;
		default -> false;
		};
	}

	/**
	 * Returns true if this is an F-state (fault).
	 *
	 * @return true if fault
	 */
	public boolean isFault() {
		return this.value >= 0xF0 && this != UNDEFINED;
	}
}
