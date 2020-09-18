package basemod;

import basemod.api.BaseModAPI;
import browngu.logging.Logger;
import browngu.logging.Severity;
import brownshome.modding.LoadingStage;
import brownshome.modding.Mod;
import brownshome.modding.annotation.DefineMod;

import java.util.Collection;
import java.util.List;

@DefineMod(
		name = "baseMod",
		version = "1.0.0"
)
public class BaseMod extends Mod implements BaseModAPI {
	public BaseMod() {
		super(new BaseModInfo());
	}

	@Override
	protected Collection<LoadingStage> configureLoadingProcess() {
		return List.of(
				createLoadingStageRequest("Log information", () -> {
					Logger.logger().log(Severity.INFO, "%s initialized successfully", this);
				}),

				createLoadingStageRequest("Crash", () -> { throw new RuntimeException("Intentional crash"); })
		);
	}
}
