package gamax92.thistle.api;

import li.cil.oc.api.machine.Context;

public interface IThistleDevice {

	public int lengthThistle();

	public int readThistle(Context context, int address);

	public void writeThistle(Context context, int address, int data);
}
