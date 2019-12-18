package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Uses a url as a source of mods. The URL should point to a jar containing one mod. Additionally, there are
 * some nuances regarding the loading of Mod classes and non-mod classes.
 *
 * When a class is required to be loaded by a mod, that mod's specific classloader should be checked first, after that,
 * if the class cannot be found, the class-loaders of all selected mods should be queried (so that APIs from other mods
 * can be loaded).
 */
final class URLModSource extends ModSource {
	private final ServiceLoader<ModInfo> infoLoader;
	private final URL url;

	// Not final as the serviceloader may need to be remade when the parent is set.
	private ClassLoader classLoader;
	private ServiceLoader<Mod> modLoader;

	URLModSource(URL url) {
		this.url = url;

		// ModInfo objects only have access to their own files and application files, as there is no guarantee that
		infoLoader = ServiceLoader.load(ModInfo.class, new URLClassLoader(String.format("URLClassLoader[%s]", url), new URL[] { url }, ClassLoader.getSystemClassLoader()));
	}

	@Override
	public Collection<ModInfo> availableMods(String modName) {
		List<ModInfo> availableVersions = new ArrayList<>();

		for (var info : infoLoader) {
			if (!info.name().equals(modName)) {
				continue;
			}

			availableVersions.add(info);
		}

		availableVersions.sort(null);

		return availableVersions;
	}

	@Override
	public void setParentLoader(ClassLoader delegate) {
		classLoader = new URLClassLoader(String.format("URLClassLoader[%s]", url), new URL[] { url }, ClassLoader.getSystemClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				synchronized (getClassLoadingLock(name)) {
					// First, check if the class has already been loaded
					Class<?> c = findLoadedClass(name);

					classLookup:
					if (c == null) {
						// Search parent
						try {
							c = getParent().loadClass(name);
							break classLookup;
						} catch (ClassNotFoundException ignored) {  }

						// Search self
						try {
							c = findClass(name);
							break classLookup;
						} catch (ClassNotFoundException ignored) {  }

						// Search delegate
						c = delegate.loadClass(name);
					}

					if (resolve) {
						resolveClass(c);
					}

					return c;
				}
			}
		};

		modLoader = ServiceLoader.load(Mod.class, classLoader);
	}

	@Override
	public ClassLoader classLoader(ModInfo info) {
		return classLoader;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info) {
		for (var mod : modLoader) {
			if (mod.info().equals(info)) {
				return (MOD_CLASS) mod;
			}
		}

		throw new IllegalArgumentException(String.format("Cannot find '%s'", info));
	}

	@Override
	public String toString() {
		return String.format("ModSource[%s]", url);
	}
}
