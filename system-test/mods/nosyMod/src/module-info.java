import brownshome.modding.*;
import nosy.*;

module nosymod {
	requires static brownshome.modding.annotation;

	requires brownshome.modding;
	requires browngu.logging;

	provides Mod with NosyMod;
	provides ModInfo with NosyModInfo;

	uses Mod;
}