package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.alfen.enums.ChargingState;
import io.openems.edge.evse.simulator.core.ChargePointSimulator;

public class EvseChargePointAlfenImplTest {

	/**
	 * Basic test that the component activates without errors.
	 */
	@Test
	public void test() throws Exception {
		new ComponentTest(new EvseChargePointAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(false) //
						.setPhaseRotation(L1_L2_L3) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	/**
	 * Test with simulated Modbus register values.
	 *
	 * <p>
	 * This demonstrates how to use DummyModbusBridge to simulate actual wallbox
	 * responses.
	 */
	@Test
	public void testWithSimulatedData() throws Exception {
		// Create DummyModbusBridge with pre-populated registers
		var modbus = new DummyModbusBridge("modbus0")
				// Voltages at register 306-311 (3x Float32)
				// Float32 for 230.0V = 0x4366, 0x0000
				.withRegisters(306, //
						0x4366, 0x0000, // L1: 230.0V
						0x4366, 0x0000, // L2: 230.0V
						0x4366, 0x0000) // L3: 230.0V

				// Currents at register 320-325 (3x Float32)
				// Float32 for 16.0A = 0x4180, 0x0000
				.withRegisters(320, //
						0x4180, 0x0000, // L1: 16.0A
						0x4180, 0x0000, // L2: 16.0A
						0x4180, 0x0000) // L3: 16.0A

				// Power at register 344-347 (Float64)
				// Float64 for 11040.0W (16A * 230V * 3)
				.withRegisters(344, //
						0x40C5, 0x9200, 0x0000, 0x0000) // ~11040W

				// Total energy at register 374-377 (Float64)
				// Float64 for 1234.5 Wh
				.withRegisters(374, //
						0x4093, 0x4A00, 0x0000, 0x0000) // ~1234.5 Wh

				// Charging state at register 1201-1205 (String, 5 registers)
				// "C2" = Charging state C, sub-state 2
				.withRegisters(1201, //
						0x4332, 0x0000, 0x0000, 0x0000, 0x0000) // "C2"

				// Max current at register 1210-1211 (Float32 in mA)
				// Float32 for 16000.0 mA
				.withRegisters(1210, //
						0x467A, 0x0000) // 16000 mA

				// Phase configuration at register 1215 (Uint16)
				.withRegister(1215, 3); // 3-phase

		new ComponentTest(new EvseChargePointAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", modbus) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(false) //
						.setPhaseRotation(L1_L2_L3) //
						.build()) //
				// First cycle - data is read from Modbus
				.next(new TestCase("First cycle")) //
				// Second cycle - verify channel values
				.next(new TestCase("Verify values") //
						.output(EvseChargePointAlfen.ChannelId.PHASE_CONFIGURATION, 3)) //
				.deactivate();
	}

	/**
	 * Test read-only mode.
	 */
	@Test
	public void testReadOnly() throws Exception {
		new ComponentTest(new EvseChargePointAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(true) //
						.setPhaseRotation(L1_L2_L3) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	/**
	 * Test using ChargePointSimulator for a complete charging cycle.
	 *
	 * <p>
	 * This demonstrates the advantage of using the simulator over manual hex
	 * values: readable code, automatic value calculation, and realistic behavior.
	 */
	@Test
	public void testWithChargePointSimulator() throws Exception {
		// Create simulator - much cleaner than manual hex values!
		var simulator = ChargePointSimulator.alfen("modbus0");

		var test = new ComponentTest(new EvseChargePointAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", simulator.getModbus()) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(false) //
						.setPhaseRotation(L1_L2_L3) //
						.build());

		// Initial state: no car connected
		test.next(new TestCase("Initial - no car") //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		// Car plugs in
		simulator.plugIn();
		test.next(new TestCase("Car plugged in") //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //
				.output(EvseChargePointAlfen.ChannelId.PHASE_CONFIGURATION, 3));

		// Set current limit - charging starts automatically
		simulator.setCurrentLimit(16000); // 16A
		test.next(new TestCase("Charging at 16A") //
				.output(EvseChargePointAlfen.ChannelId.PHASE_CONFIGURATION, 3));

		// Simulate 10 minutes of charging
		simulator.tick(10 * 60 * 1000); // 10 minutes in ms
		test.next(new TestCase("After 10 minutes"));

		// Reduce current
		simulator.setCurrentLimit(8000); // 8A
		test.next(new TestCase("Reduced to 8A"));

		// Stop charging (set current to 0)
		simulator.setCurrentLimit(0);
		test.next(new TestCase("Charging paused"));

		// Car unplugs
		simulator.unplug();
		test.next(new TestCase("Car unplugged") //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		test.deactivate();
	}

	/**
	 * Test error handling with simulator.
	 */
	@Test
	public void testSimulatorErrorState() throws Exception {
		var simulator = ChargePointSimulator.alfen("modbus0");

		var test = new ComponentTest(new EvseChargePointAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", simulator.getModbus()) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setDebugMode(false) //
						.setReadOnly(false) //
						.setPhaseRotation(L1_L2_L3) //
						.build());

		// Normal operation
		simulator.plugIn();
		test.next(new TestCase("Normal operation"));

		// Error occurs
		simulator.setError();
		test.next(new TestCase("Error state") //
				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false));

		// Error cleared
		simulator.clearError();
		test.next(new TestCase("Error cleared"));

		test.deactivate();
	}

	/**
	 * Test charging state evaluation logic.
	 */
	@Test
	public void testEvaluateIsReadyForCharging() {
		// State A - Not connected
		assert !EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.A);

		// State B - Connected, ready
		assert EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.B);

		// State C - Charging
		assert EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.C);

		// State D - Charging with ventilation
		assert EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.D);

		// State E - Error
		assert !EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.E);

		// State F - Not available
		assert !EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.F);

		// Undefined
		assert !EvseChargePointAlfenImpl.evaluateIsReadyForCharging(ChargingState.UNDEFINED);
	}

	/**
	 * Test charging state string parsing.
	 */
	@Test
	public void testChargingStateFromString() {
		assert ChargingState.fromString("A") == ChargingState.A;
		assert ChargingState.fromString("B1") == ChargingState.B;
		assert ChargingState.fromString("B2") == ChargingState.B;
		assert ChargingState.fromString("C1") == ChargingState.C;
		assert ChargingState.fromString("C2") == ChargingState.C;
		assert ChargingState.fromString("D") == ChargingState.D;
		assert ChargingState.fromString("E") == ChargingState.E;
		assert ChargingState.fromString("F") == ChargingState.F;
		assert ChargingState.fromString("") == ChargingState.UNDEFINED;
		assert ChargingState.fromString(null) == ChargingState.UNDEFINED;
		assert ChargingState.fromString("X") == ChargingState.UNDEFINED;
	}
}
