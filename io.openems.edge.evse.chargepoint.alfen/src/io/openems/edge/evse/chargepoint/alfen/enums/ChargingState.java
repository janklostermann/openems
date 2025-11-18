package io.openems.edge.evse.chargepoint.alfen.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Charging state according to IEC 61851-1.
 */
public enum ChargingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	A(0, "Not connected"), //
	B(1, "Connected, not ready"), //
	C(2, "Charging"), //
	D(3, "Charging with ventilation"), //
	E(4, "Error"), //
	F(5, "Not available"); //

	private final int value;
	private final String name;

	private ChargingState(int value, String name) {
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
	 * Parses a charging state string from the Alfen wallbox.
	 *
	 * @param state the state string (e.g., "A", "B1", "C2", etc.)
	 * @return the {@link ChargingState}
	 */
	public static ChargingState fromString(String state) {
		if (state == null || state.isEmpty()) {
			return UNDEFINED;
		}
		// Extract the first character which represents the main state
		char firstChar = state.trim().toUpperCase().charAt(0);
		return switch (firstChar) {
		case 'A' -> A;
		case 'B' -> B;
		case 'C' -> C;
		case 'D' -> D;
		case 'E' -> E;
		case 'F' -> F;
		default -> UNDEFINED;
		};
	}
}
