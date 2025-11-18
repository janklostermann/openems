package io.openems.edge.evse.simulator.core;

/**
 * Charging states according to IEC 61851-1.
 */
public enum ChargePointState {
	A("Not connected"), //
	B("Connected, not ready"), //
	C("Charging"), //
	D("Charging with ventilation"), //
	E("Error"), //
	F("Not available");

	private final String description;

	private ChargePointState(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}
}
