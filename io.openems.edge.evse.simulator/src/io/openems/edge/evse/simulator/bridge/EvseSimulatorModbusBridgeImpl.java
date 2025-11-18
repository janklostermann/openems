package io.openems.edge.evse.simulator.bridge;

import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractModbusListener;
import com.ghgande.j2mod.modbus.procimg.ProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.worker.internal.DefectiveComponents;
import io.openems.edge.bridge.modbus.api.worker.internal.TasksSupplier;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusRecordFloat32;
import io.openems.edge.common.modbusslave.ModbusRecordFloat64;
import io.openems.edge.evse.simulator.core.ChargePointSimulatorCore;
import io.openems.edge.evse.simulator.core.ChargePointState;
import io.openems.edge.evse.simulator.core.ModbusRegisterMapper;

/**
 * EVSE Simulator Modbus Bridge implementation.
 *
 * <p>
 * This component simulates an EVSE chargepoint over Modbus for testing and
 * demonstration. It behaves like a real Modbus bridge but internally simulates
 * chargepoint behavior using {@link ChargePointSimulatorCore}.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Evse.ModbusBridge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvseSimulatorModbusBridgeImpl extends AbstractModbusBridge
		implements EvseSimulatorModbusBridge, BridgeModbus, BridgeModbusTcp, OpenemsComponent, EventHandler {

	private final ModbusTransaction modbusTransaction = new ModbusTransaction() {
		@Override
		public void execute() {
			this.response = EvseSimulatorModbusBridgeImpl.this.executeModbusRequest(this.request);
		}
	};

	private final AbstractModbusListener modbusListener = new AbstractModbusListener() {
		@Override
		public ProcessImage getProcessImage(int unitId) {
			return EvseSimulatorModbusBridgeImpl.this.processImage;
		}

		@Override
		public void run() {
		}

		@Override
		public void stop() {
		}
	};

	private final TasksSupplier tasksSupplier;
	private final DefectiveComponents defectiveComponents;

	private SimpleProcessImage processImage;
	private ChargePointSimulatorCore core;
	private ModbusRegisterMapper mapper;
	private DeviceType deviceType;
	private InetAddress ipAddress;
	private long lastCycleTime;

	public EvseSimulatorModbusBridgeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values(), //
				EvseSimulatorModbusBridge.ChannelId.values() //
		);
		this.tasksSupplier = getValueViaReflection(this.worker, "tasksSupplier");
		this.defectiveComponents = getValueViaReflection(this.worker, "defectiveComponents");
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, new io.openems.edge.bridge.modbus.api.Config(//
				config.id(), config.alias(), config.enabled(), config.logVerbosity(), 2));

		this.deviceType = config.deviceType();
		this.mapper = this.deviceType.createMapper();
		this.processImage = new SimpleProcessImage();

		// Initialize the core simulator
		this.core = new ChargePointSimulatorCore();
		this.core.setVoltages(config.initialVoltageL1(), config.initialVoltageL2(), config.initialVoltageL3());
		this.core.setPhases(config.phases());

		// Set virtual IP address for identification
		this.ipAddress = InetAddress.getByName("127.0.0.1");
		this.lastCycleTime = System.currentTimeMillis();

		// Update channels and registers
		this.updateChannels();
		this.updateRegisters();

		this._setSimulatorRunning(true);
		this._setDeviceType(this.deviceType);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws UnknownHostException {
		super.modified(context, new io.openems.edge.bridge.modbus.api.Config(//
				config.id(), config.alias(), config.enabled(), config.logVerbosity(), 2));

		// Update configuration
		if (this.deviceType != config.deviceType()) {
			this.deviceType = config.deviceType();
			this.mapper = this.deviceType.createMapper();
			this._setDeviceType(this.deviceType);
		}

		this.core.setVoltages(config.initialVoltageL1(), config.initialVoltageL2(), config.initialVoltageL3());
		this.core.setPhases(config.phases());

		this.updateChannels();
		this.updateRegisters();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this._setSimulatorRunning(false);
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.processImage == null) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.processControlChannels();
			this.simulateCycle();
			this.onBeforeProcessImage();
		}
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			// Handle write operations if needed
		}
		}
	}

	/**
	 * Process control channel write values.
	 */
	private void processControlChannels() {
		// Check SIMULATE_PLUG_IN
		var plugInOpt = this.getSimulatePlugInChannel().getNextWriteValueAndReset();
		if (plugInOpt.isPresent() && Boolean.TRUE.equals(plugInOpt.get())) {
			this.core.plugIn();
		}

		// Check SIMULATE_UNPLUG
		var unplugOpt = this.getSimulateUnplugChannel().getNextWriteValueAndReset();
		if (unplugOpt.isPresent() && Boolean.TRUE.equals(unplugOpt.get())) {
			this.core.unplug();
		}

		// Check SET_CURRENT_LIMIT
		var currentLimitOpt = this.getSetCurrentLimitChannel().getNextWriteValueAndReset();
		if (currentLimitOpt.isPresent()) {
			this.core.setCurrentLimit(currentLimitOpt.get());
		}

		// Check SIMULATE_ERROR
		var errorOpt = this.getSimulateErrorChannel().getNextWriteValueAndReset();
		if (errorOpt.isPresent() && Boolean.TRUE.equals(errorOpt.get())) {
			this.core.setError();
		}

		// Check CLEAR_ERROR
		var clearErrorOpt = this.getClearErrorChannel().getNextWriteValueAndReset();
		if (clearErrorOpt.isPresent() && Boolean.TRUE.equals(clearErrorOpt.get())) {
			this.core.clearError();
		}
	}

	/**
	 * Simulate one cycle - advance time and update state.
	 */
	private void simulateCycle() {
		var now = System.currentTimeMillis();
		var elapsed = now - this.lastCycleTime;
		this.lastCycleTime = now;

		// Advance simulation time
		this.core.tick(elapsed);

		// Update channels with current state
		this.updateChannels();

		// Update Modbus registers
		this.updateRegisters();
	}

	/**
	 * Update OpenEMS channels with current simulator state.
	 */
	private void updateChannels() {
		this._setSimulatedState(this.core.getState());
		this._setSimulatedPower((int) this.core.getPowerWatts());
		this._setSimulatedEnergySession((long) this.core.getSessionEnergyWh());
		this._setSimulatedEnergyTotal((long) this.core.getTotalEnergyWh());

		// Currents - only when charging
		var currentMa = this.core.getState() == ChargePointState.C ? this.core.getSetCurrentMa() : 0;
		this._setSimulatedCurrentL1(currentMa);
		this._setSimulatedCurrentL2(this.core.getPhases() >= 2 ? currentMa : 0);
		this._setSimulatedCurrentL3(this.core.getPhases() >= 3 ? currentMa : 0);

		// Voltages
		this._setSimulatedVoltageL1((int) (this.core.getVoltageL1() * 1000));
		this._setSimulatedVoltageL2((int) (this.core.getVoltageL2() * 1000));
		this._setSimulatedVoltageL3((int) (this.core.getVoltageL3() * 1000));

		this._setSimulatedPhases(this.core.getPhases());
		this._setSimulatedCurrentLimit(this.core.getSetCurrentMa());
	}

	/**
	 * Update Modbus registers using the device-specific mapper.
	 */
	private void updateRegisters() {
		// Clear and rebuild process image
		this.processImage = new SimpleProcessImage();

		// Use the mapper to set registers based on current core state
		this.mapper.updateRegisters(this.processImage, this.core);
	}

	private void onBeforeProcessImage() {
		var cycleTasks = this.tasksSupplier.getCycleTasks(this.defectiveComponents);
		for (var readTask : cycleTasks.reads()) {
			readTask.execute(this);
		}
	}

	private ModbusResponse executeModbusRequest(ModbusRequest request) {
		return request.createResponse(this.modbusListener);
	}

	@Override
	public InetAddress getIpAddress() {
		return this.ipAddress;
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		return this.modbusTransaction;
	}

	@Override
	public void closeModbusConnection() {
		// No real connection to close
	}
}
