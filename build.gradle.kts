import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class DownloadImguiFatTask : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val f = outputFile.get().asFile
        if (f.exists()) return
        f.parentFile.mkdirs()
        val url = URI("https://github.com/SpaiR/imgui-java/releases/download/v${version.get()}/native-libraries.zip").toURL()
        val zis = ZipInputStream(url.openStream())
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            if (entry.name == "libimgui-java64.dylib") { f.writeBytes(zis.readBytes()); break }
            zis.closeEntry(); entry = zis.nextEntry
        }
        zis.close()
    }
}

plugins {
    id("java-library")
    id("eclipse")

    id("com.gtnewhorizons.gtnhconvention")
    id("com.gtnewhorizons.retrofuturagradle")
    id("com.github.spotbugs") version "6.1.11"
}

group = "github.thehighcruw.dimensium"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

minecraft {
    mcVersion.set("1.7.10")
    username.set("Developer")
    injectedTags.put("VERSION", project.version)
    extraRunJvmArguments.add("-Dorg.lwjgl.openal.libname=/dev/null")
    extraRunJvmArguments.add("-XX:ErrorFile=${rootDir}/run/jvm-crash-%p.log")
    extraRunJvmArguments.add("-XX:+CreateMinidumpOnCrash")
}

tasks.injectTags.configure {
    outputClassName.set("github.thehighcruw.dimensium.Tags")
}

tasks.processResources.configure {
    val projVersion = project.version.toString()
    inputs.property("version", projVersion)
    filesMatching("mcmod.info") {
        expand(mapOf(
            "modId"            to "dimensium",
            "modName"          to "Dimensium",
            "modVersion"       to projVersion,
            "minecraftVersion" to "1.7.10"
        ))
    }
}

repositories {
    maven {
        name = "GTNH Maven"
        url = uri("https://nexus.gtnewhorizons.com/repository/public/")
    }
    mavenCentral()
}


// imgui-java: embed binding + all-platform natives into our mod jar.
// isTransitive=false excludes imgui-java-app's bundled LWJGL3 (lwjgl3ify provides it at runtime).
val imguiEmbed: Configuration by configurations.creating { isTransitive = false }
configurations.implementation.get().extendsFrom(imguiEmbed)

val cpdConfiguration = configurations.create("cpd") {
    @Suppress("DEPRECATION")
    isVisible = false
    isTransitive = true
    description = "CPD (PMD copy-paste detection) dependencies"
}

dependencies {
    cpdConfiguration("net.sourceforge.pmd:pmd-dist:6.55.0")
}

tasks.register<JavaExec>("cpdCheck") {
    group = "verification"
    description = "Run CPD (copy-paste detection) on main sources"
    classpath = cpdConfiguration
    mainClass.set("net.sourceforge.pmd.cpd.CPD")
    val reportDir = layout.buildDirectory.dir("reports/cpd")
    doFirst {
        reportDir.get().asFile.mkdirs()
    }
    args = listOf(
        "--minimum-tokens", "80",
        "--language", "java",
        "--files", "src/main/java",
        "--format", "text"
    )
    isIgnoreExitValue = true
    doLast {
        println("CPD report: ${reportDir.get().asFile.absolutePath}")
    }
}

tasks.test.configure {
    useJUnit()
}

spotbugs {
    toolVersion.set("4.8.6")
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { enabled = true }
    reports.create("xml") { enabled = false }
}

afterEvaluate {
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        isEnabled = true
    }
}

val imguiVersion = "1.92.7.1"

// The macOS native in the Maven Central jar is x86_64 only.
// The GitHub release ships a universal fat binary (x86_64 + arm64).
// Download and cache it, then substitute it in the mod jar.
val imguiMacosUniversalDir = layout.buildDirectory.dir("imgui-universal")
val downloadImguiMacosFat by tasks.registering(DownloadImguiFatTask::class) {
    version.set(imguiVersion)
    outputFile.set(imguiMacosUniversalDir.map { it.file("libimgui-java64.dylib") })
}

// Embed imgui-java classes and natives into the mod jar.
// Exclude the x86_64-only macOS dylib from the Maven artifact; inject the universal one instead.
tasks.named<Jar>("jar") {
    dependsOn(downloadImguiMacosFat)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(imguiEmbed.map { f ->
        if (f.isDirectory) f
        else zipTree(f).matching { exclude("io/imgui/java/native-bin/libimgui-java64.dylib") }
    })
    from(downloadImguiMacosFat.get().outputs.files) {
        into("io/imgui/java/native-bin")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // imgui-java 1.92.7.1: binding classes + GL3 backend + per-platform natives.
    // Maven Central natives are x86_64 only for macOS; the macOS dylib is replaced above
    // with the universal fat binary (x86_64 + arm64) from the GitHub release.
    imguiEmbed("io.github.spair:imgui-java-binding:$imguiVersion")
    imguiEmbed("io.github.spair:imgui-java-natives-macos:$imguiVersion")
    imguiEmbed("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    imguiEmbed("io.github.spair:imgui-java-natives-linux:$imguiVersion")

    // GTNHLib: compile against the dev jar (deobf), present at runtime in both dev and modpack.
    compileOnly("com.github.GTNewHorizons:GTNHLib:0.11.35:dev") { isTransitive = false }
    runtimeOnly(rfg.deobf("com.github.GTNewHorizons:GTNHLib:0.11.35"))
    compileOnly("com.github.GTNewHorizons:NotEnoughItems:2.8.118-GTNH:dev") { isTransitive = false }
    runtimeOnly(rfg.deobf("com.github.GTNewHorizons:NotEnoughItems:2.8.118-GTNH"))
    compileOnly("com.github.GTNewHorizons:GT5-Unofficial:5.09.54.118:dev") { isTransitive = false }

    // Mods to be able to test block detections
    runtimeOnlyNonPublishable("com.github.GTNewHorizons:GT5-Unofficial:5.09.54.118")
    runtimeOnlyNonPublishable("com.github.GTNewHorizons:NEI-Integration:1.5.3")
}
