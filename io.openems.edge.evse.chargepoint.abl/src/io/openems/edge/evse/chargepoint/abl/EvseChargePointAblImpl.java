package io.openems.edge.evse.chargepoint.abl;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1FaultState;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1State;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * ABL eMH1 EVSE Charge-Point driver.
 *
 * <p>
 * Behaviour specification: {@code doc/behaviour-goals.adoc}.
 *
 * <p>
 * Protocol: Modbus/ASCII over serial (BridgeModbusSerialAscii with ABL
 * compatibility flag). The device responds with {@code >} instead of {@code :}
 * as the frame-start character.
 *
 * <p>
 * Goal → test method mapping (see {@code EvseChargePointAblImplTest}):
 * <ul>
 * <li>G0a Hardware model selection → testG0a_HardwareModel
 * <li>G0b Voltage source config → testG0b_VoltageFallback
 * <li>G1 Phase currents and calculated active power → testG1_ThreePhaseMonitoring,
 * testG1_OnePhaseMonitoring, testG1_CurrentUnavailable
 * <li>G2 Charging state → testG2_StateMapping
 * <li>G3a Per-session energy → testG3a_SessionEnergy
 * <li>G3b Lifetime energy → testG3b_LifetimeEnergy
 * <li>G5 Set charge current limit → testG5_SetChargeLimit
 * <li>G7 Fault reporting → testG7_FaultStateReporting
 * <li>G8 Communication failure → testG8_CommunicationFailure
 * <li>G10 Read-only blocks writes → testG10_ReadOnlyBlocksWrites
 * <li>G11 Debug write frame log → testG11_DebugLogVerbosity
 * <li>G13 System flags → testG13_SystemFlags
 * </ul>
 *
 * <p>
 * Hypothesis tests (hardware validation required):
 * <ul>
 * <li>G4a Startup outlet enable → testG4a_StartupOutletEnable
 * <li>G4b Hard outlet disable → testG4b_OutletDisable
 * <li>G6 Soft pause → testG6_SoftPause
 * <li>G9 Upstream timeout config → testG9_UpstreamTimeout
 * </ul>
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Abl.EMh1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EvseChargePointAblImpl extends AbstractOpenemsModbusComponent //
		implements EvseChargePointAbl, EvseChargePoint, ElectricityMeter, OpenemsComponent, //
		TimedataProvider, EventHandler, ModbusComponent {

	// Register addresses
	private static final int REG_R5_STATE = 0x002E; // R5 start: state + duty cycle + currents (5 regs)
	private static final int REG_R5_FLAGS = 0x002F; // bit15=EV connected; bits[11:0]=duty cycle×10
	private static final int REG_R5_ICT1 = 0x0030; // I_ct1 [0.1A]; 0x03E8=unavailable
	private static final int REG_R5_ICT2 = 0x0031; // I_ct2 [0.1A]
	private static final int REG_R5_ICT3 = 0x0032; // I_ct3 [0.1A]
	private static final int REG_SYS_FLAGS_STATE = 0x0006; // [15:8]=0x06 ref; [7:0]=state byte (cross-check)
	private static final int REG_SYS_FLAGS = 0x0007; // bit9=upstream lost, bit8=imbalance, bit7=temp, bit6=meter fail
	private static final int REG_SET_CURRENT = 0x0014; // Write: round(targetA / 0.6 * 10); pause=0x03E8
	private static final int REG_OUTLET_CONTROL = 0x0005; // Write: 0xE2E2=enable, 0xE0E0=disable
	private static final int REG_TIMEOUT_CONFIG = 0x002C; // Write: 0x9001=timeout+autoB1B2+deviceId1

	/** Unavailable current sentinel in R5/R3 registers (state A or metering failed). */
	private static final int CURRENT_UNAVAILABLE_SENTINEL = 0x03E8;

	private final CalculateEnergyFromPower calculateConsumptionEnergy = //
			new CalculateEnergyFromPower(this, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private Config config = null;

	/** EV connection state derived from EVSE state each cycle. */
	private boolean evConnected = false;

	/** Ready-for-charging state derived from EVSE state each cycle. */
	private boolean readyForCharging = false;

	/** Per-session energy accumulator [Wh]. Resets on new session. */
	private int sessionEnergyWh = 0;

	/**
	 * True when the previous cycle had the EV disconnected (A1/E0/E2/Fx states).
	 * Used to detect the start of a new session and reset SESSION_ENERGY.
	 */
	private boolean wasDisconnected = true;

	/** Tracks elapsed time for session energy integration (ms). */
	private long lastCycleTimestampMs = 0;

	/**
	 * Number of consecutive cycles with no Modbus data (EVSE_STATE == null).
	 * Triggers {@link ModbusComponent.ChannelId#MODBUS_COMMUNICATION_FAILED} when
	 * this reaches {@link #COMM_FAILURE_CYCLES_THRESHOLD}.
	 */
	private int noDataCycles = 0;
	private static final int COMM_FAILURE_CYCLES_THRESHOLD = 5;

	/** Raw setpoint for minimum current (6 A): round(6 / 0.6 * 10) = 100. */
	private static final int MIN_CURRENT_RAW = 100;

	/** Whether charging is currently soft-paused (G6). */
	private boolean isPaused = false;

	/** Last raw setpoint written via {@link #apply}; restored on resume. */
	private int lastSetpointRaw = MIN_CURRENT_RAW;

	public EvseChargePointAblImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointAbl.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		this.getChargingPausedChannel().onSetNextWrite(v -> {
			if (v != null) {
				this.applyPause(v);
			}
		});
		this.getSetCurrentAmpereChannel().onSetNextWrite(v -> {
			if (v != null) {
				this.applySetCurrentAmpere(v);
			}
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), //
				config.modbusUnitId(), this.cm, "Modbus", config.modbus_id())) {
			return;
		}
		// Phase B: startup sequence — write 0xE2E2 if state is A1/E0, then 0x9001
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), //
				config.modbusUnitId(), this.cm, "Modbus", config.modbus_id())) {
			return;
		}
		// Phase B: re-apply startup sequence on config change
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.onBeforeProcessImage();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.onAfterProcessImage();
			break;
		}
	}

	/**
	 * Sets phase voltages from configured channel addresses, falling back to the
	 * configured static values. Runs before Modbus reads.
	 */
	private void onBeforeProcessImage() {
		this._setVoltageL1(this.resolveVoltageMv(this.config.voltageL1ChannelAddress(), this.config.fallbackVoltageL1()));
		this._setVoltageL2(this.resolveVoltageMv(this.config.voltageL2ChannelAddress(), this.config.fallbackVoltageL2()));
		this._setVoltageL3(this.resolveVoltageMv(this.config.voltageL3ChannelAddress(), this.config.fallbackVoltageL3()));
	}

	/**
	 * Resolves a phase voltage [mV]. Reads from the configured channel address if
	 * non-empty and the channel carries a defined value; falls back to the
	 * configured static fallback voltage otherwise.
	 *
	 * @param channelAddress optional channel address (e.g. "meter0/VoltageL1")
	 * @param fallbackV      static fallback voltage [V]
	 * @return resolved voltage [mV]
	 */
	private int resolveVoltageMv(String channelAddress, int fallbackV) {
		if (channelAddress != null && !channelAddress.isBlank()) {
			try {
				@SuppressWarnings("unchecked")
				var ch = (Channel<Integer>) this.componentManager
						.getChannel(ChannelAddress.fromString(channelAddress));
				var v = ch.value();
				if (v.isDefined()) {
					return v.get();
				}
			} catch (Exception e) {
				// channel not found or value unavailable — fall through to fallback
			}
		}
		return fallbackV * 1_000;
	}

	/**
	 * Processes stable channel values from this cycle's Modbus reads. Runs after
	 * nextProcessImage(), so channel.value() is current-cycle data.
	 */
	private void onAfterProcessImage() {
		// EV state — read from stable channel value
		final AblEMh1State state = (AblEMh1State) this.getEvseStateChannel().value().asEnum();

		// EV connection and ready-for-charging flags
		this.evConnected = state.isEvConnected();
		this.readyForCharging = state.isReadyForCharging();
		this.channel(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING).setNextValue(this.readyForCharging);

		// Fault state
		final var faultState = AblEMh1FaultState.fromDeviceState(state);
		this.channel(EvseChargePointAbl.ChannelId.FAULT_STATE).setNextValue(faultState.getValue());

		// Reflect soft-pause state so the channel read value stays current
		this.channel(EvseChargePointAbl.ChannelId.CHARGING_PAUSED).setNextValue(this.isPaused);

		// Active power [W] from current-cycle Modbus values and resolved voltages
		// (set by onBeforeProcessImage — external channel or fallback)
		final int vL1mV = this.getVoltageL1Channel().value().orElse(this.config.fallbackVoltageL1() * 1_000);
		final int vL2mV = this.getVoltageL2Channel().value().orElse(this.config.fallbackVoltageL2() * 1_000);
		final int vL3mV = this.getVoltageL3Channel().value().orElse(this.config.fallbackVoltageL3() * 1_000);
		final int cL1 = this.getCurrentL1Channel().value().orElse(0);
		final int cL2 = this.getCurrentL2Channel().value().orElse(0);
		final int cL3 = this.getCurrentL3Channel().value().orElse(0);
		final int activePowerW = (int) ((long) cL1 * vL1mV / 1_000_000
				+ (long) cL2 * vL2mV / 1_000_000
				+ (long) cL3 * vL3mV / 1_000_000);
		this._setActivePower(activePowerW);

		// System flags
		final int sysFlags = this.getSysFlagsRawChannel().value().orElse(0);
		this.getUpstreamCommLostChannel().setNextValue(((sysFlags >> 9) & 1) != 0);
		this.getLoadImbalanceChannel().setNextValue(((sysFlags >> 8) & 1) != 0);
		this.getTemperatureWarningChannel().setNextValue(((sysFlags >> 7) & 1) != 0);
		this.getPhaseMeteringFailedChannel().setNextValue(((sysFlags >> 6) & 1) != 0);

		// Session energy: reset on new session, accumulate while charging
		if (this.evConnected && this.wasDisconnected) {
			this.sessionEnergyWh = 0;
			this.lastCycleTimestampMs = System.currentTimeMillis();
		} else if (this.evConnected && activePowerW > 0 && this.lastCycleTimestampMs > 0) {
			final var nowMs = System.currentTimeMillis();
			final var elapsedH = (nowMs - this.lastCycleTimestampMs) / 3_600_000.0;
			this.sessionEnergyWh += (int) (activePowerW * elapsedH);
			this.lastCycleTimestampMs = nowMs;
		}
		this.wasDisconnected = !this.evConnected;
		this.channel(EvseChargePointAbl.ChannelId.SESSION_ENERGY).setNextValue(this.sessionEnergyWh);

		// Lifetime energy accumulation
		this.calculateConsumptionEnergy.update(activePowerW);

		// Communication failure watchdog: state == UNDEFINED when no Modbus data arrived
		if (state == AblEMh1State.UNDEFINED) {
			if (++this.noDataCycles >= COMM_FAILURE_CYCLES_THRESHOLD) {
				this._setModbusCommunicationFailed(true);
			}
		} else {
			this.noDataCycles = 0;
			this._setModbusCommunicationFailed(false);
		}
	}

	@Override
	public boolean isReadOnly() {
		return this.config != null && this.config.readOnly();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config != null ? this.config.phaseRotation() : PhaseRotation.L1_L2_L3;
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		// Phase B: use hardwareType from config for accurate phase and max current
		final var phase = this.config != null ? this.config.hardwareType().getPhase() : THREE_PHASE;
		final var maxA = this.config != null ? this.config.hardwareType().getMaxCurrentInAmpere() : 32;
		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phase, 6, maxA)) //
				.setIsEvConnected(this.evConnected) //
				.setIsReadyForCharging(this.readyForCharging) //
				.build();
	}

	/**
	 * Applies a soft pause or resume to register 0x0014.
	 *
	 * <p>
	 * {@code true}: writes the pause sentinel {@link #CURRENT_UNAVAILABLE_SENTINEL}
	 * (0x03E8), suspending current while the CP pilot remains active. {@code false}:
	 * restores {@link #lastSetpointRaw} (or {@link #MIN_CURRENT_RAW} if no setpoint
	 * has been applied since startup). Ignored when read-only.
	 */
	private void applyPause(boolean pause) {
		if (this.isReadOnly()) {
			return;
		}
		this.isPaused = pause;
		try {
			this.getSetCurrentRawChannel().setNextWriteValue(
					pause ? CURRENT_UNAVAILABLE_SENTINEL : this.lastSetpointRaw);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to write SET_CURRENT_RAW for pause: " + e.getMessage());
		}
	}

	/**
	 * Handles a direct write to {@link EvseChargePointAbl.ChannelId#SET_CURRENT_AMPERE}.
	 *
	 * <p>
	 * Clamps the value to [6 A, hardware maximum], converts to raw, saves as
	 * {@link #lastSetpointRaw}, and writes to the device unless paused or read-only.
	 */
	private void applySetCurrentAmpere(int ampere) {
		if (this.isReadOnly()) {
			return;
		}
		final var maxA = this.config != null ? this.config.hardwareType().getMaxCurrentInAmpere() : 32;
		final var clamped = Math.max(6, Math.min(maxA, ampere));
		final var raw = (int) Math.round(clamped / 0.6 * 10);
		this.lastSetpointRaw = raw;
		if (this.isPaused) {
			return; // setpoint saved; written to device on resume
		}
		try {
			this.getSetCurrentRawChannel().setNextWriteValue(raw);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to write SET_CURRENT_RAW from SET_CURRENT_AMPERE: " + e.getMessage());
		}
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config != null && this.config.readOnly()) {
			return;
		}
		if (!(actions.applySetPoint() instanceof ApplySetPoint.Action.Ampere a)) {
			return;
		}
		final var maxA = this.config != null ? this.config.hardwareType().getMaxCurrentInAmpere() : 32;
		final var targetA = Math.max(6, Math.min(maxA, a.value()));
		// Setpoint formula: round(targetAmpere / 0.6 * 10)
		// e.g. 16A → round(16 / 0.6 * 10) = round(266.67) = 267
		final var raw = (int) Math.round(targetA / 0.6 * 10);
		this.lastSetpointRaw = raw;
		if (this.isPaused) {
			return; // setpoint saved; written to device on resume
		}
		try {
			this.getSetCurrentRawChannel().setNextWriteValue(raw);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to write SET_CURRENT_RAW: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotation = this.getPhaseRotation();

		// Converter: device unit 0.1A → mA (×100); unavailable sentinel 0x03E8 → 0
		final ElementToChannelConverter currentConverter = new ElementToChannelConverter(v -> {
			if (v == null) {
				return null;
			}
			final int raw = ((Number) v).intValue();
			return raw == CURRENT_UNAVAILABLE_SENTINEL ? 0 : raw * 100;
		});

		// Converter: extract low byte as EVSE state code (high byte is address reference)
		final ElementToChannelConverter stateConverter = new ElementToChannelConverter(v -> {
			if (v == null) {
				return null;
			}
			return ((Number) v).intValue() & 0xFF;
		});

		// Converter: raw duty-cycle ↔ A; pause sentinel 0x03E8 → 0
		// Forward (element→channel): A = round(raw * 0.6 / 10); sentinel 0x03E8 → 0
		// Backward (channel→element): raw = round(A / 0.6 * 10)
		final ElementToChannelConverter setpointConverter = new ElementToChannelConverter(
				v -> {
					if (v == null) {
						return null;
					}
					final int raw = ((Number) v).intValue();
					return raw == CURRENT_UNAVAILABLE_SENTINEL ? 0 : (int) Math.round(raw * 0.6 / 10);
				},
				v -> {
					if (v == null) {
						return null;
					}
					return (int) Math.round(((Number) v).intValue() / 0.6 * 10);
				});

		return new ModbusProtocol(this,
				// R5 block: 5 consecutive registers 0x002E–0x0032
				// reg 46: [15:8]=0x2E ref; [7:0]=state byte → EVSE_STATE
				// reg 47: duty-cycle / EV-connected flag — skipped (state byte is authoritative)
				// regs 48–50: I_ct1/2/3 [0.1A] → CURRENT_L1/L2/L3 [mA]
				new FC3ReadRegistersTask(REG_R5_STATE, Priority.HIGH, //
						m(EvseChargePointAbl.ChannelId.EVSE_STATE, new UnsignedWordElement(REG_R5_STATE),
								stateConverter), //
						new DummyRegisterElement(REG_R5_FLAGS), //
						m(phaseRotation.channelCurrentL1(), new UnsignedWordElement(REG_R5_ICT1), currentConverter), //
						m(phaseRotation.channelCurrentL2(), new UnsignedWordElement(REG_R5_ICT2), currentConverter), //
						m(phaseRotation.channelCurrentL3(), new UnsignedWordElement(REG_R5_ICT3), currentConverter)), //
				// System-flags block: 2 consecutive registers 0x0006–0x0007
				// reg 6: state cross-check — skipped
				// reg 7: bit9=upstream lost, bit8=imbalance, bit7=temp, bit6=metering fail
				new FC3ReadRegistersTask(REG_SYS_FLAGS_STATE, Priority.LOW, //
						new DummyRegisterElement(REG_SYS_FLAGS_STATE), //
						m(EvseChargePointAbl.ChannelId.SYS_FLAGS_RAW, new UnsignedWordElement(REG_SYS_FLAGS))), //
				// Current setpoint read-back: register 0x0014 → SET_CURRENT_AMPERE [A]
				new FC3ReadRegistersTask(REG_SET_CURRENT, Priority.LOW, //
						m(EvseChargePointAbl.ChannelId.SET_CURRENT_AMPERE, new UnsignedWordElement(REG_SET_CURRENT),
								setpointConverter)), //
				// Write: charge current setpoint → register 0x0014
				// Note: ABL eMH1 responds to FC6 with corrupt frame; use FC16 instead
				new FC16WriteRegistersTask(REG_SET_CURRENT, //
						m(EvseChargePointAbl.ChannelId.SET_CURRENT_RAW, new UnsignedWordElement(REG_SET_CURRENT))) //
		);
	}

	@Override
	public String debugLog() {
		return "State:" + this.getEvseState() //
				+ "|EV:" + this.evConnected //
				+ "|Ready:" + this.readyForCharging //
				+ "|P:" + this.getActivePower().orElse(null) + "W";
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
