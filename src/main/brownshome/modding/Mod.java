package brownshome.modding;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * An instance of this class represents a mod.
 */
public abstract class Mod {
	private final ModInfo info;

	// Lazy
	private ModLoader loader;

	protected Mod(ModInfo info) {
		this.info = info;
	}

	/**
	 * Creates a blank request for a stages stage.
	 * @param name The name of the stage.
	 * @param action A task to be executed
	 * @return An object that can be used to configure the stages stage request.
	 */
	protected final LoadingStage createLoadingStageRequest(LoadingStageName name, Callable<Void> action) {
		return new LoadingStage(this, name, action);
	}

	/**
	 * Forces a collection of requests to execute in their iteration order.
	 */
	protected final void fixExecutionOrder(Collection<LoadingStage> requests) {
		Iterator<LoadingStage> it = requests.iterator();

		if (!it.hasNext()) {
			return;
		}

		var previousRequest = it.next();

		while (it.hasNext()) {
			previousRequest.before(previousRequest = it.next());
		}
	}

	/**
	 * Configures the stages process for this module.
	 * @return a collection of stages stage requests. These requests may be executed in an order other than the iteration
	 *         unless steps are taken to configure the order {@link #fixExecutionOrder}
	 */
	protected abstract Collection<LoadingStage> configureLoadingProcess();

	/**
	 * Gets a mod by name. This mod should be listed as a dependency of this mod if it is to be used in this method to
	 * ensure that it is present.
	 *
	 * @param name the name of the requested mod
	 * @param <MOD_TYPE> the expected type of the mod
	 *
	 * @throws IllegalArgumentException if the mod does not exist
	 * @throws ClassCastException if the mod is not the expected class. This will never be thrown if MOD_TYPE = Mod.
	 */
	@SuppressWarnings("unchecked")
	protected final <MOD_TYPE extends Mod> MOD_TYPE getNamedMod(String name) {
		return (MOD_TYPE) loader.getNamedMod(name);
	}

	public ModInfo info() {
		return info;
	}

	// MOD-LOADER ONLY CALLS.

	/**
	 * Called by the ModLoader to set itself as the loader of this mod once it has been constructed.
	 */
	void loader(ModLoader loader) {
		assert this.loader == null;

		this.loader = loader;
	}

	@Override
	public String toString() {
		return info().toString();
	}
}