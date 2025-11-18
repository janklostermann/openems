package io.openems.edge.evse.simulator.keba;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

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
	public void updateRegisters(SimpleProcessImage pi, ChargePointSimulatorCore core) {
		// Charging state (doubleword)
		addDoubleword(pi, REG_CHARGING_STATE, stateToKebaChargingState(core.getState()));

		// Cable state (doubleword)
		addDoubleword(pi, REG_CABLE_STATE, stateToCableState(core.getState()));

		// Error code (0 = no error)
		addDoubleword(pi, REG_ERROR_CODE, core.getState() == ChargePointState.E ? 1 : 0);

		// Currents in mA (doubleword)
		int currentMa = (int) (core.getCurrentAmps() * 1000);
		int currentL2 = (core.getPhases() == 3) ? currentMa : 0;
		int currentL3 = (core.getPhases() == 3) ? currentMa : 0;
		addDoubleword(pi, REG_CURRENT_L1, currentMa);
		addDoubleword(pi, REG_CURRENT_L2, currentL2);
		addDoubleword(pi, REG_CURRENT_L3, currentL3);

		// Serial number (example)
		addDoubleword(pi, REG_SERIAL_NUMBER, 12345);

		// Product type and features (P30 c-series)
		addDoubleword(pi, REG_PRODUCT_TYPE, 0x00011111);

		// Firmware version (e.g., 1.13.4)
		addDoubleword(pi, REG_FIRMWARE, 0x010D04);

		// Active power in mW (scale factor -3, so we store as mW)
		int powerMw = (int) (core.getPowerWatts() * 1000);
		pi.addRegister(REG_ACTIVE_POWER, new SimpleRegister((powerMw >> 16) & 0xFFFF));
		pi.addRegister(REG_ACTIVE_POWER + 1, new SimpleRegister(powerMw & 0xFFFF));

		// Total energy in 0.1 Wh
		int energy01Wh = (int) (core.getTotalEnergyWh() * 10);
		pi.addRegister(REG_ENERGY, new SimpleRegister((energy01Wh >> 16) & 0xFFFF));
		pi.addRegister(REG_ENERGY + 1, new SimpleRegister(energy01Wh & 0xFFFF));

		// Voltages in mV (scale factor 3)
		int voltageL1Mv = (int) (core.getVoltageL1() * 1000);
		int voltageL2Mv = (int) (core.getVoltageL2() * 1000);
		int voltageL3Mv = (int) (core.getVoltageL3() * 1000);
		pi.addRegister(REG_VOLTAGE_L1, new SimpleRegister((voltageL1Mv >> 16) & 0xFFFF));
		pi.addRegister(REG_VOLTAGE_L1 + 1, new SimpleRegister(voltageL1Mv & 0xFFFF));
		pi.addRegister(REG_VOLTAGE_L2, new SimpleRegister((voltageL2Mv >> 16) & 0xFFFF));
		pi.addRegister(REG_VOLTAGE_L2 + 1, new SimpleRegister(voltageL2Mv & 0xFFFF));
		pi.addRegister(REG_VOLTAGE_L3, new SimpleRegister((voltageL3Mv >> 16) & 0xFFFF));
		pi.addRegister(REG_VOLTAGE_L3 + 1, new SimpleRegister(voltageL3Mv & 0xFFFF));

		// Power factor (0-100%)
		addDoubleword(pi, REG_POWER_FACTOR, 1000); // 100.0%

		// Max charging current (current setting) in mA
		addDoubleword(pi, REG_MAX_CHARGING_CURRENT, core.getSetCurrentMa());

		// Max supported current (hardware limit) in mA
		addDoubleword(pi, REG_MAX_SUPPORTED_CURRENT, core.getMaxCurrentMa());

		// Session energy in 0.1 Wh
		int sessionEnergy01Wh = (int) (core.getSessionEnergyWh() * 10);
		pi.addRegister(REG_SESSION_ENERGY, new SimpleRegister((sessionEnergy01Wh >> 16) & 0xFFFF));
		pi.addRegister(REG_SESSION_ENERGY + 1, new SimpleRegister(sessionEnergy01Wh & 0xFFFF));

		// Set current (write register) in mA
		addDoubleword(pi, REG_SET_CURRENT, core.getSetCurrentMa());
	}

	/**
	 * Add a doubleword (two registers) to the process image.
	 *
	 * @param pi      the process image
	 * @param address the starting address
	 * @param value   the value (low word stored at address+1)
	 */
	private static void addDoubleword(SimpleProcessImage pi, int address, int value) {
		pi.addRegister(address, new SimpleRegister(0));
		pi.addRegister(address + 1, new SimpleRegister(value));
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
