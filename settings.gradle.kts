pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SOH3IL"
include(":app")
include(":core:jalali")
include(":core:model")
include(":core:database")
include(":core:scheduler")
include(":core:designsystem")
include(":core:updater")
include(":feature:calendar")
include(":feature:reminder")
include(":feature:settings")
