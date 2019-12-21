import brownshome.modding.*;
import basemod.*;

module basemod {
	exports basemod.api;

	requires static brownshome.modding.annotation;

	requires brownshome.modding;
	requires browngu.logging;

	provides Mod with BaseMod;
	provides ModInfo with BaseModInfo;
}