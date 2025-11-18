package io.openems.edge.evse.simulator.alfen;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;

/**
 * Modbus register mapper for Alfen Eve chargepoints.
 *
 * <p>
 * Maps {@link ChargePointSimulatorCore} state to Alfen-specific Modbus
 * registers.
 */
public class AlfenRegisterMapper implements ModbusRegisterMapper {

	// Alfen Modbus register addresses
	private static final int REG_VOLTAGE_L1 = 306;
	private static final int REG_CURRENT_L1 = 320;
	private static final int REG_POWER = 344;
	private static final int REG_ENERGY = 374;
	private static final int REG_CHARGING_STATE = 1201;
	private static final int REG_MAX_CURRENT = 1210;
	private static final int REG_PHASES = 1215;

	@Override
	public void updateRegisters(SimpleProcessImage pi, ChargePointSimulatorCore core) {
		// Voltages (register 306-311, 3x Float32)
		addRegisters(pi, REG_VOLTAGE_L1, //
				floatToRegisters((float) core.getVoltageL1()), //
				floatToRegisters((float) core.getVoltageL2()), //
				floatToRegisters((float) core.getVoltageL3()));

		// Currents (register 320-325, 3x Float32)
		float current = (float) core.getCurrentAmps();
		float currentL2 = (core.getPhases() == 3) ? current : 0f;
		float currentL3 = (core.getPhases() == 3) ? current : 0f;
		addRegisters(pi, REG_CURRENT_L1, //
				floatToRegisters(current), //
				floatToRegisters(currentL2), //
				floatToRegisters(currentL3));

		// Power (register 344-347, Float64)
		addRegisters(pi, REG_POWER, //
				doubleToRegisters(core.getPowerWatts()));

		// Total energy (register 374-377, Float64)
		addRegisters(pi, REG_ENERGY, //
				doubleToRegisters(core.getTotalEnergyWh()));

		// Charging state (register 1201-1205, String 5 registers)
		addRegisters(pi, REG_CHARGING_STATE, //
				stateToRegisters(core.getState()));

		// Max current (register 1210-1211, Float32 in mA)
		addRegisters(pi, REG_MAX_CURRENT, //
				floatToRegisters((float) core.getSetCurrentMa()));

		// Phase configuration (register 1215, Uint16)
		pi.addRegister(REG_PHASES, new SimpleRegister(core.getPhases()));
	}

	/**
	 * Add multiple register arrays to the process image.
	 *
	 * @param pi           the process image
	 * @param startAddress the starting address
	 * @param arrays       arrays of register values
	 */
	private static void addRegisters(SimpleProcessImage pi, int startAddress, int[]... arrays) {
		int address = startAddress;
		for (var arr : arrays) {
			for (int value : arr) {
				pi.addRegister(address++, new SimpleRegister(value));
			}
		}
	}

	/**
	 * Convert a float to two 16-bit register values.
	 *
	 * @param value the float value
	 * @return array of two integers [highWord, lowWord]
	 */
	private static int[] floatToRegisters(float value) {
		int bits = Float.floatToIntBits(value);
		return new int[] { (bits >> 16) & 0xFFFF, bits & 0xFFFF };
	}

	/**
	 * Convert a double to four 16-bit register values.
	 *
	 * @param value the double value
	 * @return array of four integers
	 */
	private static int[] doubleToRegisters(double value) {
		long bits = Double.doubleToLongBits(value);
		return new int[] { //
				(int) ((bits >> 48) & 0xFFFF), //
				(int) ((bits >> 32) & 0xFFFF), //
				(int) ((bits >> 16) & 0xFFFF), //
				(int) (bits & 0xFFFF) //
		};
	}

	/**
	 * Convert charging state to Modbus string registers.
	 *
	 * <p>
	 * Alfen uses a string representation like "A", "B1", "B2", "C1", "C2", etc.
	 *
	 * @param state the charging state
	 * @return array of 5 register values
	 */
	private static int[] stateToRegisters(ChargePointState state) {
		String stateStr = switch (state) {
		case A -> "A";
		case B -> "B2"; // B2 = connected, ready for charging
		case C -> "C2"; // C2 = charging
		case D -> "D2"; // D2 = charging with ventilation
		case E -> "E";
		case F -> "F";
		};

		// Convert string to register values (2 chars per register)
		int[] registers = new int[5];
		byte[] bytes = stateStr.getBytes();
		for (int i = 0; i < bytes.length && i < 10; i += 2) {
			int highByte = bytes[i] & 0xFF;
			int lowByte = (i + 1 < bytes.length) ? bytes[i + 1] & 0xFF : 0;
			registers[i / 2] = (highByte << 8) | lowByte;
		}
		return registers;
	}
}
