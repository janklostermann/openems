package io.openems.edge.evse.chargepoint.abl;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1FaultState;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1State;

/**
 * ABL eMH1 EVSE Charge-Point channel interface.
 *
 * <p>
 * Full behaviour specification: {@code doc/behaviour-goals.adoc} in this module.
 */
@ProviderType
public interface EvseChargePointAbl extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * EVSE State.
		 *
		 * <p>
		 * Current operational state as reported by the device (state byte from R5
		 * register 0x002E or R3 register 0x0034). Encoding: A1→0xA1, C2→0xC2,
		 * F4→0xF4, etc.
		 */
		EVSE_STATE(Doc.of(AblEMh1State.values())), //

		/**
		 * Fault State.
		 *
		 * <p>
		 * Set to the specific {@link AblEMh1FaultState} when the device reports an
		 * F-state. {@link AblEMh1FaultState#UNDEFINED} when no fault is active.
		 */
		FAULT_STATE(Doc.of(AblEMh1FaultState.values())), //

		/**
		 * Session Energy [Wh].
		 *
		 * <p>
		 * Per-session energy accumulator. Resets to 0 on each new EV connection.
		 * Retains the final value after the session ends until the next session starts.
		 * Not persisted across restarts (intentional — a partial session value after a
		 * restart would be misleading).
		 */
		SESSION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Per-session energy [Wh]. Resets to 0 on each new EV connection.")), //

		/**
		 * Outlet Enabled.
		 *
		 * <p>
		 * Write {@code false} to send 0xE0E0 to register 0x0005 (hard outlet disable —
		 * session ends, cable may unlock). Write {@code true} to send 0xE2E2 (re-enable
		 * outlet). Compatible with {@code Controller.IO.ChannelSingleThreshold}.
		 *
		 * <p>
		 * Semantic distinction: this is a hard hardware disable. For a soft pause that
		 * keeps the EV connected, use {@link ChannelId#CHARGING_PAUSED}.
		 */
		OUTLET_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Enable (true) or disable (false) the charging outlet.")), //

		/**
		 * Charging Paused (soft pause, G6).
		 *
		 * <p>
		 * Write {@code true} to send the pause sentinel (0x03E8) to register 0x0014,
		 * suspending current flow while keeping the CP pilot signal active — the EV
		 * stays connected and the session is not interrupted. Write {@code false} to
		 * resume: the last current setpoint applied via
		 * {@link io.openems.edge.evse.api.chargepoint.EvseChargePoint#apply} is
		 * re-sent (or minimum 6 A if no setpoint has been applied since startup).
		 *
		 * <p>
		 * Semantic distinction from {@link ChannelId#OUTLET_ENABLED}: pause is soft —
		 * the CP pilot signal remains active and the EV does not disconnect.
		 * {@code OUTLET_ENABLED = false} is a hard hardware disable — the session ends
		 * and the cable may unlock.
		 *
		 * <p>
		 * Compatible with {@code Controller.IO.ChannelSingleThreshold}.
		 */
		CHARGING_PAUSED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Soft-pause: true = suspend current (0x03E8, EV stays connected); false = resume at last setpoint.")), //

		/**
		 * Upstream Communication Lost.
		 *
		 * <p>
		 * Set when system-flags register 0x0007 bit 9 = 1 (BC6 condition). May
		 * eventually trigger fault F4/F14 if upstream timeout is enabled.
		 */
		UPSTREAM_COMM_LOST(Doc.of(Level.WARNING) //
				.text("ABL eMH1: upstream communication lost (BC6)")), //

		/**
		 * Load Imbalance Detected.
		 *
		 * <p>
		 * Set when system-flags register 0x0007 bit 8 = 1 (BC3 condition). May cause
		 * current reduction (state C4).
		 */
		LOAD_IMBALANCE(Doc.of(Level.WARNING) //
				.text("ABL eMH1: load imbalance detected (BC3)")), //

		/**
		 * Temperature Warning.
		 *
		 * <p>
		 * Set when system-flags register 0x0007 bit 7 = 1 (BC5 condition): device
		 * temperature is between 60 °C and 80 °C.
		 */
		TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("ABL eMH1: temperature warning 60–80 °C (BC5)")), //

		/**
		 * Phase Current Metering Failed.
		 *
		 * <p>
		 * Set when system-flags register 0x0007 bit 6 = 1 (BC4 condition). Current
		 * readings from R5 registers 0x0030–0x0032 are unreliable when this flag is
		 * active.
		 */
		PHASE_METERING_FAILED(Doc.of(Level.WARNING) //
				.text("ABL eMH1: phase current metering failed (BC4) — current readings unreliable")), //

		/**
		 * Charge Current Setpoint [A].
		 *
		 * <p>
		 * Read: the current setpoint read back from register 0x0014. Reflects whatever
		 * value was last written (by the controller via
		 * {@link io.openems.edge.evse.api.chargepoint.EvseChargePoint#apply} or by a
		 * direct channel write). Reads 0 A when the device is soft-paused
		 * ({@link ChannelId#CHARGING_PAUSED} = true, sentinel 0x03E8 in register).
		 *
		 * <p>
		 * Write: sets the charge current directly. Value is clamped to [6 A, hardware
		 * maximum]. Saves the value as the resume setpoint for
		 * {@link ChannelId#CHARGING_PAUSED}. Write is suppressed when
		 * {@link ChannelId#CHARGING_PAUSED} is active (value is remembered for resume)
		 * or when the device is read-only.
		 *
		 * <p>
		 * Compatible with {@code Controller.IO.ChannelSingleThreshold}.
		 */
		SET_CURRENT_AMPERE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Charge current setpoint [A], read back from register 0x0014.")), //

		/**
		 * Set Current Raw (internal).
		 *
		 * <p>
		 * Internal write channel mapped to Modbus register 0x0014 by
		 * {@code defineModbusProtocol()}. Value = {@code round(targetAmpere / 0.6 * 10)}.
		 * Range: 100 (6 A) to {@code round(maxA / 0.6 * 10)}. Pause sentinel: 1000
		 * (0x03E8 = 100 % duty cycle = no current allowed).
		 */
		SET_CURRENT_RAW(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.text("Internal: raw duty-cycle value written to register 0x0014.")), //

		/**
		 * System Flags Raw (internal).
		 *
		 * <p>
		 * Internal read channel mapped to Modbus register 0x0007 by
		 * {@code defineModbusProtocol()}. Individual flag bits are extracted in
		 * {@code onBeforeProcessImage()} to populate {@link ChannelId#UPSTREAM_COMM_LOST},
		 * {@link ChannelId#LOAD_IMBALANCE}, {@link ChannelId#TEMPERATURE_WARNING} and
		 * {@link ChannelId#PHASE_METERING_FAILED}.
		 */
		SYS_FLAGS_RAW(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("Internal: raw value of system-flags register 0x0007.")), //
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
	 * Gets the Channel for {@link ChannelId#EVSE_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<AblEMh1State> getEvseStateChannel() {
		return this.channel(ChannelId.EVSE_STATE);
	}

	/**
	 * Gets the current {@link AblEMh1State}. See {@link ChannelId#EVSE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default AblEMh1State getEvseState() {
		return this.getEvseStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#FAULT_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<AblEMh1FaultState> getFaultStateChannel() {
		return this.channel(ChannelId.FAULT_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getSessionEnergyChannel() {
		return this.channel(ChannelId.SESSION_ENERGY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OUTLET_ENABLED}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getOutletEnabledChannel() {
		return this.channel(ChannelId.OUTLET_ENABLED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_PAUSED}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getChargingPausedChannel() {
		return this.channel(ChannelId.CHARGING_PAUSED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UPSTREAM_COMM_LOST}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getUpstreamCommLostChannel() {
		return this.channel(ChannelId.UPSTREAM_COMM_LOST);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOAD_IMBALANCE}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getLoadImbalanceChannel() {
		return this.channel(ChannelId.LOAD_IMBALANCE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getTemperatureWarningChannel() {
		return this.channel(ChannelId.TEMPERATURE_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_METERING_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getPhaseMeteringFailedChannel() {
		return this.channel(ChannelId.PHASE_METERING_FAILED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_AMPERE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetCurrentAmpereChannel() {
		return this.channel(ChannelId.SET_CURRENT_AMPERE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_RAW}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetCurrentRawChannel() {
		return this.channel(ChannelId.SET_CURRENT_RAW);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SYS_FLAGS_RAW}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getSysFlagsRawChannel() {
		return this.channel(ChannelId.SYS_FLAGS_RAW);
	}
}
