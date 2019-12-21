import brownshome.modding.*;
import childmod.*;

module childmod {
	requires static brownshome.modding.annotation;

	requires brownshome.modding;
	requires browngu.logging;
	requires basemod;

	provides Mod with ChildMod;
	provides ModInfo with ChildModInfo;

	opens childmod to basemod;
}