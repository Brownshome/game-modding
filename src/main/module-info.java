import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

module brownshome.modding {
	requires browngu.logging;

	uses ModInfo;
	uses Mod;

	exports brownshome.modding;
	exports brownshome.modding.util;
	exports brownshome.modding.modsource;
}
