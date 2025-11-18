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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatingPointDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatingPointQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.alfen.enums.ChargingState;
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
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.applyConfig(config);
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
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatingPointDoublewordElement(306)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatingPointDoublewordElement(308)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatingPointDoublewordElement(310))),

				// Current measurements (register 320-325, 3x Float32)
				// Alfen reports current in Ampere, we convert to mA
				new FC3ReadRegistersTask(320, Priority.HIGH,
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatingPointDoublewordElement(320),
								SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatingPointDoublewordElement(322),
								SCALE_FACTOR_MINUS_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatingPointDoublewordElement(324),
								SCALE_FACTOR_MINUS_3)),

				// Power measurement (register 344-347, Float64)
				new FC3ReadRegistersTask(344, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatingPointQuadruplewordElement(344))),

				// Total energy (register 374-377, Float64)
				// Alfen reports in Wh, convert to Wh (divide by 1000 according to evcc)
				new FC3ReadRegistersTask(374, Priority.LOW,
						m(EvseChargePointAlfen.ChannelId.TOTAL_ENERGY, new FloatingPointQuadruplewordElement(374),
								v -> v == null ? null : Math.round((Double) v))),

				// Charging state (register 1201, String 5 registers)
				new FC3ReadRegistersTask(1201, Priority.HIGH,
						m(EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE, new StringWordElement(1201, 5))),

				// Max current setting (register 1210, Float32 in mA)
				new FC3ReadRegistersTask(1210, Priority.LOW,
						m(EvseChargePointAlfen.ChannelId.SET_CHARGING_CURRENT,
								new FloatingPointDoublewordElement(1210))),
				new FC6WriteRegisterTask(1210,
						m(EvseChargePointAlfen.ChannelId.SET_CHARGING_CURRENT,
								new FloatingPointDoublewordElement(1210))),

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
			this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());

			// Parse charging state from raw string
			var rawState = this.channel(EvseChargePointAlfen.ChannelId.RAW_CHARGING_STATE).value().asOptional();
			var chargingState = rawState.map(ChargingState::fromString).orElse(ChargingState.UNDEFINED);
			setValue(this, EvseChargePointAlfen.ChannelId.CHARGING_STATE, chargingState);

			// Evaluate is ready for charging based on charging state
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, evaluateIsReadyForCharging(chargingState));
		}
		}
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
			this.setChargingCurrent(current);
			this.logIfDebug("Setting charging current to " + current + " mA");
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
