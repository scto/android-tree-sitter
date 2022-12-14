import com.android.build.gradle.BaseExtension
import com.itsaky.androidide.treesitter.TreeSitterPlugin
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
  id("com.android.application") version "7.4.0-rc01" apply false
  id("com.android.library") version "7.4.0-rc01" apply false
  id("com.vanniktech.maven.publish.base") version "0.22.0" apply false
}

fun Project.configureBaseExtension(isTreeSitterModule: Boolean) {
  extensions.findByType(BaseExtension::class)?.run {
    compileSdkVersion(33)

    if (isTreeSitterModule) {
      @Suppress("UnstableApiUsage")
      ndkVersion = "24.0.8215888"
    }

    defaultConfig {
      minSdk = 21
      targetSdk = 33
      versionCode = project.findProperty("VERSION_CODE")!!.toString().toInt()
      versionName = project.findProperty("VERSION_NAME")!!.toString()

      if (isTreeSitterModule) {
        val rootProjDir: String = rootProject.projectDir.absolutePath
        val tsDir = "${rootProjDir}/tree-sitter-lib"
        externalNativeBuild {
          cmake { arguments("-DPROJECT_DIR=${rootProjDir}", "-DTS_DIR=${tsDir}") }
        }
      }
    }

    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }

    if (isTreeSitterModule) {
      externalNativeBuild {
        cmake {
          path = file("${project.projectDir.absolutePath}/src/main/cpp/CMakeLists.txt")
          version = "3.22.1"
        }
      }
    }
  }
}

subprojects {
  val isTreeSitterModule = plugins.hasPlugin(TreeSitterPlugin::class.java)

  plugins.withId("com.android.application") { configureBaseExtension(isTreeSitterModule) }
  plugins.withId("com.android.library") { configureBaseExtension(isTreeSitterModule) }

  plugins.withId("com.vanniktech.maven.publish.base") {
    @Suppress("UnstableApiUsage")
    configure<MavenPublishBaseExtension> {
      group = "io.github.itsaky"
      version = project.findProperty("VERSION_NAME")!!
      pomFromGradleProperties()
      publishToMavenCentral(SonatypeHost.S01)
      signAllPublications()
      configure(AndroidSingleVariantLibrary(publishJavadocJar = false))
    }
  }
}

tasks.register<Delete>("clean").configure {
  delete(rootProject.buildDir)
  delete(rootProject.file("build/host"))
}