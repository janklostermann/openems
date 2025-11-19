package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.types.Tuple;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.ABL", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointAblImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointAbl, ModbusComponent, OpenemsComponent, TimedataProvider, EvseChargePoint,
		EventHandler, ElectricityMeter {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointAblImpl.class);

	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private Config config;
	private Tuple<Instant, Integer> previousCurrent = null;
	private ChargePointSimulatorCore simulator;
	private long lastTickTime;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

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
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (config.simulationMode()) {
			// In simulation mode, we don't need a real Modbus connection
			super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
					"Modbus", "");
			this.initializeSimulator();
		} else {
			super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
					"Modbus", config.modbus_id());
		}
		this.applyConfig(config);
	}

	private void initializeSimulator() {
		this.simulator = new ChargePointSimulatorCore();
		this.simulator.setVoltages(230.0, 230.0, 230.0);
		this.simulator.setPhases(this.config != null && this.config.wiring() == SingleOrThreePhase.SINGLE_PHASE ? 1 : 3);
		this.lastTickTime = System.currentTimeMillis();
		setValue(this, EvseChargePointAbl.ChannelId.SIMULATION_MODE, true);
		this.logInfo(this.log, "Simulation mode activated");
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
		setValue(this, EvseChargePointAbl.ChannelId.DEBUG_MODE, config.debugMode());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		/*
		 * ABL EVCC2/3 Modbus Protocol Definition based on specification document
		 * "Schnittstellenbeschreibung_Modbus-ASCII.pdf"
		 *
		 * Note: The specification uses Modbus ASCII over RS485, but for Modbus TCP we
		 * use the same register addresses.
		 */

		var modbusProtocol = new ModbusProtocol(this, //

				// Register 0x0001-0x0002: Read device-ID and firmware-revision
				new FC3ReadRegistersTask(0x0001, Priority.LOW, //
						m(EvseChargePointAbl.ChannelId.DEVICE_ID, new UnsignedWordElement(0x0001),
								new ElementToChannelConverter(value -> {
									// Bits 21..16 contain the device-ID (0x01...0x10)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										return (val >> 16) & 0x1F; // Extract device ID from bits 21..16
									}
									return null;
								})),
						m(EvseChargePointAbl.ChannelId.FIRMWARE_VERSION, new UnsignedWordElement(0x0002),
								new ElementToChannelConverter(value -> {
									// Bits 15..12: Major revision, Bits 11..8: Minor revision
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int major = (val >> 12) & 0x0F;
										int minor = (val >> 8) & 0x0F;
										return major + "." + minor;
									}
									return "Unknown";
								}))),

				// Register 0x0033-0x0035: Read current state, EV connection, and phase currents
				// Each register is read once; multi-value extraction happens in EventHandler
				new FC3ReadRegistersTask(0x0033, Priority.HIGH, //
						m(EvseChargePointAbl.ChannelId.EV_CONNECTED, new UnsignedWordElement(0x0033),
								new ElementToChannelConverter(value -> {
									// Bit 39 (MSB of first register): 0 = UCP > 10V (no EV), 1 = UCP <= 10V (EV
									// connected)
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										return ((val >> 8) & 0x01) == 1; // Bit 39 is bit 8 of high byte
									}
									return false;
								})),
						m(EvseChargePointAbl.ChannelId.CHARGING_STATE, new UnsignedWordElement(0x0034),
								new ElementToChannelConverter(value -> {
									// High byte contains state, low byte contains L1 current
									// We extract state here, L1 current is handled in EventHandler
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int stateValue = (val >> 8) & 0xFF;
										// Also set L1 current as side effect
										int currentL1 = val & 0xFF;
										EvseChargePointAblImpl.this._setPhaseCurrentL1(currentL1 == 0x64 ? null : currentL1);
										return ChargingState.fromValue(stateValue);
									}
									return ChargingState.UNDEFINED;
								})),
						m(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2, new UnsignedWordElement(0x0035),
								new ElementToChannelConverter(value -> {
									// High byte contains L2 current, low byte contains L3 current
									Integer val = TypeUtils.getAsType(INTEGER, value);
									if (val != null) {
										int currentL2 = (val >> 8) & 0xFF;
										int currentL3 = val & 0xFF;
										// Set L3 as side effect
										EvseChargePointAblImpl.this._setPhaseCurrentL3(currentL3 == 0x64 ? null : currentL3);
										return currentL2 == 0x64 ? null : currentL2;
									}
									return null;
								}))),

				// Register 0x0014: Set Icmax (write only)
				// Value: Duty cycle Icmax [%]*10 (0x0050...0x03E8 = 8%...100%)
				// According to spec: 6A corresponds to ~10% duty cycle (0x0064)
				// 32A corresponds to ~53% duty cycle (0x0214)
				new FC16WriteRegistersTask(0x0014, //
						m(EvseChargePointAbl.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(0x0014),
								new ElementToChannelConverter(value -> {
									// Convert milliampere to duty cycle percentage * 10
									// Formula: duty_cycle = (current_mA / 600) [for 6A = 10%, 60A = 100%]
									// Simplified: duty_cycle_x10 = (current_mA * 10) / 600 = current_mA / 60
									Integer currentMa = TypeUtils.getAsType(INTEGER, value);
									if (currentMa == null || currentMa == 0) {
										return 0; // 0 = stop charging
									}

									// Convert mA to duty cycle % * 10
									// 6000 mA = 6A should map to ~10% duty cycle (100 in register, since
									// %*10)
									// 32000 mA = 32A should map to ~53% duty cycle (530 in register)
									// Linear mapping: duty_cycle_pct_x10 = (currentMa - 6000) * 900 / 54000 +
									// 100
									// Simplified approximation for 6-32A range
									int dutyCycleX10 = Math.min(1000, Math.max(80, (currentMa * 167) / 10000));

									return dutyCycleX10;
								})))

		);

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("State:").append(this.getChargingState()).append("|") //
				.append("L:").append(this.getActivePower().asString());

		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointAbl.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString());
		}
		return b.toString();
	}

	private void logIfDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	// Helper methods to set channel values from Modbus converters
	private void _setChargingState(ChargingState state) {
		this.channel(EvseChargePointAbl.ChannelId.CHARGING_STATE).setNextValue(state);
	}

	private void _setPhaseCurrentL1(Integer current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).setNextValue(current);
	}

	private void _setPhaseCurrentL2(Integer current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).setNextValue(current);
	}

	private void _setPhaseCurrentL3(Integer current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).setNextValue(current);
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		var config = this.config;
		if (config == null || config.readOnly()) {
			return null;
		}

		final var phases = this.getWiring();
		final var maxCurrentMa = config.maxCurrent() * 1000; // Convert A to mA

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, 6000, maxCurrentMa)) //
				.setIsEvConnected(this.getEvConnectedChannel().value().orElse(0) != 0) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	private SingleOrThreePhase getWiring() {
		return this.config.wiring();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			if (this.config.simulationMode() && this.simulator != null) {
				this.handleSimulationMode();
			} else {
				this.handleNormalMode();
			}

			if (this.config.debugMode()) {
				this.updateRawRegisterChannels();
			}
		}
		}
	}

	private void handleSimulationMode() {
		// Process simulation control channels
		var plugInChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAbl.ChannelId.SIMULATE_PLUG_IN);
		var plugIn = plugInChannel.getNextWriteValueAndReset();
		if (plugIn.isPresent() && plugIn.get()) {
			this.simulator.plugIn();
		}

		var unplugChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAbl.ChannelId.SIMULATE_UNPLUG);
		var unplug = unplugChannel.getNextWriteValueAndReset();
		if (unplug.isPresent() && unplug.get()) {
			this.simulator.unplug();
		}

		var errorChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAbl.ChannelId.SIMULATE_ERROR);
		var error = errorChannel.getNextWriteValueAndReset();
		if (error.isPresent() && error.get()) {
			this.simulator.setError();
		}

		var clearErrorChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAbl.ChannelId.SIMULATE_CLEAR_ERROR);
		var clearError = clearErrorChannel.getNextWriteValueAndReset();
		if (clearError.isPresent() && clearError.get()) {
			this.simulator.clearError();
		}

		// Tick the simulator
		var now = System.currentTimeMillis();
		this.simulator.tick(now - this.lastTickTime);
		this.lastTickTime = now;

		// Update channels from simulator
		var state = this.simulator.getState();
		var chargingState = convertSimulatorState(state);
		setValue(this, EvseChargePointAbl.ChannelId.CHARGING_STATE, chargingState);
		setValue(this, EvseChargePointAbl.ChannelId.EV_CONNECTED,
				state == ChargePointState.B || state == ChargePointState.C || state == ChargePointState.D);
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, evaluateIsReadyForCharging(chargingState));

		// Update electrical values
		int currentA = (int) this.simulator.getCurrentAmps();
		int phases = this.simulator.getPhases();

		// Set ABL phase currents (in A)
		setValue(this, EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1, currentA);
		setValue(this, EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2, phases == 3 ? currentA : 0);
		setValue(this, EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3, phases == 3 ? currentA : 0);

		// Set ElectricityMeter channels (in mV and mA)
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, 230000);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, 230000);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, 230000);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, currentA * 1000);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, phases == 3 ? currentA * 1000 : 0);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, phases == 3 ? currentA * 1000 : 0);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, (int) this.simulator.getPowerWatts());

		// Update energy calculations
		this.calculateEnergyL1.update((int) this.simulator.getPowerL1Watts());
		this.calculateEnergyL2.update((int) this.simulator.getPowerL2Watts());
		this.calculateEnergyL3.update((int) this.simulator.getPowerL3Watts());
	}

	private void handleNormalMode() {
		// Calculate energy from power
		this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
		this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
		this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());

		// Update IS_READY_FOR_CHARGING based on charging state
		var chargingState = this.getChargingState();
		var isReady = evaluateIsReadyForCharging(chargingState);
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReady);

		// Map phase currents from ABL channels to ElectricityMeter channels
		// Convert A to mA (*1000) for meter channels
		Integer currentL1 = (Integer) this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).value().get();
		Integer currentL2 = (Integer) this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).value().get();
		Integer currentL3 = (Integer) this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).value().get();

		setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, currentL1 != null ? currentL1 * 1000 : null);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, currentL2 != null ? currentL2 * 1000 : null);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, currentL3 != null ? currentL3 * 1000 : null);

		// Set voltage to nominal 230V per phase (ABL doesn't provide voltage measurement)
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, 230000);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, 230000);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, 230000);
	}

	private void updateRawRegisterChannels() {
		// Get current values and format as hex
		var currentL1 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).value().get();
		var currentL2 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).value().get();
		var currentL3 = this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).value().get();
		var state = this.getChargingState();
		var evConnected = this.channel(EvseChargePointAbl.ChannelId.EV_CONNECTED).value().get();

		// Format values as hex strings
		setValue(this, EvseChargePointAbl.ChannelId.RAW_CURRENT_L1,
				currentL1 != null ? String.format("%02X", ((Integer) currentL1) & 0xFF) : "N/A");
		setValue(this, EvseChargePointAbl.ChannelId.RAW_CURRENT_L2,
				currentL2 != null ? String.format("%02X", ((Integer) currentL2) & 0xFF) : "N/A");
		setValue(this, EvseChargePointAbl.ChannelId.RAW_CURRENT_L3,
				currentL3 != null ? String.format("%02X", ((Integer) currentL3) & 0xFF) : "N/A");
		setValue(this, EvseChargePointAbl.ChannelId.RAW_STATE,
				state != null ? String.format("%02X", state.getValue()) : "N/A");
		setValue(this, EvseChargePointAbl.ChannelId.RAW_EV_CONNECTED,
				evConnected != null ? (Boolean.TRUE.equals(evConnected) ? "01" : "00") : "N/A");
	}

	private static ChargingState convertSimulatorState(ChargePointState state) {
		return switch (state) {
		case A -> ChargingState.A1;
		case B -> ChargingState.B1;
		case C -> ChargingState.C2;
		case D -> ChargingState.F7; // State D requested by EV (ABL treats as error)
		case E -> ChargingState.E0;
		case F -> ChargingState.E0; // Not available -> Outlet disabled
		};
	}

	/**
	 * Evaluates if the charging station is ready for charging based on state.
	 *
	 * @param chargingState the current charging state
	 * @return true if ready for charging
	 */
	protected static boolean evaluateIsReadyForCharging(ChargingState chargingState) {
		if (chargingState == null) {
			return false;
		}

		return switch (chargingState.status) {
		case READY_FOR_CHARGING, CHARGING -> true;
		default -> false;
		};
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}

		final var now = Instant.now();
		final var current = actions.getApplySetPointInMilliAmpere().value();

		this.handleApplyCharge(now, current);
	}

	/**
	 * Applies the charging current setpoint with rate limiting.
	 *
	 * @param now     current timestamp
	 * @param current desired current in milliampere
	 */
	private void handleApplyCharge(Instant now, int current) {
		// Rate limit: minimum 5 seconds between changes (as per best practice from
		// other implementations)
		if (this.previousCurrent != null && Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			this.logIfDebug("Rate limit active, skipping current update");
			return;
		}

		this.previousCurrent = Tuple.of(now, current);

		if (this.config.simulationMode() && this.simulator != null) {
			this.logIfDebug("Setting simulated charging current to " + current + " mA");
			this.simulator.setCurrentLimit(current);
		} else {
			try {
				this.logIfDebug("Setting charging current to " + current + " mA");
				this.setChargingCurrent(current);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Failed to set charging current: " + e.getMessage());
			}
		}
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		}
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}
}
