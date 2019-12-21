import brownshome.modding.*;
import childmod.*;

module childmod {
	requires static brownshome.modding.annotation;

	requires browngu.logging;
	requires brownshome.modding;
	requires basemod;

	provides Mod with ChildMod;
	provides ModInfo with ChildModInfo;
}