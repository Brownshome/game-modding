package brownshome.modding.modsource;

import brownshome.modding.Mod;
import brownshome.modding.ModInfo;
import brownshome.modding.modsource.ModSource;

import java.io.IOException;
import java.net.URL;
import java.util.*;

class ClassloaderModSource extends ModSource {
	private final ServiceLoader<ModInfo> infoLoader;
	private final ServiceLoader<Mod> modLoader;

	private final class ModSourceClassloader extends ClassLoader {
		final Collection<ClassLoader> classloaders;

		private ModSourceClassloader(ClassLoader parent, Collection<ClassLoader> modloaders) {
			this.classloaders = modloaders;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			for (var loader : classloaders) {
				try {
					return loader.loadClass(name);
				} catch(ClassNotFoundException e) { /* Do nothing */ }
			}

			throw new ClassNotFoundException(name);
		}

		@Override
		protected URL findResource(String name) {
			for (var loader : classloaders) {
				URL result = loader.getResource(name);

				if (result != null) {
					return result;
				}
			}

			return null;
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			Collection<URL> all = new ArrayList<>();

			for (var loader : classloaders) {
				for (Enumeration<URL> e = loader.getResources(name); e.hasMoreElements(); ) {
					all.add(e.nextElement());
				}
			}

			return Collections.enumeration(all);
		}
	};

	ClassloaderModSource(Collection<ClassLoader> loaders) {
		ClassLoader combinedLoader = new ModSourceClassloader(Thread.currentThread().getContextClassLoader(), loaders);

		infoLoader = ServiceLoader.load(ModInfo.class, combinedLoader);
		modLoader = ServiceLoader.load(Mod.class, combinedLoader);
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
	@SuppressWarnings("unchecked")
	public <MOD_CLASS extends Mod> MOD_CLASS loadMod(ModInfo info) {
		for (var mod : modLoader) {
			if (mod.info().equals(info)) {
				return (MOD_CLASS) mod;
			}
		}

		throw new IllegalArgumentException(String.format("Cannot find '%s'", info));
	}
}
