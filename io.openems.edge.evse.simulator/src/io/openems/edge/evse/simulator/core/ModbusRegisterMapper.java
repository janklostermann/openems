package io.openems.edge.evse.simulator.core;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;

/**
 * Interface for device-specific Modbus register mapping.
 *
 * <p>
 * Implementations translate the generic {@link ChargePointSimulatorCore} state
 * into device-specific Modbus register values.
 */
public interface ModbusRegisterMapper {

	/**
	 * Update the {@link SimpleProcessImage} registers based on simulator state.
	 *
	 * <p>
	 * This is called after each state change to synchronize the Modbus registers
	 * with the current simulator state.
	 *
	 * @param processImage the {@link SimpleProcessImage} to update
	 * @param core         the {@link ChargePointSimulatorCore} with current state
	 */
	void updateRegisters(SimpleProcessImage processImage, ChargePointSimulatorCore core);
}
