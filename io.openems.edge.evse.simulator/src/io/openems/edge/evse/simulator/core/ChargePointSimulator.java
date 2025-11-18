package io.openems.edge.evse.simulator.core;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.evse.simulator.alfen.AlfenRegisterMapper;

/**
 * Chargepoint simulator for testing.
 *
 * <p>
 * Wraps a {@link ChargePointSimulatorCore} and {@link ModbusRegisterMapper} to
 * provide a complete simulation of a chargepoint device for use in unit tests.
 */
public class ChargePointSimulator {

	private final ChargePointSimulatorCore core;
	private final ModbusRegisterMapper mapper;
	private final DummyModbusBridge modbus;

	/**
	 * Create a new ChargePointSimulator.
	 *
	 * @param modbusId the Modbus bridge ID
	 * @param mapper   the device-specific register mapper
	 */
	public ChargePointSimulator(String modbusId, ModbusRegisterMapper mapper) {
		this.modbus = new DummyModbusBridge(modbusId);
		this.core = new ChargePointSimulatorCore();
		this.mapper = mapper;
		this.updateRegisters();
	}

	/**
	 * Factory method for Alfen chargepoint simulator.
	 *
	 * @param modbusId the Modbus bridge ID
	 * @return a new {@link ChargePointSimulator} configured for Alfen
	 */
	public static ChargePointSimulator alfen(String modbusId) {
		return new ChargePointSimulator(modbusId, new AlfenRegisterMapper());
	}

	/**
	 * Get the {@link DummyModbusBridge} for use in tests.
	 *
	 * @return the modbus bridge
	 */
	public DummyModbusBridge getModbus() {
		return this.modbus;
	}

	/**
	 * Get the underlying {@link ChargePointSimulatorCore}.
	 *
	 * @return the core simulator
	 */
	public ChargePointSimulatorCore getCore() {
		return this.core;
	}

	// State transitions

	/**
	 * Simulate car plugging in.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator plugIn() {
		this.core.plugIn();
		this.updateRegisters();
		return this;
	}

	/**
	 * Simulate car unplugging.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator unplug() {
		this.core.unplug();
		this.updateRegisters();
		return this;
	}

	/**
	 * Simulate car requesting to charge.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator requestCharge() {
		this.core.requestCharge();
		this.updateRegisters();
		return this;
	}

	/**
	 * Simulate car stopping charge.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator stopCharge() {
		this.core.stopCharge();
		this.updateRegisters();
		return this;
	}

	/**
	 * Set an error state.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator setError() {
		this.core.setError();
		this.updateRegisters();
		return this;
	}

	/**
	 * Clear error state.
	 *
	 * @return this for chaining
	 */
	public ChargePointSimulator clearError() {
		this.core.clearError();
		this.updateRegisters();
		return this;
	}

	// Control methods

	/**
	 * Set the current limit in milliampere.
	 *
	 * @param milliAmps current limit in mA
	 * @return this for chaining
	 */
	public ChargePointSimulator setCurrentLimit(int milliAmps) {
		this.core.setCurrentLimit(milliAmps);
		this.updateRegisters();
		return this;
	}

	/**
	 * Set the number of phases.
	 *
	 * @param phases 1 or 3
	 * @return this for chaining
	 */
	public ChargePointSimulator setPhases(int phases) {
		this.core.setPhases(phases);
		this.updateRegisters();
		return this;
	}

	/**
	 * Set individual phase voltages.
	 *
	 * @param l1 voltage L1 in V
	 * @param l2 voltage L2 in V
	 * @param l3 voltage L3 in V
	 * @return this for chaining
	 */
	public ChargePointSimulator setVoltages(double l1, double l2, double l3) {
		this.core.setVoltages(l1, l2, l3);
		this.updateRegisters();
		return this;
	}

	/**
	 * Simulate time passing.
	 *
	 * @param milliseconds time passed
	 * @return this for chaining
	 */
	public ChargePointSimulator tick(long milliseconds) {
		this.core.tick(milliseconds);
		this.updateRegisters();
		return this;
	}

	// Configuration

	/**
	 * Set the total energy (e.g., to simulate a charger with history).
	 *
	 * @param energyWh total energy in Wh
	 * @return this for chaining
	 */
	public ChargePointSimulator setTotalEnergy(double energyWh) {
		this.core.setTotalEnergyWh(energyWh);
		this.updateRegisters();
		return this;
	}

	/**
	 * Get the current charging state.
	 *
	 * @return the state
	 */
	public ChargePointState getState() {
		return this.core.getState();
	}

	/**
	 * Update all Modbus registers from current state.
	 */
	private void updateRegisters() {
		this.mapper.updateRegisters(this.modbus, this.core);
	}
}
