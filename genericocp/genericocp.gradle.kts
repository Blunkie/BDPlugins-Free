import ProjectVersions.openosrsVersion

version = "0.0.1"

project.extra["PluginName"] = "BD Generic OCP"
project.extra["PluginDescription"] = "BD Generic OCP"
project.extra["ProjectSupportUrl"] = "https://github.com/bigdrizzle13/BDPlugins"

dependencies {
    compileOnly(group = "com.openosrs.externals", name = "oneclickutils", version = "0.0.1+");
    compileOnly(group = "com.openosrs.externals", name = "iutils", version = "4.8.2+");
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"],
                    "Plugin-Dependencies" to arrayOf(
                        nameToId("iUtils"), nameToId("BD One Click Utils")).joinToString(),
            ))
        }
    }
}
