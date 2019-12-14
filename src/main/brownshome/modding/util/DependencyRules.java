package brownshome.modding.util;

import brownshome.modding.ModVersion;
import brownshome.modding.util.DependencyRule;

public enum DependencyRules implements DependencyRule {
	/**
	 * Requires a particular version of a mod, ignoring build information
	 */
	EXACTLY() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return v.matches(required);
		}
	},

	NEWER() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return v.isNewerThan(required);
		}
	},

	NEWER_OR_EQUAL() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return v.isNewerThan(required) || v.matches(required);
		}
	},

	OLDER() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return required.isNewerThan(v);
		}
	},

	OLDER_OR_EQUAL() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return required.isNewerThan(v) || v.matches(required);
		}
	},

	/**
	 * Excludes a particular version, ignoring build information
	 */
	EXCLUDE() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return !v.matches(required);
		}
	},

	/**
	 * Requires that the chosen version by compatible with this one. Pre-release tags are never compatible unless
	 * they match exactly.
	 */
	COMPATIBLE() {
		@Override
		public boolean meets(ModVersion required, ModVersion v) {
			return v.isCompatibleWith(required);
		}
	};
}
