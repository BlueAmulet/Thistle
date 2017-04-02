package gamax92.thistle.api;

import java.util.HashMap;
import java.util.Map;

import li.cil.oc.api.network.Environment;

public class WrapperRegistry {

	private static final WrapperRegistry INSTANCE = new WrapperRegistry();

	private Map<Class<? extends Environment>, Class<? extends ThistleWrapper>> registry = new HashMap<Class<? extends Environment>, Class<? extends ThistleWrapper>>();

	public static WrapperRegistry instance() {
		return INSTANCE;
	}

	public static void registerWrapper(Class<? extends Environment> host, Class<? extends ThistleWrapper> wrapper) {
		instance().registry.put(host, wrapper);
	}

	public static Class<? extends ThistleWrapper> getWrapper(Class<? extends Environment> host) {
		return instance().registry.get(host);
	}
}
