package gamax92.ocsymon;

/** Interface defining callbacks provided by the host. */
public interface SymonNativeFunction {
	Object invoke(Object[] args);
}
