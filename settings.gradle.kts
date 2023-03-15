rootProject.name = "koishibot"

var subProjectNames = listOf(
    "koishibot-core",
    "koishibot-network",
    "koishibot-message-api",
    "koishibot-qq-backend",
    "koishibot-kook-backend",
    "koishibot-telegram-backend",
    "koishibot-delegate-backend",
    "koishibot-monitor"
)

for (name in subProjectNames) {
    include(":${name}")
    val pro = project(":${name}")
    pro.projectDir = file(name)
}