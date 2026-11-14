rootProject.name = "railway-platform"

include(
    "platform-disl-parser",
    "platform-oracle",
    "platform-app",
    "modules:fleet",
    "modules:dislocation"
)

project(":modules:fleet").projectDir = file("modules/fleet")
project(":modules:dislocation").projectDir = file("modules/dislocation")
