package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.alfen.enums.ChargingState;
import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Alfen", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointAlfenImpl extends AbstractOpenemsModbusComponent
		implements EvseChargePointAlfen, ModbusComponent, OpenemsComponent, TimedataProvider, EvseChargePoint,
		EventHandler, ElectricityMeter {

	private final Logger log = LoggerFactory.getLogger(EvseChargePointAlfenImpl.class);
	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private Config config;
	private ChargePointSimulatorCore simulator;
	private long lastTickTime;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseChargePointAlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointAlfen.ChannelId.values() //
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
		this.simulator.setPhases(3);
		this.lastTickTime = System.currentTimeMillis();
		setValue(this, EvseChargePointAlfen.ChannelId.SIMULATION_MODE, true);
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
		setValue(this, EvseChargePointAlfen.ChannelId.DEBUG_MODE, config.debugMode());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				// Voltage measurements (register 306-311, 3x Float32)
				new FC3ReadRegistersTask(306, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(306)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(308)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(310))),

				// Current measurements (register 320-325, 3x Float32)
				// Alfen reports current in Ampere, we convert to mA
				new FC3ReadRegistersTask(320, Priority.HIGH,
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(320),
								SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(322),
								SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(324),
								SCALE_FACTOR_MINUS_3)),

				// Power measurement (register 344-347, Float64)
				new FC3ReadRegistersTask(344, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatQuadruplewordElement(344))),

				// Total energy (register 374-377, Float64)
				// Alfen reports in Wh, convert to Wh (divide by 1000 according to evcc)
				new FC3ReadRegistersTask(374, Priority.LOW,
						m(EvseChargePointAlfen.ChannelId.TOTAL_ENERGY, new FloatQuadruplewordElement(374),
								new ElementToChannelConverter(v -> v == null ? null : Math.round((Double) v)))),

				// Charging state (register 1201, String 5 registers)
				new FC3ReadRegistersTask(1201, Priority.HIGH,
						m(EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE, new StringWordElement(1201, 5))),

				// Max current setting (register 1210, Float32 in mA)
				new FC3ReadRegistersTask(1210, Priority.LOW,
						m(EvseChargePointAlfen.ChannelId.SET_CHARGING_CURRENT,
								new FloatDoublewordElement(1210))),
				new FC16WriteRegistersTask(1210,
						m(EvseChargePointAlfen.ChannelId.SET_CHARGING_CURRENT,
								new FloatDoublewordElement(1210))),

				// Phase configuration (register 1215, Uint16)
				new FC3ReadRegistersTask(1215, Priority.LOW,
						m(EvseChargePointAlfen.ChannelId.PHASE_CONFIGURATION, new UnsignedWordElement(1215))),
				new FC6WriteRegisterTask(1215,
						m(EvseChargePointAlfen.ChannelId.PHASE_CONFIGURATION, new UnsignedWordElement(1215)))

		);
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder() //
				.append("L:").append(this.getActivePower().asString());
		if (!this.config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(this.channel(EvseChargePointAlfen.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString()) //
					.append("|Phases:") //
					.append(this.getPhaseConfiguration().asString());
		}
		return b.toString();
	}

	private void logIfDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
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
				.channel(EvseChargePointAlfen.ChannelId.SIMULATE_PLUG_IN);
		var plugIn = plugInChannel.getNextWriteValueAndReset();
		if (plugIn.isPresent() && plugIn.get()) {
			this.simulator.plugIn();
		}

		var unplugChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAlfen.ChannelId.SIMULATE_UNPLUG);
		var unplug = unplugChannel.getNextWriteValueAndReset();
		if (unplug.isPresent() && unplug.get()) {
			this.simulator.unplug();
		}

		var errorChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAlfen.ChannelId.SIMULATE_ERROR);
		var error = errorChannel.getNextWriteValueAndReset();
		if (error.isPresent() && error.get()) {
			this.simulator.setError();
		}

		var clearErrorChannel = (BooleanWriteChannel) this
				.channel(EvseChargePointAlfen.ChannelId.SIMULATE_CLEAR_ERROR);
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
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE, state.name());
		setValue(this, EvseChargePointAlfen.ChannelId.CHARGING_STATE, chargingState);
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, evaluateIsReadyForCharging(chargingState));

		// Update electrical values
		int voltageL1 = (int) (this.simulator.getVoltageL1() * 1000); // mV
		int voltageL2 = (int) (this.simulator.getVoltageL2() * 1000);
		int voltageL3 = (int) (this.simulator.getVoltageL3() * 1000);
		int currentMa = (int) (this.simulator.getCurrentAmps() * 1000);

		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L1, voltageL1);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L2, voltageL2);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE_L3, voltageL3);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L1, currentMa);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L2, this.simulator.getPhases() == 3 ? currentMa : 0);
		setValue(this, ElectricityMeter.ChannelId.CURRENT_L3, this.simulator.getPhases() == 3 ? currentMa : 0);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, (int) this.simulator.getPowerWatts());
		setValue(this, EvseChargePointAlfen.ChannelId.TOTAL_ENERGY, (long) this.simulator.getTotalEnergyWh());

		// Update energy calculations
		this.calculateEnergyL1.update((int) this.simulator.getPowerL1Watts());
		this.calculateEnergyL2.update((int) this.simulator.getPowerL2Watts());
		this.calculateEnergyL3.update((int) this.simulator.getPowerL3Watts());
	}

	private void handleNormalMode() {
		this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
		this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
		this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());

		// Parse charging state from raw string
		var rawState = this.channel(EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE).value().get();
		var chargingState = rawState != null
				? ChargingState.fromString(rawState.toString())
				: ChargingState.UNDEFINED;
		setValue(this, EvseChargePointAlfen.ChannelId.CHARGING_STATE, chargingState);

		// Evaluate is ready for charging based on charging state
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, evaluateIsReadyForCharging(chargingState));
	}

	private void updateRawRegisterChannels() {
		// Get current values and format as hex
		var voltageL1 = this.getVoltageL1Channel().value().get();
		var voltageL2 = this.getVoltageL2Channel().value().get();
		var voltageL3 = this.getVoltageL3Channel().value().get();
		var currentL1 = this.getCurrentL1Channel().value().get();
		var currentL2 = this.getCurrentL2Channel().value().get();
		var currentL3 = this.getCurrentL3Channel().value().get();
		var power = this.getActivePowerChannel().value().get();
		var energy = this.channel(EvseChargePointAlfen.ChannelId.TOTAL_ENERGY).value().get();
		var state = this.channel(EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE).value().get();
		var maxCurrent = this.channel(EvseChargePointAlfen.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().get();

		// Format values as hex strings (simulating Float32/64 encoding)
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_VOLTAGE_L1,
				voltageL1 != null ? floatToHex(voltageL1 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_VOLTAGE_L2,
				voltageL2 != null ? floatToHex(voltageL2 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_VOLTAGE_L3,
				voltageL3 != null ? floatToHex(voltageL3 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_CURRENT_L1,
				currentL1 != null ? floatToHex(currentL1 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_CURRENT_L2,
				currentL2 != null ? floatToHex(currentL2 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_CURRENT_L3,
				currentL3 != null ? floatToHex(currentL3 / 1000f) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_POWER, power != null ? doubleToHex((Integer) power) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_ENERGY,
				energy != null ? doubleToHex(((Number) energy).doubleValue()) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_STATE, state != null ? stringToHex(state.toString()) : "N/A");
		setValue(this, EvseChargePointAlfen.ChannelId.RAW_MAX_CURRENT,
				maxCurrent != null ? floatToHex(((Number) maxCurrent).floatValue()) : "N/A");
	}

	private static String floatToHex(float value) {
		int bits = Float.floatToIntBits(value);
		return String.format("%08X", bits);
	}

	private static String doubleToHex(double value) {
		long bits = Double.doubleToLongBits(value);
		return String.format("%016X", bits);
	}

	private static String stringToHex(String value) {
		var sb = new StringBuilder();
		for (char c : value.toCharArray()) {
			sb.append(String.format("%04X", (int) c));
		}
		return sb.toString();
	}

	private static ChargingState convertSimulatorState(ChargePointState state) {
		return switch (state) {
		case A -> ChargingState.A;
		case B -> ChargingState.B;
		case C -> ChargingState.C;
		case D -> ChargingState.D;
		case E -> ChargingState.E;
		case F -> ChargingState.F;
		};
	}

	/**
	 * Evaluates if the charger is ready for charging based on the charging state.
	 *
	 * @param chargingState the current charging state
	 * @return true if ready for charging
	 */
	protected static boolean evaluateIsReadyForCharging(ChargingState chargingState) {
		return switch (chargingState) {
		case B, C, D -> true;
		case A, E, F, UNDEFINED -> false;
		};
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		var config = this.config;
		if (config == null || config.readOnly()) {
			return ChargePointAbilities.create().build();
		}

		final var isEvConnected = switch (this.getChargingState()) {
		case UNDEFINED, A, E, F -> false;
		case B, C, D -> true;
		};

		// Alfen wallboxes typically support 6A to 32A charging
		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, 32000)) //
				.setIsEvConnected(isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.config.readOnly()) {
			return;
		}

		var current = actions.getApplySetPointInMilliAmpere().value();
		this.handleApplyCharge(current);
	}

	private void handleApplyCharge(int current) {
		try {
			if (this.config.simulationMode() && this.simulator != null) {
				this.simulator.setCurrentLimit(current);
				this.logIfDebug("Setting simulated charging current to " + current + " mA");
			} else {
				this.setChargingCurrent(current);
				this.logIfDebug("Setting charging current to " + current + " mA");
			}
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to set charging current: " + e.getMessage());
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
}
