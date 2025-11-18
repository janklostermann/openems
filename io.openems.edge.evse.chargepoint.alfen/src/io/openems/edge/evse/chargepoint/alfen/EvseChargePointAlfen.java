package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
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
