package brownshome.modding;

import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.util.PredefinedLoadingStages;
import brownshome.modding.util.StringLoadingStage;

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * An instance of this class represents a mod.
 */
public abstract class Mod {
	private final ModInfo info;

	private final LoadingStage startStage;
	private final LoadingStage endStage;

	// Lazy
	private ModLoader loader;

	protected Mod(ModInfo info) {
		this.info = info;
		startStage = new LoadingStage(this, PredefinedLoadingStages.START, this::logStart);
		endStage = new LoadingStage(this, PredefinedLoadingStages.END, this::logEnd);
	}

	/**
	 * Creates a blank request for a stages stage.
	 * @param name The name of the stage.
	 * @param action A task to be executed
	 * @return An object that can be used to configure the stages stage request.
	 */
	protected final LoadingStage createLoadingStageRequest(LoadingStageName name, Callable<Void> action) {
		return new LoadingStage(this, name, action).after(startStage).before(endStage);
	}

	protected final LoadingStage createLoadingStageRequest(String name, Callable<Void> action) {
		return createLoadingStageRequest(new StringLoadingStage(name), action);
	}

	protected final LoadingStage createLoadingStageRequest(LoadingStageName name, Runnable action) {
		return createLoadingStageRequest(name, Executors.callable(action, null));
	}

	protected final LoadingStage createLoadingStageRequest(String name, Runnable action) {
		return createLoadingStageRequest(new StringLoadingStage(name), Executors.callable(action, null));
	}

	/**
	 * Forces a collection of requests to execute in their iteration order.
	 *
	 * @return the same collection
	 */
	protected final Collection<LoadingStage> fixExecutionOrder(Collection<LoadingStage> requests) {
		Iterator<LoadingStage> it = requests.iterator();

		if (!it.hasNext()) {
			return requests;
		}

		var previousRequest = it.next();

		while (it.hasNext()) {
			previousRequest.before(previousRequest = it.next());
		}

		return requests;
	}

	/**
	 * Configures the stages process for this module.
	 * @return a collection of stages stage requests. These requests may be executed in an order other than the iteration
	 *         unless steps are taken to configure the order {@link #fixExecutionOrder}
	 */
	protected abstract Collection<? extends LoadingStage> configureLoadingProcess();

	final LoadingStage startStage() {
		return startStage;
	}

	final LoadingStage endStage() {
		return endStage;
	}

	private Void logEnd() {
		Logger.logger().log(Severity.INFO, "Mod %s finished loading.", this);
		return null;
	}

	private Void logStart() {
		Logger.logger().log(Severity.INFO, "Mod %s started loading.", this);
		return null;
	}

	/**
	 * Gets a mod by name. This mod should be listed as a dependency of this mod if it is to be used in this method to
	 * ensure that it is present.
	 *
	 * @param name the name of the requested mod
	 * @param <MOD_TYPE> the expected type of the mod
	 *
	 * @throws ClassCastException if the mod is not the expected class. This will never be thrown if {@link MOD_TYPE} is {@link Mod}.
	 */
	@SuppressWarnings("unchecked")
	protected final <MOD_TYPE extends Mod> MOD_TYPE namedMod(String name) {
		return (MOD_TYPE) loader.namedMod(name);
	}

	/**
	 * Gets a service loader that returns all service providers loaded currently. This loader will load from all classpath
	 * sources and loaded mods.
	 *
	 * @param serviceClass the class to load services for
	 * @param <TYPE> the type of the service provider
	 * @return a service loader that will load from all mod and classpath sources.
	 */
	protected final <TYPE> ServiceLoader<TYPE> serviceLoader(Class<TYPE> serviceClass) {
		return loader.serviceLoader(serviceClass);
	}

	/**
	 * Returns the mod that is currently executing a loading stage
	 */
	protected final Mod currentlyLoadingMod() {
		return loader.currentlyLoadingMod();
	}

	public ModInfo info() {
		return info;
	}

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

	final void signalIsLoading() {
		loader.signalIsLoading(this);
	}
}