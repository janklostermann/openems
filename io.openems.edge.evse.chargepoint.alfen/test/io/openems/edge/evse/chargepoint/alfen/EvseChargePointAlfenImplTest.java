package io.openems.edge.evse.chargepoint.alfen;

import static io.openems.edge.meter.api.PhaseRotation.L1_L2_L3;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.evse.chargepoint.alfen.enums.ChargingState;

public class EvseChargePointAlfenImplTest {

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
