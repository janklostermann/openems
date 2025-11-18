package io.openems.edge.evse.simulator.bridge;

import io.openems.edge.evse.simulator.abl.AblRegisterMapper;
import io.openems.edge.evse.simulator.alfen.AlfenRegisterMapper;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;
import io.openems.edge.evse.simulator.heidelberg.HeidelbergRegisterMapper;
import io.openems.edge.evse.simulator.keba.KebaRegisterMapper;

/**
 * Supported EVSE device types for simulation.
 */
public enum DeviceType {
	ALFEN("Alfen Eve"),
	HEIDELBERG("Heidelberg Energy Control"),
	ABL("ABL eMH1"),
	KEBA("Keba P30");

	private final String displayName;

	private DeviceType(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Gets the display name.
	 *
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Creates the appropriate {@link ModbusRegisterMapper} for this device type.
	 *
	 * @return a new register mapper instance
	 */
	public ModbusRegisterMapper createMapper() {
		return switch (this) {
		case ALFEN -> new AlfenRegisterMapper();
		case HEIDELBERG -> new HeidelbergRegisterMapper();
		case ABL -> new AblRegisterMapper();
		case KEBA -> new KebaRegisterMapper();
		};
	}
}
