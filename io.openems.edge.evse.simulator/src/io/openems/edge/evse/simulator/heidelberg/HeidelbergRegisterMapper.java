package io.openems.edge.evse.simulator.heidelberg;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

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
	public void updateRegisters(SimpleProcessImage pi, ChargePointSimulatorCore core) {
		// Input Registers (FC4) - Heidelberg uses these for readings
		// Layout version
		pi.addInputRegister(REG_LAYOUT_VERSION, new SimpleRegister(0x0108)); // V1.0.8

		// Charging state (Heidelberg uses specific state codes)
		pi.addInputRegister(REG_CHARGING_STATE, new SimpleRegister(stateToHeidelbergCode(core.getState())));

		// Currents in 10mA units (scale factor 2 = divide by 100 to get A)
		int currentMa10 = (int) (core.getCurrentAmps() * 100);
		int currentL2 = (core.getPhases() == 3) ? currentMa10 : 0;
		int currentL3 = (core.getPhases() == 3) ? currentMa10 : 0;
		pi.addInputRegister(REG_CURRENT_L1, new SimpleRegister(currentMa10));
		pi.addInputRegister(REG_CURRENT_L2, new SimpleRegister(currentL2));
		pi.addInputRegister(REG_CURRENT_L3, new SimpleRegister(currentL3));

		// Temperature (deci-degrees Celsius)
		pi.addInputRegister(REG_TEMPERATURE, new SimpleRegister(250)); // 25.0°C

		// Voltages in V
		pi.addInputRegister(REG_VOLTAGE_L1, new SimpleRegister((int) core.getVoltageL1()));
		pi.addInputRegister(REG_VOLTAGE_L2, new SimpleRegister((int) core.getVoltageL2()));
		pi.addInputRegister(REG_VOLTAGE_L3, new SimpleRegister((int) core.getVoltageL3()));

		// Lock state (1 = unlocked)
		pi.addInputRegister(REG_LOCK_STATE, new SimpleRegister(1));

		// Active power in W
		pi.addInputRegister(REG_ACTIVE_POWER, new SimpleRegister((int) core.getPowerWatts()));

		// Energy (doubleword, Wh)
		long energyWh = (long) core.getTotalEnergyWh();
		pi.addInputRegister(REG_ENERGY, new SimpleRegister((int) (energyWh >> 16)));
		pi.addInputRegister(REG_ENERGY + 1, new SimpleRegister((int) (energyWh & 0xFFFF)));

		// Session energy
		long sessionWh = (long) core.getSessionEnergyWh();
		pi.addInputRegister(REG_SESSION_ENERGY, new SimpleRegister((int) (sessionWh >> 16)));
		pi.addInputRegister(REG_SESSION_ENERGY + 1, new SimpleRegister((int) (sessionWh & 0xFFFF)));

		// Max/Min current
		pi.addInputRegister(REG_MAX_CURRENT, new SimpleRegister(core.getMaxCurrentMa() / 1000));
		pi.addInputRegister(REG_MIN_CURRENT, new SimpleRegister(core.getMinCurrentMa() / 1000));

		// Holding Registers (FC3)
		// Set current in 10mA units
		pi.addRegister(REG_SET_CURRENT, new SimpleRegister(core.getSetCurrentMa() / 10));

		// Phase switch (1 = single, 3 = three)
		pi.addRegister(REG_PHASE_SWITCH, new SimpleRegister(core.getPhases()));
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
