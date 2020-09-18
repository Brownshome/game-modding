package brownshome.modding.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class ModdingPlugin : Plugin<Project> {
	override fun apply(target: Project) {
		target.configurations.create("mod") {
			it.isCanBeConsumed = false
		}

		target.afterEvaluate { proj ->
			proj.tasks.create("collectMods", Copy::class.java) { task ->
				proj.configurations.getByName("mod").resolvedConfiguration.firstLevelModuleDependencies.forEach { dep ->
					val folderName = if (dep.moduleVersion.equals("unspecified")) {
						dep.moduleName
					} else {
						"${dep.moduleName}-${dep.moduleVersion}"
					}

					task.into("${proj.buildDir}/mods")

					task.from(dep.moduleArtifacts.map { it.file }) { copySpec ->
						copySpec.into(folderName)
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