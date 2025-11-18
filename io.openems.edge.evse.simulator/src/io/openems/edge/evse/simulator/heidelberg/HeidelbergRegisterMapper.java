package io.openems.edge.evse.simulator.heidelberg;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;

/**
 * Modbus register mapper for Heidelberg Connect chargepoints.
 *
 * <p>
 * Maps {@link ChargePointSimulatorCore} state to Heidelberg-specific Modbus
 * registers. Heidelberg uses FC4 Input Registers for most values.
 */
public class HeidelbergRegisterMapper implements ModbusRegisterMapper {

	// Heidelberg Modbus register addresses (Input Registers - FC4)
	private static final int REG_LAYOUT_VERSION = 4;
	private static final int REG_CHARGING_STATE = 5;
	private static final int REG_CURRENT_L1 = 6;
	private static final int REG_CURRENT_L2 = 7;
	private static final int REG_CURRENT_L3 = 8;
	private static final int REG_TEMPERATURE = 9;
	private static final int REG_VOLTAGE_L1 = 10;
	private static final int REG_VOLTAGE_L2 = 11;
	private static final int REG_VOLTAGE_L3 = 12;
	private static final int REG_LOCK_STATE = 13;
	private static final int REG_ACTIVE_POWER = 14;
	private static final int REG_ENERGY = 17;
	private static final int REG_SESSION_ENERGY = 19;
	private static final int REG_MAX_CURRENT = 100;
	private static final int REG_MIN_CURRENT = 101;

	// Holding Registers (FC3)
	private static final int REG_SET_CURRENT = 261;
	private static final int REG_PHASE_SWITCH = 501;

	@Override
	public void updateRegisters(DummyModbusBridge modbus, ChargePointSimulatorCore core) {
		// Input Registers (FC4) - Heidelberg uses these for readings
		// Layout version
		modbus.withInputRegister(REG_LAYOUT_VERSION, 0x0108); // V1.0.8

		// Charging state (Heidelberg uses specific state codes)
		modbus.withInputRegister(REG_CHARGING_STATE, stateToHeidelbergCode(core.getState()));

		// Currents in 10mA units (scale factor 2 = divide by 100 to get A)
		int currentMa10 = (int) (core.getCurrentAmps() * 100);
		int currentL2 = (core.getPhases() == 3) ? currentMa10 : 0;
		int currentL3 = (core.getPhases() == 3) ? currentMa10 : 0;
		modbus.withInputRegister(REG_CURRENT_L1, currentMa10);
		modbus.withInputRegister(REG_CURRENT_L2, currentL2);
		modbus.withInputRegister(REG_CURRENT_L3, currentL3);

		// Temperature (deci-degrees Celsius)
		modbus.withInputRegister(REG_TEMPERATURE, 250); // 25.0°C

		// Voltages in V
		modbus.withInputRegister(REG_VOLTAGE_L1, (int) core.getVoltageL1());
		modbus.withInputRegister(REG_VOLTAGE_L2, (int) core.getVoltageL2());
		modbus.withInputRegister(REG_VOLTAGE_L3, (int) core.getVoltageL3());

		// Lock state (1 = unlocked)
		modbus.withInputRegister(REG_LOCK_STATE, 1);

		// Active power in W
		modbus.withInputRegister(REG_ACTIVE_POWER, (int) core.getPowerWatts());

		// Energy (doubleword, Wh)
		long energyWh = (long) core.getTotalEnergyWh();
		modbus.withInputRegisters(REG_ENERGY, (int) (energyWh >> 16), (int) (energyWh & 0xFFFF));

		// Session energy
		long sessionWh = (long) core.getSessionEnergyWh();
		modbus.withInputRegisters(REG_SESSION_ENERGY, (int) (sessionWh >> 16), (int) (sessionWh & 0xFFFF));

		// Max/Min current
		modbus.withInputRegister(REG_MAX_CURRENT, core.getMaxCurrentMa() / 1000);
		modbus.withInputRegister(REG_MIN_CURRENT, core.getMinCurrentMa() / 1000);

		// Holding Registers (FC3)
		// Set current in 10mA units
		modbus.withRegister(REG_SET_CURRENT, core.getSetCurrentMa() / 10);

		// Phase switch (1 = single, 3 = three)
		modbus.withRegister(REG_PHASE_SWITCH, core.getPhases());
	}

	/**
	 * Convert charging state to Heidelberg state code.
	 *
	 * <p>
	 * Heidelberg uses: A1=2, B1=3, B2=4, C1=5, C2=6, etc.
	 */
	private static int stateToHeidelbergCode(ChargePointState state) {
		return switch (state) {
		case A -> 2;  // A1 - not connected
		case B -> 4;  // B2 - connected, ready
		case C -> 6;  // C2 - charging
		case D -> 8;  // D2 - charging with ventilation
		case E -> 9;  // E - error
		case F -> 10; // F - not available
		};
	}
}
