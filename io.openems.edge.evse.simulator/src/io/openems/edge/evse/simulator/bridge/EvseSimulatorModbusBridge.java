package io.openems.edge.evse.simulator.bridge;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.simulator.core.ChargePointState;

/**
 * Interface for EVSE Simulator Modbus Bridge.
 *
 * <p>
 * This component simulates an EVSE chargepoint over Modbus for testing and
 * demonstration purposes. It can be controlled via channels and provides
 * realistic state transitions following IEC 61851-1.
 */
@ProviderType
public interface EvseSimulatorModbusBridge extends BridgeModbus, BridgeModbusTcp, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Control channels (writable)
		/**
		 * Simulate plugging in a vehicle.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Boolean
		 * <li>Write: true to plug in vehicle
		 * </ul>
		 */
		SIMULATE_PLUG_IN(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Set to true to simulate plugging in a vehicle")),

		/**
		 * Simulate unplugging a vehicle.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Boolean
		 * <li>Write: true to unplug vehicle
		 * </ul>
		 */
		SIMULATE_UNPLUG(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Set to true to simulate unplugging a vehicle")),

		/**
		 * Set the charging current limit in milliamps.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		SET_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Set the charging current limit")),

		/**
		 * Simulate an error condition.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Boolean
		 * </ul>
		 */
		SIMULATE_ERROR(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Set to true to simulate an error condition")),

		/**
		 * Clear error and return to previous state.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Boolean
		 * </ul>
		 */
		CLEAR_ERROR(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Set to true to clear error condition")),

		// Monitoring channels (read-only)
		/**
		 * Current charging state (IEC 61851-1).
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: ChargePointState
		 * </ul>
		 */
		SIMULATED_STATE(Doc.of(ChargePointState.values())//
				.text("Current simulated charging state")),

		/**
		 * Current charging power in Watts.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SIMULATED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("Current simulated charging power")),

		/**
		 * Total energy charged in this session in Wh.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		SIMULATED_ENERGY_SESSION(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT_HOURS)//
				.text("Energy charged in current session")),

		/**
		 * Total lifetime energy in Wh.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		SIMULATED_ENERGY_TOTAL(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT_HOURS)//
				.text("Total lifetime energy")),

		/**
		 * Current on phase L1 in mA.
		 *
		 * <ul>
		 * <li>Interface: EvseSimulatorModbusBridge
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		SIMULATED_CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Simulated current on L1")),

		/**
		 * Current on phase L2 in mA.
		 */
		SIMULATED_CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Simulated current on L2")),

		/**
		 * Current on phase L3 in mA.
		 */
		SIMULATED_CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Simulated current on L3")),

		/**
		 * Voltage on phase L1 in mV.
		 */
		SIMULATED_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Simulated voltage on L1")),

		/**
		 * Voltage on phase L2 in mV.
		 */
		SIMULATED_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Simulated voltage on L2")),

		/**
		 * Voltage on phase L3 in mV.
		 */
		SIMULATED_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Simulated voltage on L3")),

		/**
		 * Number of active phases.
		 */
		SIMULATED_PHASES(Doc.of(OpenemsType.INTEGER)//
				.text("Number of active phases")),

		/**
		 * Current limit in mA.
		 */
		SIMULATED_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current charging limit")),

		/**
		 * Device type being simulated.
		 */
		DEVICE_TYPE(Doc.of(DeviceType.values())//
				.text("Device type being simulated")),

		/**
		 * Indicates the simulator is running.
		 */
		SIMULATOR_RUNNING(Doc.of(Level.OK)//
				.text("Simulator is running")),

		// Hardware input channels (read from real hardware)
		/**
		 * Hardware input enabled.
		 */
		HARDWARE_INPUT_ENABLED(Doc.of(OpenemsType.BOOLEAN)//
				.text("Hardware input is enabled")),

		/**
		 * Hardware connected status.
		 */
		HARDWARE_CONNECTED(Doc.of(OpenemsType.BOOLEAN)//
				.text("Real hardware is connected")),

		/**
		 * Hardware state (read from device).
		 */
		HW_STATE(Doc.of(ChargePointState.values())//
				.text("State read from hardware")),

		/**
		 * Hardware power in W.
		 */
		HW_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.text("Power read from hardware")),

		/**
		 * Hardware energy in Wh.
		 */
		HW_ENERGY(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT_HOURS)//
				.text("Energy read from hardware")),

		/**
		 * Hardware current L1 in mA.
		 */
		HW_CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current L1 from hardware")),

		/**
		 * Hardware current L2 in mA.
		 */
		HW_CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current L2 from hardware")),

		/**
		 * Hardware current L3 in mA.
		 */
		HW_CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE)//
				.text("Current L3 from hardware")),

		/**
		 * Hardware voltage L1 in mV.
		 */
		HW_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Voltage L1 from hardware")),

		/**
		 * Hardware voltage L2 in mV.
		 */
		HW_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Voltage L2 from hardware")),

		/**
		 * Hardware voltage L3 in mV.
		 */
		HW_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.text("Voltage L3 from hardware")),

		// Copy control channels
		/**
		 * Copy hardware values to simulator.
		 */
		COPY_HW_TO_SIM(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Copy hardware values to simulator")),

		/**
		 * Copy simulator values to hardware output (for comparison).
		 */
		COPY_SIM_TO_HW(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("Use simulator values as reference"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// Channel accessors for control channels
	/**
	 * Gets the Channel for {@link ChannelId#SIMULATE_PLUG_IN}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSimulatePlugInChannel() {
		return this.channel(ChannelId.SIMULATE_PLUG_IN);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIMULATE_UNPLUG}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSimulateUnplugChannel() {
		return this.channel(ChannelId.SIMULATE_UNPLUG);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetCurrentLimitChannel() {
		return this.channel(ChannelId.SET_CURRENT_LIMIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIMULATE_ERROR}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSimulateErrorChannel() {
		return this.channel(ChannelId.SIMULATE_ERROR);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CLEAR_ERROR}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getClearErrorChannel() {
		return this.channel(ChannelId.CLEAR_ERROR);
	}

	// Channel accessors for monitoring channels
	/**
	 * Gets the simulated charging state.
	 *
	 * @return the state value
	 */
	public default Value<ChargePointState> getSimulatedState() {
		return this.<ChargePointState>channel(ChannelId.SIMULATED_STATE).value();
	}

	/**
	 * Gets the simulated power in Watts.
	 *
	 * @return the power value
	 */
	public default Value<Integer> getSimulatedPower() {
		return this.<Integer>channel(ChannelId.SIMULATED_POWER).value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIMULATED_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSimulatedPowerChannel() {
		return this.channel(ChannelId.SIMULATED_POWER);
	}

	/**
	 * Gets the session energy in Wh.
	 *
	 * @return the energy value
	 */
	public default Value<Long> getSimulatedEnergySession() {
		return this.<Long>channel(ChannelId.SIMULATED_ENERGY_SESSION).value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIMULATED_ENERGY_SESSION}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getSimulatedEnergySessionChannel() {
		return this.channel(ChannelId.SIMULATED_ENERGY_SESSION);
	}

	/**
	 * Gets the total energy in Wh.
	 *
	 * @return the energy value
	 */
	public default Value<Long> getSimulatedEnergyTotal() {
		return this.<Long>channel(ChannelId.SIMULATED_ENERGY_TOTAL).value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIMULATED_ENERGY_TOTAL}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getSimulatedEnergyTotalChannel() {
		return this.channel(ChannelId.SIMULATED_ENERGY_TOTAL);
	}

	// Internal setters
	/**
	 * Internal method to set the simulated state.
	 *
	 * @param value the state
	 */
	public default void _setSimulatedState(ChargePointState value) {
		this.channel(ChannelId.SIMULATED_STATE).setNextValue(value);
	}

	/**
	 * Internal method to set the simulated power.
	 *
	 * @param value the power in W
	 */
	public default void _setSimulatedPower(int value) {
		this.channel(ChannelId.SIMULATED_POWER).setNextValue(value);
	}

	/**
	 * Internal method to set the session energy.
	 *
	 * @param value the energy in Wh
	 */
	public default void _setSimulatedEnergySession(long value) {
		this.channel(ChannelId.SIMULATED_ENERGY_SESSION).setNextValue(value);
	}

	/**
	 * Internal method to set the total energy.
	 *
	 * @param value the energy in Wh
	 */
	public default void _setSimulatedEnergyTotal(long value) {
		this.channel(ChannelId.SIMULATED_ENERGY_TOTAL).setNextValue(value);
	}

	/**
	 * Internal method to set current on L1.
	 *
	 * @param value the current in mA
	 */
	public default void _setSimulatedCurrentL1(int value) {
		this.channel(ChannelId.SIMULATED_CURRENT_L1).setNextValue(value);
	}

	/**
	 * Internal method to set current on L2.
	 *
	 * @param value the current in mA
	 */
	public default void _setSimulatedCurrentL2(int value) {
		this.channel(ChannelId.SIMULATED_CURRENT_L2).setNextValue(value);
	}

	/**
	 * Internal method to set current on L3.
	 *
	 * @param value the current in mA
	 */
	public default void _setSimulatedCurrentL3(int value) {
		this.channel(ChannelId.SIMULATED_CURRENT_L3).setNextValue(value);
	}

	/**
	 * Internal method to set voltage on L1.
	 *
	 * @param value the voltage in mV
	 */
	public default void _setSimulatedVoltageL1(int value) {
		this.channel(ChannelId.SIMULATED_VOLTAGE_L1).setNextValue(value);
	}

	/**
	 * Internal method to set voltage on L2.
	 *
	 * @param value the voltage in mV
	 */
	public default void _setSimulatedVoltageL2(int value) {
		this.channel(ChannelId.SIMULATED_VOLTAGE_L2).setNextValue(value);
	}

	/**
	 * Internal method to set voltage on L3.
	 *
	 * @param value the voltage in mV
	 */
	public default void _setSimulatedVoltageL3(int value) {
		this.channel(ChannelId.SIMULATED_VOLTAGE_L3).setNextValue(value);
	}

	/**
	 * Internal method to set number of phases.
	 *
	 * @param value the number of phases
	 */
	public default void _setSimulatedPhases(int value) {
		this.channel(ChannelId.SIMULATED_PHASES).setNextValue(value);
	}

	/**
	 * Internal method to set current limit.
	 *
	 * @param value the current limit in mA
	 */
	public default void _setSimulatedCurrentLimit(int value) {
		this.channel(ChannelId.SIMULATED_CURRENT_LIMIT).setNextValue(value);
	}

	/**
	 * Internal method to set device type.
	 *
	 * @param value the device type
	 */
	public default void _setDeviceType(DeviceType value) {
		this.channel(ChannelId.DEVICE_TYPE).setNextValue(value);
	}

	/**
	 * Internal method to set simulator running state.
	 *
	 * @param value true if running
	 */
	public default void _setSimulatorRunning(boolean value) {
		this.channel(ChannelId.SIMULATOR_RUNNING).setNextValue(value);
	}
}
