package io.openems.edge.evse.simulator.core;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;

/**
 * Interface for device-specific Modbus register mapping.
 *
 * <p>
 * Implementations translate the generic {@link ChargePointSimulatorCore} state
 * into device-specific Modbus register values.
 */
public interface ModbusRegisterMapper {

	/**
	 * Update the {@link DummyModbusBridge} registers based on simulator state.
	 *
	 * <p>
	 * This is called after each state change to synchronize the Modbus registers
	 * with the current simulator state.
	 *
	 * @param modbus the {@link DummyModbusBridge} to update
	 * @param core   the {@link ChargePointSimulatorCore} with current state
	 */
	void updateRegisters(DummyModbusBridge modbus, ChargePointSimulatorCore core);
}
