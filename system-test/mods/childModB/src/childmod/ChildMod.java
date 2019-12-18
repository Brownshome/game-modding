package childmod;

import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.LoadingStage;
import brownshome.modding.Mod;
import brownshome.modding.annotation.DefineMod;

import java.util.Collection;
import java.util.List;

@DefineMod(
		name = "childMod",
		version = "1.0.0"
)
public class ChildMod extends Mod {
	public ChildMod() {
		super(new ChildModInfo());
	}

	@Override
	protected Collection<LoadingStage> configureLoadingProcess() {
		return List.of(
				createLoadingStageRequest("Log information", () -> {
					Logger.logger.log(Severity.INFO, "%s initialized successfully", this);
				})
		);
	}
}
