package brownshome.modding;

/**
 * The root exception of all failures with the mod loading system.
 */
public class ModLoadingException extends Exception {
	public ModLoadingException() {
	}

	public ModLoadingException(String message) {
		super(message);
	}

	public ModLoadingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModLoadingException(Throwable cause) {
		super(cause);
	}
}
