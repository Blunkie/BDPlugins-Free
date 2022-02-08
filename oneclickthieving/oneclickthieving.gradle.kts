version = "0.0.1"

project.extra["PluginName"] = "BD One Click Pickpocket"
project.extra["PluginDescription"] = "BD QOL for pickpocketing"

dependencies {
    compileOnly(group = "com.openosrs.externals", name = "iutils", version = "4.7.7+");
    compileOnly(group = "com.openosrs.externals", name = "oneclickutils", version = "0.0.1+");
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
                "Plugin-Dependencies" to
                        arrayOf(
                            nameToId("iUtils"),  nameToId("BD One Click Utils")).joinToString(),
            ))
        }
    }
}
