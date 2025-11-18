package io.openems.edge.evse.simulator.keba;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;

/**
 * Modbus register mapper for Keba chargepoints.
 *
 * <p>
 * Maps {@link ChargePointSimulatorCore} state to Keba-specific Modbus
 * registers. Keba uses FC3 Holding Registers with doubleword elements.
 */
public class KebaRegisterMapper implements ModbusRegisterMapper {

	// Keba Modbus register addresses (all doubleword)
	private static final int REG_CHARGING_STATE = 1000;
	private static final int REG_CABLE_STATE = 1004;
	private static final int REG_ERROR_CODE = 1006;
	private static final int REG_CURRENT_L1 = 1008;
	private static final int REG_CURRENT_L2 = 1010;
	private static final int REG_CURRENT_L3 = 1012;
	private static final int REG_SERIAL_NUMBER = 1014;
	private static final int REG_PRODUCT_TYPE = 1016;
	private static final int REG_FIRMWARE = 1018;
	private static final int REG_ACTIVE_POWER = 1020;
	private static final int REG_ENERGY = 1036;
	private static final int REG_VOLTAGE_L1 = 1040;
	private static final int REG_VOLTAGE_L2 = 1042;
	private static final int REG_VOLTAGE_L3 = 1044;
	private static final int REG_POWER_FACTOR = 1046;
	private static final int REG_MAX_CHARGING_CURRENT = 1100;
	private static final int REG_MAX_SUPPORTED_CURRENT = 1110;
	private static final int REG_SESSION_ENERGY = 1502;
	private static final int REG_SET_CURRENT = 5004;

	@Override
	public void updateRegisters(DummyModbusBridge modbus, ChargePointSimulatorCore core) {
		// Charging state (doubleword)
		modbus.withRegisters(REG_CHARGING_STATE, 0, stateToKebaChargingState(core.getState()));

		// Cable state (doubleword)
		modbus.withRegisters(REG_CABLE_STATE, 0, stateToCableState(core.getState()));

		// Error code (0 = no error)
		modbus.withRegisters(REG_ERROR_CODE, 0, core.getState() == ChargePointState.E ? 1 : 0);

		// Currents in mA (doubleword)
		int currentMa = (int) (core.getCurrentAmps() * 1000);
		int currentL2 = (core.getPhases() == 3) ? currentMa : 0;
		int currentL3 = (core.getPhases() == 3) ? currentMa : 0;
		modbus.withRegisters(REG_CURRENT_L1, 0, currentMa);
		modbus.withRegisters(REG_CURRENT_L2, 0, currentL2);
		modbus.withRegisters(REG_CURRENT_L3, 0, currentL3);

		// Serial number (example)
		modbus.withRegisters(REG_SERIAL_NUMBER, 0, 12345);

		// Product type and features (P30 c-series)
		modbus.withRegisters(REG_PRODUCT_TYPE, 0, 0x00011111);

		// Firmware version (e.g., 1.13.4)
		modbus.withRegisters(REG_FIRMWARE, 0, 0x010D04);

		// Active power in mW (scale factor -3, so we store as mW)
		int powerMw = (int) (core.getPowerWatts() * 1000);
		modbus.withRegisters(REG_ACTIVE_POWER, (powerMw >> 16) & 0xFFFF, powerMw & 0xFFFF);

		// Total energy in 0.1 Wh
		int energy01Wh = (int) (core.getTotalEnergyWh() * 10);
		modbus.withRegisters(REG_ENERGY, (energy01Wh >> 16) & 0xFFFF, energy01Wh & 0xFFFF);

		// Voltages in mV (scale factor 3)
		int voltageL1Mv = (int) (core.getVoltageL1() * 1000);
		int voltageL2Mv = (int) (core.getVoltageL2() * 1000);
		int voltageL3Mv = (int) (core.getVoltageL3() * 1000);
		modbus.withRegisters(REG_VOLTAGE_L1, (voltageL1Mv >> 16) & 0xFFFF, voltageL1Mv & 0xFFFF);
		modbus.withRegisters(REG_VOLTAGE_L2, (voltageL2Mv >> 16) & 0xFFFF, voltageL2Mv & 0xFFFF);
		modbus.withRegisters(REG_VOLTAGE_L3, (voltageL3Mv >> 16) & 0xFFFF, voltageL3Mv & 0xFFFF);

		// Power factor (0-100%)
		modbus.withRegisters(REG_POWER_FACTOR, 0, 1000); // 100.0%

		// Max charging current (current setting) in mA
		modbus.withRegisters(REG_MAX_CHARGING_CURRENT, 0, core.getSetCurrentMa());

		// Max supported current (hardware limit) in mA
		modbus.withRegisters(REG_MAX_SUPPORTED_CURRENT, 0, core.getMaxCurrentMa());

		// Session energy in 0.1 Wh
		int sessionEnergy01Wh = (int) (core.getSessionEnergyWh() * 10);
		modbus.withRegisters(REG_SESSION_ENERGY, (sessionEnergy01Wh >> 16) & 0xFFFF, sessionEnergy01Wh & 0xFFFF);

		// Set current (write register) in mA
		modbus.withRegisters(REG_SET_CURRENT, 0, core.getSetCurrentMa());
	}

	/**
	 * Convert charging state to Keba charging state code.
	 */
	private static int stateToKebaChargingState(ChargePointState state) {
		return switch (state) {
		case A -> 0; // Not connected
		case B -> 1; // Connected, not charging
		case C -> 3; // Charging
		case D -> 3; // Charging (D handled as C)
		case E -> 4; // Error
		case F -> 5; // Authorization rejected / not available
		};
	}

	/**
	 * Convert charging state to Keba cable state code.
	 */
	private static int stateToCableState(ChargePointState state) {
		return switch (state) {
		case A -> 0; // No cable
		case B, C, D -> 7; // Cable connected and locked
		case E, F -> 0;
		};
	}
}
