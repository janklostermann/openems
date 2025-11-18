package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alfen.enums.ChargingState;

public interface EvseChargePointAlfen extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Charging State.
		 *
		 * <p>
		 * According to IEC 61851-1
		 */
		CHARGING_STATE(Doc.of(ChargingState.values())),

		/**
		 * Raw charging state string from Modbus.
		 */
		RAW_CHARGING_STATE(Doc.of(STRING)),

		/**
		 * Debug channel for set charging current.
		 */
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)), //

		/**
		 * Set charging current in milliampere.
		 */
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),

		/**
		 * Current phase configuration.
		 *
		 * <p>
		 * 1 = single phase, 3 = three phase
		 */
		PHASE_CONFIGURATION(Doc.of(INTEGER) //
				.accessMode(READ_WRITE) //
				.text("Phase configuration (1 or 3)")),

		/**
		 * Total energy delivered in Wh.
		 */
		TOTAL_ENERGY(Doc.of(LONG) //
				.unit(WATT_HOURS)),

		/**
		 * Temperature of the charging station.
		 */
		TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)),

		// Simulation mode channels
		/**
		 * Simulation mode active.
		 */
		SIMULATION_MODE(Doc.of(BOOLEAN) //
				.text("Simulation mode is active")),

		/**
		 * Simulate plugging in a vehicle.
		 */
		SIMULATE_PLUG_IN(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Simulate plugging in a vehicle")),

		/**
		 * Simulate unplugging a vehicle.
		 */
		SIMULATE_UNPLUG(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Simulate unplugging a vehicle")),

		/**
		 * Simulate an error condition.
		 */
		SIMULATE_ERROR(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Simulate an error condition")),

		/**
		 * Clear simulated error.
		 */
		SIMULATE_CLEAR_ERROR(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Clear simulated error")),

		// Debug mode channels - raw register values
		/**
		 * Debug mode active.
		 */
		DEBUG_MODE(Doc.of(BOOLEAN) //
				.text("Debug mode is active")),

		/**
		 * Raw voltage L1 register value.
		 */
		RAW_VOLTAGE_L1(Doc.of(STRING) //
				.text("Raw voltage L1 registers (hex)")),

		/**
		 * Raw voltage L2 register value.
		 */
		RAW_VOLTAGE_L2(Doc.of(STRING) //
				.text("Raw voltage L2 registers (hex)")),

		/**
		 * Raw voltage L3 register value.
		 */
		RAW_VOLTAGE_L3(Doc.of(STRING) //
				.text("Raw voltage L3 registers (hex)")),

		/**
		 * Raw current L1 register value.
		 */
		RAW_CURRENT_L1(Doc.of(STRING) //
				.text("Raw current L1 registers (hex)")),

		/**
		 * Raw current L2 register value.
		 */
		RAW_CURRENT_L2(Doc.of(STRING) //
				.text("Raw current L2 registers (hex)")),

		/**
		 * Raw current L3 register value.
		 */
		RAW_CURRENT_L3(Doc.of(STRING) //
				.text("Raw current L3 registers (hex)")),

		/**
		 * Raw power register value.
		 */
		RAW_POWER(Doc.of(STRING) //
				.text("Raw power registers (hex)")),

		/**
		 * Raw energy register value.
		 */
		RAW_ENERGY(Doc.of(STRING) //
				.text("Raw energy registers (hex)")),

		/**
		 * Raw state register value.
		 */
		RAW_STATE(Doc.of(STRING) //
				.text("Raw state registers (hex)")),

		/**
		 * Raw max current register value.
		 */
		RAW_MAX_CURRENT(Doc.of(STRING) //
				.text("Raw max current registers (hex)")),

		// Override controls for debug mode
		/**
		 * Override voltage with manual value.
		 */
		OVERRIDE_VOLTAGE(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Override voltage with manual value")),

		/**
		 * Override current with manual value.
		 */
		OVERRIDE_CURRENT(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Override current with manual value")),

		/**
		 * Override power with manual value.
		 */
		OVERRIDE_POWER(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Override power with manual value")),

		/**
		 * Override state with manual value.
		 */
		OVERRIDE_STATE(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE) //
				.text("Override state with manual value")),

		/**
		 * Manual voltage L1 value in V.
		 */
		MANUAL_VOLTAGE_L1(Doc.of(INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(READ_WRITE) //
				.text("Manual voltage L1")),

		/**
		 * Manual voltage L2 value in V.
		 */
		MANUAL_VOLTAGE_L2(Doc.of(INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(READ_WRITE) //
				.text("Manual voltage L2")),

		/**
		 * Manual voltage L3 value in V.
		 */
		MANUAL_VOLTAGE_L3(Doc.of(INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(READ_WRITE) //
				.text("Manual voltage L3")),

		/**
		 * Manual current L1 value in mA.
		 */
		MANUAL_CURRENT_L1(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(READ_WRITE) //
				.text("Manual current L1")),

		/**
		 * Manual current L2 value in mA.
		 */
		MANUAL_CURRENT_L2(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(READ_WRITE) //
				.text("Manual current L2")),

		/**
		 * Manual current L3 value in mA.
		 */
		MANUAL_CURRENT_L3(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(READ_WRITE) //
				.text("Manual current L3")),

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getChargingStateChannel() {
		return this.channel(ChannelId.CHARGING_STATE);
	}

	/**
	 * Gets the current {@link ChargingState}.
	 *
	 * @return the {@link ChargingState}
	 */
	public default ChargingState getChargingState() {
		return this.getChargingStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the write value of the {@link ChannelId#SET_CHARGING_CURRENT} Channel
	 * used to set the charge current limit of the Charge-Point in [mA].
	 *
	 * @param value the next value in milliampere
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargingCurrent(Integer value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_CONFIGURATION}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getPhaseConfigurationChannel() {
		return this.channel(ChannelId.PHASE_CONFIGURATION);
	}

	/**
	 * Gets the current phase configuration.
	 *
	 * @return the phase configuration (1 or 3)
	 */
	public default Value<Integer> getPhaseConfiguration() {
		return this.getPhaseConfigurationChannel().value();
	}

	/**
	 * Sets the phase configuration.
	 *
	 * @param phases the number of phases (1 or 3)
	 * @throws OpenemsNamedException on error
	 */
	public default void setPhaseConfiguration(Integer phases) throws OpenemsNamedException {
		this.getPhaseConfigurationChannel().setNextWriteValue(phases);
	}
}
