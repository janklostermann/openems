package io.openems.edge.evse.chargepoint.abl.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Log verbosity level for ABL eMH1 charge-point driver.
 */
public enum LogVerbosity implements OptionsEnum {

	NONE(0, "None"), //
	READS_AND_WRITES(1, "Reads and Writes"), //
	READS_AND_WRITES_VERBOSE(2, "Reads, Writes and Debug Frames"); //

	private final int value;
	private final String name;

	private LogVerbosity(int value, String name) {
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
		return NONE;
	}
}
