package brownshome.modding.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.tasks.Sync

class ModdingPlugin : Plugin<Project> {
	private val javaModuleAttribute = Attribute.of("javaModule", Boolean::class.javaObjectType)

	override fun apply(target: Project) {
		fun applyCommonAttributes(attribute: AttributeContainer) {
			attribute.attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category::class.java, LIBRARY))
			attribute.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, target.objects.named(LibraryElements::class.java, JAR))
			attribute.attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling::class.java, EXTERNAL))
		}

		fun configureAsPrivateClasspath(attribute: AttributeContainer) {
			applyCommonAttributes(attribute)

			attribute.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage::class.java, JAVA_RUNTIME))
		}

		fun configureAsSharedClasspath(attribute: AttributeContainer) {
			applyCommonAttributes(attribute)

			attribute.attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage::class.java, JAVA_API))
		}

		val modConfiguration = target.configurations.create("mod") {
			it.isVisible = false
			it.isCanBeConsumed = false
			it.isCanBeResolved = false
		}

		val topLevelMods = target.configurations.create("topLevelMods") {
			it.extendsFrom(modConfiguration)

			it.isVisible = false
			it.isCanBeConsumed = false
			it.isTransitive = false

			configureAsSharedClasspath(it.attributes)
		}

		val privateModClasspath = target.configurations.create("privateModClasspath") {
			it.extendsFrom(modConfiguration)

			it.isVisible = false
			it.isCanBeConsumed = false

			configureAsPrivateClasspath(it.attributes)
		}

		val sharedModClasspath = target.configurations.create("sharedModClasspath") {
			it.extendsFrom(modConfiguration)

			it.isCanBeConsumed = false

			configureAsSharedClasspath(it.attributes)
		}

		target.afterEvaluate { proj ->
			val existingRuntimeDependencies = proj.configurations.getAt("runtimeClasspath") +
					proj.tasks.getByPath("jar").outputs.files

			val sharedMods = sharedModClasspath - topLevelMods;

			proj.tasks.create("collectMods", Sync::class.java) { task ->
				task.into("${proj.buildDir}/mods")
				task.from(sharedMods - existingRuntimeDependencies)

				modConfiguration.dependencies.forEach { dep ->
					val folderName = if (dep.version == "unspecified") {
						dep.name
					} else {
						"${dep.name}-${dep.version}"
					}

					val privateMods = privateModClasspath.fileCollection(dep) - existingRuntimeDependencies - sharedMods

					task.from(privateMods) {
						it.into(folderName)
					}
				}

				if (proj.plugins.hasPlugin("application")) {
					proj.tasks.getByName("run") { runTask ->
						runTask.dependsOn(task)
					}
				}
			}

			if (proj.plugins.hasPlugin("de.jjohannes.extra-java-module-info")) {
				privateModClasspath.attributes.attribute(javaModuleAttribute, true)
				topLevelMods.attributes.attribute(javaModuleAttribute, true)
				sharedModClasspath.attributes.attribute(javaModuleAttribute, true)
			}
		}
	}
}