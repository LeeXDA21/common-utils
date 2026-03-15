import com.android.build.api.dsl.CommonExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

/**
 * Converts a camelCase or mixedCase string to ENV_VAR_STYLE (uppercase with underscores).
 * Example: githubAccessToken -> GITHUB_ACCESS_TOKEN
 */
fun String.toEnvVarStyle(): String = replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

/**
 * Note: To configure GitHub credentials, you have to generate an access token with at least `read:packages` scope at
 * https://github.com/settings/tokens/new and then add it to any of the following:
 *
 * - Add `ghUsername` and `ghAccessToken` to Global Gradle Properties
 * - Set `GH_USERNAME` and `GH_ACCESS_TOKEN` in your environment variables or
 * - Create a `github.properties` file in your project folder with the following content:
 *      ghUsername=&lt;YOUR_GITHUB_USERNAME&gt;
 *      ghAccessToken=&lt;YOUR_GITHUB_ACCESS_TOKEN&gt;
 */
fun getProperty(key: String): String =
    Properties().apply { rootProject.file("github.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }.getProperty(key)
        ?: rootProject.findProperty(key)?.toString()
        ?: System.getenv(key.toEnvVarStyle())
        ?: throw GradleException("Property $key not found")

val githubUsername = getProperty("ghUsername")
val githubAccessToken = getProperty("ghAccessToken")
val githubRepositoryOwner =
    rootProject.findProperty("ghRepositoryOwner")?.toString()
        ?: System.getenv("GH_REPOSITORY_OWNER")
        ?: githubUsername
val githubRepositoryName =
    rootProject.findProperty("ghRepositoryName")?.toString()
        ?: System.getenv("GH_REPOSITORY_NAME")
        ?: "common-utils"
val githubRepositoryUrl = "https://github.com/$githubRepositoryOwner/$githubRepositoryName"
val githubPackageUrl = "https://maven.pkg.github.com/$githubRepositoryOwner/$githubRepositoryName"
val mavenGroup = "io.github.${githubRepositoryOwner.lowercase()}"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.github.com/LeeXDA21/oneui-design") {
            credentials {
                username = githubUsername
                password = githubAccessToken
            }
        }
    }
}

subprojects {
    plugins.withId("com.android.base") {
        project.extensions.findByType(CommonExtension::class.java)?.apply {
            compileOptions.apply {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            configurations.all {
                exclude(group = "androidx.core", module = "core")
                exclude(group = "androidx.core", module = "core-ktx")
                exclude(group = "androidx.customview", module = "customview")
                exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
                exclude(group = "androidx.drawerlayout", module = "drawerlayout")
                exclude(group = "androidx.viewpager2", module = "viewpager2")
                exclude(group = "androidx.viewpager", module = "viewpager")
                exclude(group = "androidx.appcompat", module = "appcompat")
                exclude(group = "androidx.fragment", module = "fragment")
                exclude(group = "androidx.preference", module = "preference")
                exclude(group = "androidx.recyclerview", module = "recyclerview")
                exclude(group = "androidx.slidingpanelayout", module = "slidingpanelayout")
                exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
                exclude(group = "com.google.android.material", module = "material")
            }
        }
    }
    afterEvaluate {
        if (!project.plugins.hasPlugin(libs.plugins.maven.publish.get().pluginId)) {
            return@afterEvaluate
        }
        val artifact = "common-utils"
        group = mavenGroup
        version = libs.versions.common.utils.get()
        println("Evaluated $group:$artifact:$version")
        project.extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = artifact
                    afterEvaluate {
                        from(components["release"])
                    }
                    pom {
                        name = artifact
                        description = "A collection of common utility functions and classes for my Android projects."
                        url = githubRepositoryUrl
                        developers {
                            developer {
                                id = "LeeXDA"
                                name = "Leonard Lemke"
                                email = "leo@leonard-lemke.com"
                                url = "https://www.leonard-lemke.com"
                                timezone = "Europe/Berlin"
                            }
                        }
                        scm {
                            connection = "scm:git:git://github.com/$githubRepositoryOwner/$githubRepositoryName.git"
                            developerConnection = "scm:git:ssh://github.com/$githubRepositoryOwner/$githubRepositoryName.git"
                            url = githubRepositoryUrl
                        }
                        issueManagement{
                            system = "GitHub Issues"
                            url = "$githubRepositoryUrl/issues"
                        }
                        licenses {
                            license {
                                name = "Apache-2.0"
                                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                                distribution = "repo"
                            }
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri(githubPackageUrl)
                    credentials {
                        username = githubUsername
                        password = githubAccessToken
                    }
                }
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
