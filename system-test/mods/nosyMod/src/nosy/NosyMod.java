package nosy;

import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.LoadingStage;
import brownshome.modding.Mod;
import brownshome.modding.annotation.DefineMod;

import java.util.Collection;
import java.util.List;

@DefineMod(
		name = "nosyMod",
		version = "0.0.0"
)
public class NosyMod extends Mod {
	public NosyMod() {
		super(new NosyModInfo());
	}

	@Override
	protected Collection<LoadingStage> configureLoadingProcess() {
		return List.of(
				createLoadingStageRequest("List all mods", () -> {
					for (var mod : serviceLoader(Mod.class)) {
						Logger.logger().log("Mod: %s found", mod);
					}
				})
		);
	}
}
