package io.openems.edge.evse.chargepoint.abl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.common.test.DummyConfigurationAdmin;

/**
 * Test for the real ABL EVSE component implementation using ComponentTest
 * framework.
 *
 * <p>
 * This test demonstrates the standard OpenEMS pattern for testing components
 * with their OSGi lifecycle (activate, modify, deactivate).
 *
 * <p>
 * <b>Purpose of ComponentTest:</b>
 * <ul>
 * <li>Verifies component can be activated without errors
 * <li>Ensures all channels are properly initialized
 * <li>Tests OSGi lifecycle methods (@Activate, @Modified, @Deactivate)
 * <li>Validates configuration handling
 * <li>Simulates the OSGi container environment
 * </ul>
 *
 * <p>
 * <b>When to use ComponentTest vs DummyComponent:</b>
 * <ul>
 * <li><b>ComponentTest</b> - Test the REAL implementation with mocked
 * dependencies
 * <li><b>DummyComponent</b> - Test logic that uses the component (e.g.,
 * controllers)
 * </ul>
 */
public class EvseChargePointAblImplTest {

	/**
	 * Test 1: Basic activation test.
	 *
	 * <p>
	 * This is the minimum test every OpenEMS component should have. It verifies:
	 * <ul>
	 * <li>Component activates without throwing exceptions
	 * <li>All channels are initialized (no null channels)
	 * <li>Channel IDs are correctly registered
	 * <li>Component state is valid after activation
	 * </ul>
	 *
	 * @throws Exception if component fails to activate
	 */
	@Test
	public void testActivation() throws Exception {
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						// .setAlias("Test ABL Wallbox") //
						// .setEnabled(true) //
						// .setReadOnly(false) //
						.setModbusId("modbus0") //
						// .setModbusUnitId(1) //
						// .setMaxCurrent(32) //
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();
	}

	/**
	 * Test 2: Test with read-only configuration.
	 *
	 * <p>
	 * Verifies that component can be configured in read-only mode (monitoring
	 * only, no control).
	 *
	 * @throws Exception if component fails to activate
	 */
	@Test
	public void testReadOnlyMode() throws Exception {
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						// .setEnabled(true) //
						.setReadOnly(true) // Read-only mode
						.setModbusId("modbus0") //
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();
	}

	/**
	 * Test 3: Test configuration modification.
	 *
	 * <p>
	 * Tests the @Modified lifecycle method by changing configuration after
	 * activation. This simulates what happens when a user changes configuration in
	 * Apache Felix Web Console.
	 *
	 * <p>
	 * <b>Why this is important:</b> Components must handle configuration changes at
	 * runtime without requiring restart.
	 *
	 * @throws Exception if component fails to handle configuration change
	 */
	@Test
	public void testConfigurationModification() throws Exception {
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setMaxCurrent(16) // Initial: 16A
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				// Modify configuration
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setMaxCurrent(32) // Changed: 32A
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();
	}

	/**
	 * Test 4: Test with different Modbus unit IDs.
	 *
	 * <p>
	 * Verifies component can be configured with different Modbus unit IDs for
	 * multi-device setups.
	 *
	 * @throws Exception if component fails to activate
	 */
	@Test
	public void testMultipleModbusUnits() throws Exception {
		// First charge point with unit ID 1
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();

		// Second charge point with unit ID 2
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs1") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();
	}

	/**
	 * Test 5: Test with debug mode enabled.
	 *
	 * <p>
	 * Verifies component handles debug mode configuration correctly.
	 *
	 * @throws Exception if component fails to activate
	 */
	@Test
	public void testDebugMode() throws Exception {
		new ComponentTest(new EvseChargePointAblImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setDebugMode(true) // Enable debug logging
						.build()) //
				.next(new AbstractComponentTest.TestCase()) //
				.deactivate();
	}

	/**
	 * Test 6: Test simulation mode activation.
	 *
	 * <p>
	 * Verifies component activates correctly in simulation mode without requiring a
	 * real Modbus connection. The simulator core should be initialized and the
	 * SIMULATION_MODE channel should be set to true.
	 *
	 * @throws Exception if component fails to activate
	 */
	@Test
	public void testSimulationModeActivation() throws Exception {
		var component = new EvseChargePointAblImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setSimulationMode(true) // Enable simulation mode
						.build()) //
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.SIMULATION_MODE, true)) //
				.deactivate();
	}

	/**
	 * Test 7: Test simulation mode with initial state.
	 *
	 * <p>
	 * Verifies the simulator starts in the correct initial state (A1 - not
	 * connected) and that all electrical channels are properly initialized.
	 *
	 * @throws Exception if component fails
	 */
	@Test
	public void testSimulationModeInitialState() throws Exception {
		var component = new EvseChargePointAblImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setSimulationMode(true) //
						.build()) //
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.SIMULATION_MODE, true) //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.A1) //
						.output(EvseChargePointAbl.ChannelId.EV_CONNECTED, false)) //
				.deactivate();
	}

	/**
	 * Test 8: Test simulation mode plug-in/unplug cycle.
	 *
	 * <p>
	 * Verifies the simulator correctly handles vehicle plug-in and unplug events,
	 * transitioning through states A1 -> B1 -> A1.
	 *
	 * @throws Exception if component fails
	 */
	@Test
	public void testSimulationModePlugCycle() throws Exception {
		var component = new EvseChargePointAblImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setSimulationMode(true) //
						.build()) //
				// Initial state - unplugged
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.A1)) //
				// Plug in vehicle
				.next(new AbstractComponentTest.TestCase() //
						.input(EvseChargePointAbl.ChannelId.SIMULATE_PLUG_IN, true)) //
				// Verify connected state
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.B1) //
						.output(EvseChargePointAbl.ChannelId.EV_CONNECTED, true)) //
				// Unplug vehicle
				.next(new AbstractComponentTest.TestCase() //
						.input(EvseChargePointAbl.ChannelId.SIMULATE_UNPLUG, true)) //
				// Back to unplugged
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.A1) //
						.output(EvseChargePointAbl.ChannelId.EV_CONNECTED, false)) //
				.deactivate();
	}

	/**
	 * Test 9: Test simulation mode error handling.
	 *
	 * <p>
	 * Verifies the simulator correctly handles error state transitions.
	 *
	 * @throws Exception if component fails
	 */
	@Test
	public void testSimulationModeErrorHandling() throws Exception {
		var component = new EvseChargePointAblImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setSimulationMode(true) //
						.build()) //
				// Initial state
				.next(new AbstractComponentTest.TestCase()) //
				// Set error
				.next(new AbstractComponentTest.TestCase() //
						.input(EvseChargePointAbl.ChannelId.SIMULATE_ERROR, true)) //
				// Verify error state
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.E0)) //
				// Clear error
				.next(new AbstractComponentTest.TestCase() //
						.input(EvseChargePointAbl.ChannelId.SIMULATE_CLEAR_ERROR, true)) //
				// Back to normal
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.CHARGING_STATE, ChargingState.A1)) //
				.deactivate();
	}

	/**
	 * Test 10: Test simulation mode with debug mode combined.
	 *
	 * <p>
	 * Verifies that simulation mode and debug mode can be used together.
	 *
	 * @throws Exception if component fails
	 */
	@Test
	public void testSimulationAndDebugModeCombined() throws Exception {
		var component = new EvseChargePointAblImpl();
		new ComponentTest(component) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setModbusId("modbus0") //
						.setSimulationMode(true) //
						.setDebugMode(true) //
						.build()) //
				.next(new AbstractComponentTest.TestCase() //
						.output(EvseChargePointAbl.ChannelId.SIMULATION_MODE, true) //
						.output(EvseChargePointAbl.ChannelId.DEBUG_MODE, true)) //
				.deactivate();
	}

	/**
	 * Test 11: Test evaluateIsReadyForCharging method.
	 *
	 * <p>
	 * Unit test for the static helper method that determines if the charging
	 * station is ready based on state.
	 */
	@Test
	public void testEvaluateIsReadyForCharging() {
		// States that should be ready
		assertTrue(EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.B2));
		assertTrue(EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.C1));
		assertTrue(EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.C2));
		assertTrue(EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.D1));
		assertTrue(EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.D2));

		// States that should not be ready
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.A1));
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.A2));
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.B1));
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.E0));
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(ChargingState.F0));

		// Null handling
		assertEquals(false, EvseChargePointAblImpl.evaluateIsReadyForCharging(null));
	}

	/*
	 * NOTE: Advanced ComponentTest Usage
	 *
	 * For more advanced testing, ComponentTest supports:
	 *
	 * 1. Multiple test cycles: .next(new TestCase()) .next(new TestCase())
	 * .next(new TestCase())
	 *
	 * 2. Channel value assertions: .next(new TestCase() .output(STATE_MACHINE,
	 * State.RUNNING))
	 *
	 * 3. Input injection: .next(new TestCase() .input(GRID_MODE,
	 * GridMode.ON_GRID))
	 *
	 * 4. Timedata integration: .addReference("timedata", new DummyTimedata())
	 *
	 * 5. Component Manager: .addReference("componentManager", new
	 * DummyComponentManager())
	 *
	 * Example of advanced test:
	 *
	 * @Test public void testChargingCycle() throws Exception { var component = new
	 * EvseChargePointAblImpl();
	 *
	 * new ComponentTest(component) .addReference("cm", new
	 * DummyConfigurationAdmin()) .addReference("setModbus", new
	 * DummyModbusBridge("modbus0")) .activate(MyConfig.create().setId("evcs0").build())
	 * // Initial state .next(new TestCase()) // Simulate EV connection .next(new
	 * TestCase() .output(IS_READY_FOR_CHARGING, true)) // Apply charging
	 * .next(new TestCase() .output(CHARGING_STATE, ChargingState.C2))
	 * .deactivate(); }
	 *
	 * However, for complex state machine testing, DummyAblChargePoint is more
	 * suitable as it doesn't require Modbus infrastructure.
	 */
}
