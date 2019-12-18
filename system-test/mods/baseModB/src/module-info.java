import brownshome.modding.*;
import basemod.*;

module basemod {
	requires brownshome.modding.annotation;
	requires browngu.logging;

	provides Mod with BaseMod;
	provides ModInfo with BaseModInfo;
}