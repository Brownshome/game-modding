package brownshome.modding.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.slf4j.LoggerFactory

class ModdingPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		target.configurations.create("mod") {
			it.isCanBeConsumed = false
		}

		target.afterEvaluate { proj ->
			proj.tasks.create("collectMods", Sync::class.java) { task ->
				task.into("${proj.buildDir}/mods")

				proj.configurations.getByName("mod").resolvedConfiguration.firstLevelModuleDependencies.forEach { dep ->
					val logger = LoggerFactory.getLogger(ModdingPlugin::class.java)
					logger.info("Collecting: ${dep.moduleName}")

					val folderName = if (dep.moduleVersion == "unspecified") {
						dep.moduleName
					} else {
						"${dep.moduleName}-${dep.moduleVersion}"
					}

					task.from(dep.moduleArtifacts.map { it.file }) {
						it.into(folderName)
					}
				}

				task.dependsOn(proj.configurations.getByName("mod").buildDependencies)

				if (proj.plugins.hasPlugin("application")) {
					proj.tasks.getByName("run") { runTask ->
						runTask.dependsOn(task)
					}
				}
			}
		}
	}
}