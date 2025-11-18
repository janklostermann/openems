package io.openems.edge.evse.simulator.abl;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;

/**
 * Modbus register mapper for ABL chargepoints.
 *
 * <p>
 * Maps {@link ChargePointSimulatorCore} state to ABL-specific Modbus registers.
 * ABL uses FC3 Holding Registers with an offset based on plug number.
 */
public class AblRegisterMapper implements ModbusRegisterMapper {

	private static final int PLUG_OFFSET = 0x100; // 256 per plug

	// Base register addresses (add plug offset)
	private static final int REG_CURRENT_L1 = 12289;
	private static final int REG_CURRENT_L2 = 12291;
	private static final int REG_CURRENT_L3 = 12293;
	private static final int REG_ACTIVE_POWER = 12301;
	private static final int REG_ENERGY = 12303;
	private static final int REG_CHARGE_POINT_STATE = 12337;
	private static final int REG_CURRENT_LIMIT = 12338;

	private final int plugNumber;
	private final int offset;

	/**
	 * Create mapper for plug 1 (default).
	 */
	public AblRegisterMapper() {
		this(1);
	}

	/**
	 * Create mapper for a specific plug.
	 *
	 * @param plugNumber the plug number (1 or 2)
	 */
	public AblRegisterMapper(int plugNumber) {
		this.plugNumber = plugNumber;
		this.offset = PLUG_OFFSET * (plugNumber - 1);
	}

	@Override
	public void updateRegisters(SimpleProcessImage pi, ChargePointSimulatorCore core) {
		// Currents as doubleword in 10mA units (scale factor 2)
		int currentMa10 = (int) (core.getCurrentAmps() * 100);
		int currentL2 = (core.getPhases() == 3) ? currentMa10 : 0;
		int currentL3 = (core.getPhases() == 3) ? currentMa10 : 0;

		pi.addRegister(REG_CURRENT_L1 + this.offset, new SimpleRegister(0));
		pi.addRegister(REG_CURRENT_L1 + this.offset + 1, new SimpleRegister(currentMa10));
		pi.addRegister(REG_CURRENT_L2 + this.offset, new SimpleRegister(0));
		pi.addRegister(REG_CURRENT_L2 + this.offset + 1, new SimpleRegister(currentL2));
		pi.addRegister(REG_CURRENT_L3 + this.offset, new SimpleRegister(0));
		pi.addRegister(REG_CURRENT_L3 + this.offset + 1, new SimpleRegister(currentL3));

		// Active power as doubleword in W
		int powerW = (int) core.getPowerWatts();
		pi.addRegister(REG_ACTIVE_POWER + this.offset, new SimpleRegister(0));
		pi.addRegister(REG_ACTIVE_POWER + this.offset + 1, new SimpleRegister(powerW));

		// Energy as doubleword in Wh
		int energyWh = (int) core.getTotalEnergyWh();
		pi.addRegister(REG_ENERGY + this.offset, new SimpleRegister(0));
		pi.addRegister(REG_ENERGY + this.offset + 1, new SimpleRegister(energyWh));

		// Charge point state
		pi.addRegister(REG_CHARGE_POINT_STATE + this.offset, new SimpleRegister(stateToAblCode(core.getState())));

		// Current limit in 0.1A units (scale factor -1)
		pi.addRegister(REG_CURRENT_LIMIT + this.offset, new SimpleRegister(core.getSetCurrentMa() / 100));
	}

	/**
	 * Convert charging state to ABL state code.
	 *
	 * <p>
	 * ABL status codes according to AblStatus enum.
	 */
	private static int stateToAblCode(ChargePointState state) {
		return switch (state) {
		case A -> 0xA1;  // Available, not connected
		case B -> 0xB1;  // Connected, waiting
		case C -> 0xC2;  // Charging
		case D -> 0xC2;  // Charging (D not specifically supported)
		case E -> 0xE0;  // Error
		case F -> 0xF1;  // Not available
		};
	}

	/**
	 * Get the plug number this mapper is configured for.
	 *
	 * @return the plug number
	 */
	public int getPlugNumber() {
		return this.plugNumber;
	}
}
