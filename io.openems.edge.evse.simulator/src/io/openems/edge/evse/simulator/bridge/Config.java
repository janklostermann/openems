package io.openems.edge.evse.simulator.bridge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.modbus.api.LogVerbosity;

@ObjectClassDefinition(//
		name = "Simulator EVSE Modbus Bridge", //
		description = "Simulates a Modbus EVSE chargepoint for testing and demonstration purposes.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "modbus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Device Type", description = "The type of EVSE device to simulate")
	DeviceType deviceType() default DeviceType.ALFEN;

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the simulated Modbus device")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Log-Verbosity", description = "The level of logging for Modbus communication")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	@AttributeDefinition(name = "Initial Voltage L1", description = "Initial voltage on phase L1 in Volts")
	double initialVoltageL1() default 230.0;

	@AttributeDefinition(name = "Initial Voltage L2", description = "Initial voltage on phase L2 in Volts")
	double initialVoltageL2() default 230.0;

	@AttributeDefinition(name = "Initial Voltage L3", description = "Initial voltage on phase L3 in Volts")
	double initialVoltageL3() default 230.0;

	@AttributeDefinition(name = "Number of Phases", description = "Number of active phases (1 or 3)")
	int phases() default 3;

	String webconsole_configurationFactory_nameHint() default "Simulator EVSE Modbus Bridge [{id}]";
}
