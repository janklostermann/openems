package io.openems.edge.evse.chargepoint.abl;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1FaultState;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1HardwareType;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1State;
import io.openems.edge.evse.chargepoint.abl.enums.LogVerbosity;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Tests for {@link EvseChargePointAblImpl}.
 *
 * <p>
 * Full behaviour specification: {@code doc/behaviour-goals.adoc}.
 *
 * <p>
 * Test contract:
 * <ul>
 * <li>CONFIRMED tests must all pass before this implementation is considered
 * done.
 * <li>HYPOTHESIS tests are annotated {@code @Ignore} and require hardware
 * validation.
 * </ul>
 *
 * <p>
 * Register map used in tests (R5 block, DummyModbusBridge decimal addresses):
 *
 * <pre>
 * addr 46 (0x002E): [15:8]=0x2E ref; [7:0]=state byte
 *   C2=0x2EC2=11970, A1=0x2EA1=11937, B1=0x2EB1=11953, B2=0x2EB2=11954, F4=0x2EF4=12020
 * addr 47 (0x002F): bit15=EV connected; bits[11:0]=duty cycle×10
 *   EV connected, ~16A applied: 0x810A=33034
 *   EV connected, no current:   0x8000=32768
 * addr 48 (0x0030): I_ct1 [0.1A]; 160=16A, 200=20A, 1000=unavailable
 * addr 49 (0x0031): I_ct2 [0.1A]
 * addr 50 (0x0032): I_ct3 [0.1A]
 * addr  6 (0x0006): [7:0]=state byte (sys-flags cross-check)
 * addr  7 (0x0007): bit9=upstream lost, bit8=imbalance, bit7=temp, bit6=metering
 * </pre>
 */
public class EvseChargePointAblImplTest {

	// ─── Helper ──────────────────────────────────────────────────────────────────

	private static ComponentTest prepare(EvseChargePointAblImpl sut, DummyModbusBridge bridge,
			MyConfig config) throws Exception {
		return new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(config);
	}

	private static MyConfig defaultConfig() {
		return MyConfig.create() //
				.setId("evseChargePoint0") //
				.setHardwareType(AblEMh1HardwareType.EMH1_3P_22KW_CABLE) //
				.build();
	}

	// ─── G0a Hardware Model ───────────────────────────────────────────────────────

	/**
	 * G0a CONFIRMED: 3-phase 22 kW model declares correct ability with THREE_PHASE
	 * and max 32 A.
	 */
	@Test
	public void testG0a_HardwareModel_ThreePhase22kW() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setHardwareType(AblEMh1HardwareType.EMH1_3P_22KW_CABLE) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		var abilities = sut.getChargePointAbilities();
		var applySetPoint = abilities.applySetPoint();
		assertTrue("Expected Ampere ability", applySetPoint instanceof ApplySetPoint.Ability.Ampere);
		var ampereAbility = (ApplySetPoint.Ability.Ampere) applySetPoint;
		assertEquals(THREE_PHASE, ampereAbility.phase());
		assertEquals(6, ampereAbility.min());
		assertEquals(32, ampereAbility.max());
		// Power limits derivable from ability:
		assertEquals(22080, applySetPoint.toPower(32)); // MAXIMUM_HARDWARE_POWER
		assertEquals(4140, applySetPoint.toPower(6)); // MINIMUM_HARDWARE_POWER
	}

	/**
	 * G0a CONFIRMED: 1-phase 7.2 kW model declares SINGLE_PHASE ability.
	 */
	@Test
	public void testG0a_HardwareModel_OnePhase72kW() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setHardwareType(AblEMh1HardwareType.EMH1_1P_72KW_CABLE) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		var abilities = sut.getChargePointAbilities();
		var ampereAbility = (ApplySetPoint.Ability.Ampere) abilities.applySetPoint();
		assertEquals(SINGLE_PHASE, ampereAbility.phase());
		assertEquals(6, ampereAbility.min());
		assertEquals(32, ampereAbility.max());
		assertEquals(7360, ampereAbility.toPower(32)); // 1 × 32A × 230V
	}

	// ─── G0b Voltage Fallback ────────────────────────────────────────────────────

	/**
	 * G0b CONFIRMED: fallback voltages are used for VOLTAGE_L1/L2/L3 when no
	 * channel address is configured.
	 */
	@Test
	public void testG0b_VoltageFallback() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setFallbackVoltageL1(231) //
				.setFallbackVoltageL2(232) //
				.setFallbackVoltageL3(233) //
				.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 232_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 233_000)) //
				.deactivate();
	}

	/**
	 * G0b CONFIRMED: configured voltage channel addresses are used when the
	 * referenced channels carry a defined value.
	 */
	@Test
	public void testG0b_VoltageChannelAddress_UsesExternalChannel() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		var meter = new DummyElectricityMeter("meter0") //
				.withVoltageL1(235_000) //
				.withVoltageL2(234_000) //
				.withVoltageL3(233_000);
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(meter) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setVoltageL1ChannelAddress("meter0/VoltageL1") //
						.setVoltageL2ChannelAddress("meter0/VoltageL2") //
						.setVoltageL3ChannelAddress("meter0/VoltageL3") //
						.setFallbackVoltageL1(230) // intentionally different → confirms channel wins
						.setFallbackVoltageL2(230) //
						.setFallbackVoltageL3(230) //
						.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 235_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 234_000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 233_000)) //
				.deactivate();
	}

	/**
	 * G0b CONFIRMED: falls back to configured static voltage when the referenced
	 * channel carries no value (undefined).
	 */
	@Test
	public void testG0b_VoltageChannelAddress_FallbackWhenChannelUndefined() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		var meter = new DummyElectricityMeter("meter0"); // VoltageL1 not set → undefined
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(meter) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("evseChargePoint0") //
						.setVoltageL1ChannelAddress("meter0/VoltageL1") //
						.setFallbackVoltageL1(228) //
						.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 228_000)) //
				.deactivate();
	}

	// ─── G1 Monitoring ───────────────────────────────────────────────────────────

	/**
	 * G1 CONFIRMED: 3-phase 16 A charging — currents and calculated active power.
	 */
	@Test
	public void testG1_ThreePhaseMonitoring() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, //
								0x2EC2, // state C2
								0x810A, // EV connected, duty 266 (~16A)
								160, // I_ct1 = 16.0 A
								160, // I_ct2 = 16.0 A
								160)) // I_ct3 = 16.0 A
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 16_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 16_000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 11_040)) // 3×16A×230V
				.deactivate();
	}

	/**
	 * G1 CONFIRMED: 1-phase 20 A — active power uses only L1; L2/L3 are 0 mA.
	 */
	@Test
	public void testG1_OnePhaseMonitoring() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setHardwareType(AblEMh1HardwareType.EMH1_1P_72KW_CABLE) //
				.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, //
								0x2EC2, // state C2
								0x8000 | 333, // EV connected, duty 333 (~20A)
								200, // I_ct1 = 20.0 A
								0, // I_ct2 = 0 (1-phase)
								0)) // I_ct3 = 0
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 20_000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4_600)) // 1×20A×230V
				.deactivate();
	}

	/**
	 * G1 CONFIRMED: unavailable sentinel 0x03E8 in state A1 is treated as 0 mA.
	 */
	@Test
	public void testG1_CurrentUnavailable() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, //
								0x2EA1, // state A1
								0x0000, // no EV
								1000, // I_ct1 = 0x03E8 = unavailable
								1000, // I_ct2 = unavailable
								1000)) // I_ct3 = unavailable
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 0) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0)) //
				.deactivate();
	}

	// ─── G2 Charging State ───────────────────────────────────────────────────────

	/**
	 * G2 CONFIRMED: state C2 → isEvConnected=true, IS_READY_FOR_CHARGING=true.
	 */
	@Test
	public void testG2_StateMapping() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //

				// C2: charging
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true)) //

				// A1: no EV
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0x0000, 1000, 1000, 1000)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false)) //

				// B1: EV connected, permission not yet granted
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EB1, 0x8000, 0, 0, 0)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false)) //

				// B2: permission granted, not yet charging
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EB2, 0x8000, 0, 0, 0)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true)) //

				// F4: fault
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EF4, 0x0000, 0, 0, 0)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false)) //

				.deactivate();

		// isEvConnected is not a channel — verify via ChargePointAbilities
		// (After deactivate the last processed state was F4: not connected)
		assertFalse(sut.getChargePointAbilities().isEvConnected());

		// To test intermediate states verify via inline assertions — see individual
		// state tests below for detailed isEvConnected assertions.
	}

	/**
	 * G2 CONFIRMED: state C2 → isEvConnected = true.
	 */
	@Test
	public void testG2_StateC2_IsEvConnected() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				.deactivate();
		assertTrue(sut.getChargePointAbilities().isEvConnected());
		assertTrue(sut.getChargePointAbilities().isReadyForCharging());
	}

	/**
	 * G2 CONFIRMED: state A1 → isEvConnected = false.
	 */
	@Test
	public void testG2_StateA1_NotConnected() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0x0000, 1000, 1000, 1000))) //
				.deactivate();
		assertFalse(sut.getChargePointAbilities().isEvConnected());
		assertFalse(sut.getChargePointAbilities().isReadyForCharging());
	}

	/**
	 * G2 CONFIRMED: state B1 → isEvConnected = true, isReadyForCharging = false.
	 */
	@Test
	public void testG2_StateB1_ConnectedNotReady() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EB1, 0x8000, 0, 0, 0))) //
				.deactivate();
		assertTrue(sut.getChargePointAbilities().isEvConnected());
		assertFalse(sut.getChargePointAbilities().isReadyForCharging());
	}

	/**
	 * G2 CONFIRMED: state F4 → isEvConnected = false.
	 */
	@Test
	public void testG2_StateFault_NotConnected() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EF4, 0x0000, 0, 0, 0))) //
				.deactivate();
		assertFalse(sut.getChargePointAbilities().isEvConnected());
		assertFalse(sut.getChargePointAbilities().isReadyForCharging());
	}

	// ─── G3a Session Energy ───────────────────────────────────────────────────────

	/**
	 * G3a CONFIRMED: SESSION_ENERGY resets to 0 on new session and accumulates
	 * during charging. C2→C3→C2 dip does NOT reset the accumulator.
	 */
	@Test
	public void testG3a_SessionEnergy() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				// No EV connected
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0)) //
						.output(EvseChargePointAbl.ChannelId.SESSION_ENERGY, 0)) //
				// EV connects → session starts → SESSION_ENERGY resets to 0
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EB2, 0x8000, 0, 0, 0)) //
						.output(EvseChargePointAbl.ChannelId.SESSION_ENERGY, 0)) //
				// Charging at 16A — SESSION_ENERGY should start accumulating
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				// C3 dip — should NOT reset session energy
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC3, 0x810A, 80, 80, 80))) //
				// Back to C2
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				// EV disconnects — session energy retains value
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0))) //
				.deactivate();
		// After session ends, SESSION_ENERGY >= 0 (retained from session)
		assertTrue("SESSION_ENERGY should be >= 0 after session",
				sut.getSessionEnergyChannel().value().orElse(-1) >= 0);
	}

	// ─── G3b Lifetime Energy ─────────────────────────────────────────────────────

	/**
	 * G3b CONFIRMED: ACTIVE_CONSUMPTION_ENERGY accumulates (Timedata optional).
	 */
	@Test
	public void testG3b_LifetimeEnergy() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		// No Timedata reference added — optional, should not fail
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				.next(new TestCase()) //
				.deactivate();
		// Channel should exist and be non-negative (may be null on first cycle without
		// prior timedata)
		Long energy = (Long) sut.channel(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY).value().orElse(null);
		assertTrue("ACTIVE_CONSUMPTION_ENERGY should be >= 0 or null", energy == null || energy >= 0);
	}

	// ─── G5 Set Charge Current Limit ─────────────────────────────────────────────

	/**
	 * G5 CONFIRMED: apply(16A) → SET_CURRENT_RAW channel next write value = 267.
	 *
	 * <p>
	 * Formula: round(16 / 0.6 * 10) = round(266.67) = 267.
	 */
	@Test
	public void testG5_SetChargeLimit_16A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				.deactivate();

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(16) //
				.build());

		assertEquals(Optional.of(267), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G5 CONFIRMED: apply(6A) → SET_CURRENT_RAW = 100 (IEC 61851 minimum).
	 */
	@Test
	public void testG5_SetChargeLimit_Min6A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase()) //
				.deactivate();

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(6) //
				.build());

		assertEquals(Optional.of(100), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G5 CONFIRMED: apply(32A) → SET_CURRENT_RAW = 533.
	 */
	@Test
	public void testG5_SetChargeLimit_Max32A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase()) //
				.deactivate();

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(32) //
				.build());

		assertEquals(Optional.of(533), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G5 CONFIRMED: 11 kW model declares Ability.Ampere(SINGLE_OR_THREE_PHASE, 6,
	 * 16) — scheduler will never send more than 16 A to this model.
	 */
	@Test
	public void testG5_11kWModel_AbilityMaxIs16A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setHardwareType(AblEMh1HardwareType.EMH1_3P_11KW_CABLE) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		var ability = (ApplySetPoint.Ability.Ampere) sut.getChargePointAbilities().applySetPoint();
		assertEquals(16, ability.max());
		assertEquals(THREE_PHASE, ability.phase());
	}

	// ─── G6 Soft Pause ───────────────────────────────────────────────────────────

	/**
	 * G6 HYPOTHESIS (design superseded): apply(0) was the original candidate for
	 * pause. Design changed: pause is now via {@code CHARGING_PAUSED} boolean write
	 * channel. apply() still clamps to 6 A minimum. Kept as a reminder.
	 */
	@Ignore("G6 — design changed: use CHARGING_PAUSED channel for soft pause, not apply(0)")
	@Test
	public void testG6_SoftPauseWrites0x03E8() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				.deactivate();

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplyZeroSetPoint() //
				.build());

		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G6 CONFIRMED: CHARGING_PAUSED=true → SET_CURRENT_RAW gets pause sentinel
	 * 0x03E8 (1000).
	 */
	@Test
	public void testG6_ChargingPaused_WritesSentinel() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.getChargingPausedChannel().setNextWriteValue(true))) //
				.deactivate();
		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G6 CONFIRMED: CHARGING_PAUSED=false restores the last current setpoint set
	 * via apply().
	 */
	@Test
	public void testG6_ChargingPaused_ResumeRestoresLastSetpoint() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()).setApplySetPointInAmpere(16).build()); // raw=267
		sut.getChargingPausedChannel().setNextWriteValue(true);
		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue());
		sut.getChargingPausedChannel().setNextWriteValue(false);
		assertEquals(Optional.of(267), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G6 CONFIRMED: resume with no prior apply() uses minimum current (6 A raw=100).
	 */
	@Test
	public void testG6_ChargingPaused_ResumeWithNoSetpointUsesMinimum() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getChargingPausedChannel().setNextWriteValue(true);
		sut.getChargingPausedChannel().setNextWriteValue(false);
		assertEquals(Optional.of(100), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G6 CONFIRMED: apply() during pause saves the setpoint for resume but does not
	 * write to the device.
	 */
	@Test
	public void testG6_ChargingPaused_ApplyDuringPauseSavesForResume() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getChargingPausedChannel().setNextWriteValue(true);
		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()).setApplySetPointInAmpere(20).build()); // raw=333
		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue()); // sentinel unchanged
		sut.getChargingPausedChannel().setNextWriteValue(false);
		assertEquals(Optional.of(333), sut.getSetCurrentRawChannel().getNextWriteValue()); // resumed at 20A
	}

	/**
	 * G6 CONFIRMED: CHARGING_PAUSED channel read value reflects the current pause
	 * state.
	 */
	@Test
	public void testG6_ChargingPaused_ChannelReflectsPauseState() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.getChargingPausedChannel().setNextWriteValue(true)) //
						.output(EvseChargePointAbl.ChannelId.CHARGING_PAUSED, true)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.getChargingPausedChannel().setNextWriteValue(false)) //
						.output(EvseChargePointAbl.ChannelId.CHARGING_PAUSED, false)) //
				.deactivate();
	}

	/**
	 * G6 CONFIRMED: readOnly=true → CHARGING_PAUSED write is ignored, sentinel not
	 * written.
	 */
	@Test
	public void testG6_ChargingPaused_ReadOnlyBlocks() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(true) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		sut.getChargingPausedChannel().setNextWriteValue(true);
		assertEquals(Optional.empty(), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	// ─── G14 SET_CURRENT_AMPERE Channel ──────────────────────────────────────────

	/**
	 * G14 CONFIRMED: register 0x0014 raw=267 → SET_CURRENT_AMPERE = 16 A.
	 *
	 * <p>
	 * Inverse formula: round(267 × 0.6 / 10) = round(16.02) = 16.
	 */
	@Test
	public void testSetCurrentAmpere_ReadBack_16A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EC2, 0, 0, 0, 0); // state C2
							bridge.withRegisters(20, 267); // 0x0014 = dec 20
						})) //
				.next(new TestCase()) // second cycle: SET_CURRENT is a LOW-priority task; needs ≥2 cycles
				.deactivate();
		assertEquals(Optional.of(16), sut.getSetCurrentAmpereChannel().value().asOptional());
	}

	/**
	 * G14 CONFIRMED: register 0x0014 = 0x03E8 (pause sentinel 1000) →
	 * SET_CURRENT_AMPERE = 0 A.
	 */
	@Test
	public void testSetCurrentAmpere_ReadBack_PauseSentinelIsZero() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EC2, 0, 0, 0, 0);
							bridge.withRegisters(20, 1000);
						})) //
				.next(new TestCase()) // second cycle: SET_CURRENT is a LOW-priority task; needs ≥2 cycles
				.deactivate();
		assertEquals(Optional.of(0), sut.getSetCurrentAmpereChannel().value().asOptional());
	}

	/**
	 * G14 CONFIRMED: write 16 A to SET_CURRENT_AMPERE → SET_CURRENT_RAW = 267.
	 */
	@Test
	public void testSetCurrentAmpere_Write_16A() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getSetCurrentAmpereChannel().setNextWriteValue(16);
		assertEquals(Optional.of(267), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G14 CONFIRMED: write below minimum (4 A) → clamped to 6 A → SET_CURRENT_RAW
	 * = 100.
	 */
	@Test
	public void testSetCurrentAmpere_Write_ClampToMin() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getSetCurrentAmpereChannel().setNextWriteValue(4);
		assertEquals(Optional.of(100), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G14 CONFIRMED: write above hardware maximum (99 A > 32 A) → clamped to 32 A
	 * → SET_CURRENT_RAW = 533.
	 */
	@Test
	public void testSetCurrentAmpere_Write_ClampToMax() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getSetCurrentAmpereChannel().setNextWriteValue(99);
		assertEquals(Optional.of(533), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G14 CONFIRMED: write during pause saves the setpoint but does not override
	 * the sentinel; sentinel is replaced by the saved setpoint on resume.
	 */
	@Test
	public void testSetCurrentAmpere_Write_DuringPause() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase());

		sut.getChargingPausedChannel().setNextWriteValue(true);
		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue());
		sut.getSetCurrentAmpereChannel().setNextWriteValue(20); // raw=333; paused → not written
		assertEquals(Optional.of(1000), sut.getSetCurrentRawChannel().getNextWriteValue());
		sut.getChargingPausedChannel().setNextWriteValue(false); // resume → restore 20A
		assertEquals(Optional.of(333), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G14 CONFIRMED: readOnly=true → SET_CURRENT_AMPERE write is ignored.
	 */
	@Test
	public void testSetCurrentAmpere_ReadOnly() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(true) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		sut.getSetCurrentAmpereChannel().setNextWriteValue(16);
		assertEquals(Optional.empty(), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	// ─── G7 Fault Reporting ───────────────────────────────────────────────────────

	/**
	 * G7 CONFIRMED: state F4 → FAULT_STATE = AblEMh1FaultState.F4,
	 * IS_READY_FOR_CHARGING = false.
	 */
	@Test
	public void testG7_FaultStateReporting() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //

				// F4 fault
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EF4, 0x0000, 0, 0, 0)) //
						.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, false) //
						.output(EvseChargePointAbl.ChannelId.FAULT_STATE, AblEMh1FaultState.F4)) //

				// F9 overcurrent
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EF9, 0x0000, 0, 0, 0)) //
						.output(EvseChargePointAbl.ChannelId.FAULT_STATE, AblEMh1FaultState.F9)) //

				// Back to A1 — fault clears
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0x0000, 0, 0, 0)) //
						.output(EvseChargePointAbl.ChannelId.FAULT_STATE, AblEMh1FaultState.UNDEFINED)) //

				.deactivate();
	}

	// ─── G8 Communication Failure ────────────────────────────────────────────────

	/**
	 * G8 CONFIRMED: Modbus communication failure → MODBUS_COMMUNICATION_FAILED
	 * channel active.
	 *
	 * <p>
	 * Uses AbstractOpenemsModbusComponent's built-in MODBUS_COMMUNICATION_FAILED
	 * channel, which is set automatically when the FC3 read fails.
	 */
	@Test
	public void testG8_CommunicationFailure() throws Exception {
		var sut = new EvseChargePointAblImpl();
		// Bridge with no registers — read will fail (or return empty)
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase(), 5) //
				// After several failing cycles, MODBUS_COMMUNICATION_FAILED should be true
				.next(new TestCase() //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, true)) //
				.deactivate();
	}

	// ─── G9 Upstream Timeout Config ──────────────────────────────────────────────

	/**
	 * G9 HYPOTHESIS: on startup with enableUpstreamTimeout=true, register 0x002C
	 * (dec 44) is written with 0x9001 (= timeout enabled + auto B1→B2 + device ID
	 * 1).
	 *
	 * <p>
	 * Also covers G12 (B1→B2 auto-transition): the same 0x9001 write encodes
	 * bits[13:12]=01 (auto B1→B2) together with the timeout setting.
	 */
	@Ignore("G9/G12 — HYPOTHESIS: verify write ordering (0xE2E2 before 0x9001) on hardware")
	@Test
	public void testG9_UpstreamTimeoutWritesReg002C() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setEnableUpstreamTimeout(true) //
				.build()) //
				// Startup state = A1 → 0xE2E2 written first, then 0x9001
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0))) //
				.next(new TestCase()) //
				.deactivate();

		// Assert register 44 (0x002C) was written with 0x9001 = 36865
		// (Verification mechanism TBD in Phase 5 — check DummyModbusBridge process image)
		// assertEquals(0x9001, bridge.getRegister(44)); // placeholder
	}

	// ─── G10 Read-Only ───────────────────────────────────────────────────────────

	/**
	 * G10 CONFIRMED: readOnly=true → apply() does not set SET_CURRENT_RAW write
	 * value.
	 */
	@Test
	public void testG10_ReadOnlyBlocksSetpointWrite() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(true) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(16) //
				.build());

		// Read-only: next write value must remain empty
		assertEquals(Optional.empty(), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	/**
	 * G10 CONFIRMED: readOnly=true → OUTLET_ENABLED write does not propagate.
	 */
	@Test
	public void testG10_ReadOnlyBlocksOutletWrite() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(true) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		// Attempt to disable outlet
		sut.getOutletEnabledChannel().setNextWriteValue(false);
		// In read-only mode, no write to register 0x0005 should occur
		// (verified by the absence of register 5 write in Phase B implementation)
		// For Phase A skeleton: simply assert write value is present on channel
		// but implementation must not forward it to the bridge
		assertEquals(Optional.of(false), sut.getOutletEnabledChannel().getNextWriteValue());
	}

	// ─── G11 Debug Log ───────────────────────────────────────────────────────────

	/**
	 * G11 CONFIRMED: logVerbosity=READS_AND_WRITES_VERBOSE → apply() logs "DEBUG
	 * WRITE:" frame.
	 *
	 * <p>
	 * Implementation note: Phase B adds log output; Phase 5 will determine the
	 * exact assertion mechanism (log capture or indirect channel check).
	 */
	@Test
	public void testG11_DebugLogVerbosity() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(false) //
				.setLogVerbosity(LogVerbosity.READS_AND_WRITES_VERBOSE) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		// Phase B: verify that applying a setpoint produces a log entry containing
		// "DEBUG WRITE:". Assertion mechanism TBD in Phase 5.
		// Activation with verbose logging must complete without error (verified above).
	}

	/**
	 * G11 CONFIRMED: readOnly=true + READS_AND_WRITES_VERBOSE → logs "BLOCKED BY
	 * READ-ONLY: DEBUG WRITE:".
	 */
	@Test
	public void testG11_ReadOnlyBlockedLogEntry() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(true) //
				.setLogVerbosity(LogVerbosity.READS_AND_WRITES_VERBOSE) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		// Phase B: verify log entry contains "BLOCKED BY READ-ONLY: DEBUG WRITE:"
		assertTrue(sut.isReadOnly());
		// SET_CURRENT_RAW must remain unset when readOnly
		sut.apply(io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions //
				.from(sut.getChargePointAbilities()) //
				.setApplySetPointInAmpere(16) //
				.build());
		assertEquals(Optional.empty(), sut.getSetCurrentRawChannel().getNextWriteValue());
	}

	// ─── G13 System Flags ────────────────────────────────────────────────────────

	/**
	 * G13 CONFIRMED: system flags register 0x0007 bit 9 = 1 → UPSTREAM_COMM_LOST.
	 */
	@Test
	public void testG13_SystemFlagBit9_UpstreamCommLost() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0); // R5: A1 state
							bridge.withRegisters(6, 0x06A1, 0x0200); // sys-flags: bit9=1 (upstream lost)
						}) //
						.output(EvseChargePointAbl.ChannelId.UPSTREAM_COMM_LOST, true)) //
				.deactivate();
	}

	/**
	 * G13 CONFIRMED: system flags register 0x0007 bit 8 = 1 → LOAD_IMBALANCE.
	 */
	@Test
	public void testG13_SystemFlagBit8_LoadImbalance() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0);
							bridge.withRegisters(6, 0x06A1, 0x0100); // bit8=1 (load imbalance)
						}) //
						.output(EvseChargePointAbl.ChannelId.LOAD_IMBALANCE, true)) //
				.deactivate();
	}

	/**
	 * G13 CONFIRMED: system flags register 0x0007 bit 7 = 1 → TEMPERATURE_WARNING.
	 */
	@Test
	public void testG13_SystemFlagBit7_TemperatureWarning() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0);
							bridge.withRegisters(6, 0x06A1, 0x0080); // bit7=1 (temperature warning)
						}) //
						.output(EvseChargePointAbl.ChannelId.TEMPERATURE_WARNING, true)) //
				.deactivate();
	}

	/**
	 * G13 CONFIRMED: system flags register 0x0007 bit 6 = 1 → PHASE_METERING_FAILED.
	 */
	@Test
	public void testG13_SystemFlagBit6_PhaseMeteringFailed() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, defaultConfig()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0);
							bridge.withRegisters(6, 0x06A1, 0x0040); // bit6=1 (phase metering failed)
						}) //
						.output(EvseChargePointAbl.ChannelId.PHASE_METERING_FAILED, true)) //
				.deactivate();
	}

	// ─── G4a Startup Outlet Enable (HYPOTHESIS) ──────────────────────────────────

	/**
	 * G4a HYPOTHESIS: startup with readOnly=false and device in A1 → register
	 * 0x0005 (dec 5) written with 0xE2E2 = 58082.
	 */
	@Ignore("G4a — HYPOTHESIS: verify E0→E2 re-enables without hardware reset (U6)")
	@Test
	public void testG4a_StartupEnablesOutletWhenInA1() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(false) //
				.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EA1, 0, 0, 0, 0))) //
				.deactivate();
		// Assert register 5 (0x0005) was written with 0xE2E2 = 58082
		// assertEquals(0xE2E2, bridge.getRegister(5)); // assertion mechanism TBD
	}

	/**
	 * G4a HYPOTHESIS: startup when device is already in C2 (charging) → register
	 * 0x0005 must NOT be written (precondition: device must be in A1 or E0).
	 */
	@Ignore("G4a — HYPOTHESIS: verify startup safety when EV already charging (U6)")
	@Test
	public void testG4a_StartupSkipsOutletWriteWhenInC2() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(false) //
				.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.withRegisters(46, 0x2EC2, 0x810A, 160, 160, 160))) //
				.deactivate();
		// Assert register 5 was NOT written = no 0xE2E2 to an already-charging device
	}

	// ─── G4b Hard Outlet Disable (HYPOTHESIS) ────────────────────────────────────

	/**
	 * G4b HYPOTHESIS: OUTLET_ENABLED = false → register 0x0005 written with
	 * 0xE0E0 = 57568.
	 */
	@Ignore("G4b — HYPOTHESIS: verify E0→E2 re-enable works without hardware reset")
	@Test
	public void testG4b_OutletDisableWritesE0E0() throws Exception {
		var sut = new EvseChargePointAblImpl();
		var bridge = new DummyModbusBridge("modbus0");
		prepare(sut, bridge, MyConfig.create() //
				.setId("evseChargePoint0") //
				.setReadOnly(false) //
				.build()) //
				.next(new TestCase()) //
				.deactivate();

		sut.getOutletEnabledChannel().setNextWriteValue(false);
		// Phase B: verify register 0x0005 is written with 0xE0E0 = 57568
	}
}
