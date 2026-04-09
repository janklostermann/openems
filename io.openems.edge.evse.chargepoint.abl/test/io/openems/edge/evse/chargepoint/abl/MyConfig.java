package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evse.chargepoint.abl.enums.AblEMh1HardwareType;
import io.openems.edge.evse.chargepoint.abl.enums.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias = "";
		private boolean enabled = true;
		private boolean readOnly = false;
		private AblEMh1HardwareType hardwareType = AblEMh1HardwareType.EMH1_3P_22KW_CABLE;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;
		private int fallbackVoltageL1 = 230;
		private int fallbackVoltageL2 = 230;
		private int fallbackVoltageL3 = 230;
		private String voltageL1ChannelAddress = "";
		private String voltageL2ChannelAddress = "";
		private String voltageL3ChannelAddress = "";
		private boolean enableUpstreamTimeout = true;
		private LogVerbosity logVerbosity = LogVerbosity.NONE;
		private String modbusId = "modbus0";
		private int modbusUnitId = 1;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setHardwareType(AblEMh1HardwareType hardwareType) {
			this.hardwareType = hardwareType;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public Builder setFallbackVoltageL1(int fallbackVoltageL1) {
			this.fallbackVoltageL1 = fallbackVoltageL1;
			return this;
		}

		public Builder setFallbackVoltageL2(int fallbackVoltageL2) {
			this.fallbackVoltageL2 = fallbackVoltageL2;
			return this;
		}

		public Builder setFallbackVoltageL3(int fallbackVoltageL3) {
			this.fallbackVoltageL3 = fallbackVoltageL3;
			return this;
		}

		public Builder setVoltageL1ChannelAddress(String voltageL1ChannelAddress) {
			this.voltageL1ChannelAddress = voltageL1ChannelAddress;
			return this;
		}

		public Builder setVoltageL2ChannelAddress(String voltageL2ChannelAddress) {
			this.voltageL2ChannelAddress = voltageL2ChannelAddress;
			return this;
		}

		public Builder setVoltageL3ChannelAddress(String voltageL3ChannelAddress) {
			this.voltageL3ChannelAddress = voltageL3ChannelAddress;
			return this;
		}

		public Builder setEnableUpstreamTimeout(boolean enableUpstreamTimeout) {
			this.enableUpstreamTimeout = enableUpstreamTimeout;
			return this;
		}

		public Builder setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public AblEMh1HardwareType hardwareType() {
		return this.builder.hardwareType;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public int fallbackVoltageL1() {
		return this.builder.fallbackVoltageL1;
	}

	@Override
	public int fallbackVoltageL2() {
		return this.builder.fallbackVoltageL2;
	}

	@Override
	public int fallbackVoltageL3() {
		return this.builder.fallbackVoltageL3;
	}

	@Override
	public String voltageL1ChannelAddress() {
		return this.builder.voltageL1ChannelAddress;
	}

	@Override
	public String voltageL2ChannelAddress() {
		return this.builder.voltageL2ChannelAddress;
	}

	@Override
	public String voltageL3ChannelAddress() {
		return this.builder.voltageL3ChannelAddress;
	}

	@Override
	public boolean enableUpstreamTimeout() {
		return this.builder.enableUpstreamTimeout;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public String Modbus_target() {
		return generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "";
	}
}
