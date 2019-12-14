package brownshome.modding;

public class ModStageExecutionException extends ModLoadingException {
	private final Mod mod;
	private final LoadingStageName stageName;
	private final Exception cause;

	ModStageExecutionException(Mod mod, LoadingStageName stageName, Exception cause) {
		super("Exception in '" + mod + "[" + stageName + "]'", cause);

		this.mod = mod;
		this.stageName = stageName;
		this.cause = cause;
	}

	public Mod mod() {
		return mod;
	}

	public LoadingStageName stageName() {
		return stageName;
	}

	public Exception cause() {
		return cause;
	}
}
