rootProject.name = "railway-platform"

include(
    "platform-disl-parser",
    "platform-oracle",
    "platform-rwl",
    "platform-app",
    "modules:fleet",
    "modules:dislocation",
    "modules:intake"
)

project(":modules:fleet").projectDir = file("modules/fleet")
project(":modules:dislocation").projectDir = file("modules/dislocation")
project(":modules:intake").projectDir = file("modules/intake")
