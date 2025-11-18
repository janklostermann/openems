package io.openems.edge.evse.simulator.core;

/**
 * Core simulator logic for IEC 61851-1 chargepoints.
 *
 * <p>
 * Contains state machine and physics calculations that are common to all
 * chargepoints. Device-specific behavior (like Modbus register mapping) is
 * handled by {@link ModbusRegisterMapper} implementations.
 */
public class ChargePointSimulatorCore {

	private ChargePointState state = ChargePointState.A;
	private int setCurrentMa = 0;
	private int phases = 3;
	private double voltageL1 = 230.0;
	private double voltageL2 = 230.0;
	private double voltageL3 = 230.0;
	private double totalEnergyWh = 0;
	private double sessionEnergyWh = 0;

	// Configuration
	private int minCurrentMa = 6000;
	private int maxCurrentMa = 32000;

	/**
	 * Simulate car plugging in.
	 *
	 * <p>
	 * Transitions from state A to B.
	 */
	public void plugIn() {
		if (this.state == ChargePointState.A) {
			this.state = ChargePointState.B;
			this.sessionEnergyWh = 0;
		}
	}

	/**
	 * Simulate car unplugging.
	 *
	 * <p>
	 * Transitions to state A.
	 */
	public void unplug() {
		this.state = ChargePointState.A;
		this.setCurrentMa = 0;
	}

	/**
	 * Simulate car requesting to charge.
	 *
	 * <p>
	 * Transitions from state B to C if current limit is set.
	 */
	public void requestCharge() {
		if (this.state == ChargePointState.B && this.setCurrentMa >= this.minCurrentMa) {
			this.state = ChargePointState.C;
		}
	}

	/**
	 * Simulate car stopping charge.
	 *
	 * <p>
	 * Transitions from state C back to B.
	 */
	public void stopCharge() {
		if (this.state == ChargePointState.C || this.state == ChargePointState.D) {
			this.state = ChargePointState.B;
		}
	}

	/**
	 * Set an error state.
	 */
	public void setError() {
		this.state = ChargePointState.E;
	}

	/**
	 * Clear error and return to state A.
	 */
	public void clearError() {
		if (this.state == ChargePointState.E) {
			this.state = ChargePointState.A;
		}
	}

	/**
	 * Set the current limit in milliampere.
	 *
	 * <p>
	 * This simulates the EMS setting a charging current. If the car is in state B
	 * and current is above minimum, charging starts. If current is set to 0,
	 * charging pauses.
	 *
	 * @param milliAmps current limit in mA
	 */
	public void setCurrentLimit(int milliAmps) {
		this.setCurrentMa = Math.min(milliAmps, this.maxCurrentMa);

		// Auto-transition based on current
		if (this.state == ChargePointState.B && this.setCurrentMa >= this.minCurrentMa) {
			this.state = ChargePointState.C;
		} else if (this.state == ChargePointState.C && this.setCurrentMa < this.minCurrentMa) {
			this.state = ChargePointState.B;
		}
	}

	/**
	 * Set the number of phases.
	 *
	 * @param phases 1 or 3
	 */
	public void setPhases(int phases) {
		if (phases == 1 || phases == 3) {
			this.phases = phases;
		}
	}

	/**
	 * Set individual phase voltages.
	 *
	 * @param l1 voltage L1 in V
	 * @param l2 voltage L2 in V
	 * @param l3 voltage L3 in V
	 */
	public void setVoltages(double l1, double l2, double l3) {
		this.voltageL1 = l1;
		this.voltageL2 = l2;
		this.voltageL3 = l3;
	}

	/**
	 * Simulate time passing.
	 *
	 * <p>
	 * Accumulates energy if charging.
	 *
	 * @param milliseconds time passed
	 */
	public void tick(long milliseconds) {
		if (this.state == ChargePointState.C || this.state == ChargePointState.D) {
			double energyWh = this.getPowerWatts() * milliseconds / 3_600_000.0;
			this.totalEnergyWh += energyWh;
			this.sessionEnergyWh += energyWh;
		}
	}

	// Getters for calculated values

	/**
	 * Get current on each phase in Ampere.
	 *
	 * @return current in A (0 if not charging)
	 */
	public double getCurrentAmps() {
		if (this.state == ChargePointState.C || this.state == ChargePointState.D) {
			return this.setCurrentMa / 1000.0;
		}
		return 0;
	}

	/**
	 * Get total active power in Watts.
	 *
	 * @return power in W
	 */
	public double getPowerWatts() {
		double current = this.getCurrentAmps();
		if (this.phases == 1) {
			return this.voltageL1 * current;
		}
		return (this.voltageL1 + this.voltageL2 + this.voltageL3) * current;
	}

	/**
	 * Get power on L1 in Watts.
	 *
	 * @return power L1 in W
	 */
	public double getPowerL1Watts() {
		return this.voltageL1 * this.getCurrentAmps();
	}

	/**
	 * Get power on L2 in Watts.
	 *
	 * @return power L2 in W
	 */
	public double getPowerL2Watts() {
		if (this.phases == 1) {
			return 0;
		}
		return this.voltageL2 * this.getCurrentAmps();
	}

	/**
	 * Get power on L3 in Watts.
	 *
	 * @return power L3 in W
	 */
	public double getPowerL3Watts() {
		if (this.phases == 1) {
			return 0;
		}
		return this.voltageL3 * this.getCurrentAmps();
	}

	// Simple getters

	public ChargePointState getState() {
		return this.state;
	}

	public int getSetCurrentMa() {
		return this.setCurrentMa;
	}

	public int getPhases() {
		return this.phases;
	}

	public double getVoltageL1() {
		return this.voltageL1;
	}

	public double getVoltageL2() {
		return this.voltageL2;
	}

	public double getVoltageL3() {
		return this.voltageL3;
	}

	public double getTotalEnergyWh() {
		return this.totalEnergyWh;
	}

	public double getSessionEnergyWh() {
		return this.sessionEnergyWh;
	}

	public int getMinCurrentMa() {
		return this.minCurrentMa;
	}

	public int getMaxCurrentMa() {
		return this.maxCurrentMa;
	}

	// Configuration setters

	public void setMinCurrentMa(int minCurrentMa) {
		this.minCurrentMa = minCurrentMa;
	}

	public void setMaxCurrentMa(int maxCurrentMa) {
		this.maxCurrentMa = maxCurrentMa;
	}

	public void setTotalEnergyWh(double totalEnergyWh) {
		this.totalEnergyWh = totalEnergyWh;
	}
}
