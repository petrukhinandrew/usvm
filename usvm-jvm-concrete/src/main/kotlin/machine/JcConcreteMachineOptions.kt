package machine

import java.io.File
import java.util.jar.JarFile
import org.jacodb.api.jvm.JcByteCodeLocation
import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.impl.features.classpaths.JcUnknownClass

interface JcConcreteMachineOptions {
    fun userClassesIn(cp: JcClasspath): Sequence<JcClassOrInterface>

    fun isUserClass(clazz: JcClassOrInterface): Boolean
}

data class JcBuildDirsConcreteMachineOptionsImpl(
    val projectLocations: List<JcByteCodeLocation> = emptyList(),
    val dependenciesLocations: List<JcByteCodeLocation> = emptyList(),
): JcConcreteMachineOptions {

    override fun isUserClass(clazz: JcClassOrInterface): Boolean {
        return projectLocations.contains(clazz.declaration.location.jcLocation)
    }

    override fun userClassesIn(cp: JcClasspath): Sequence<JcClassOrInterface> {
        return projectLocations
            .asSequence()
            .flatMap { it.classNames ?: emptySet() }
            .mapNotNull { cp.findClassOrNull(it) }
            .filterNot { it is JcUnknownClass }
    }
}

class JcJarConcreteMachineOptions(
    jarFile: File,
    val extraUserClasses: Set<String> = emptySet()
): JcConcreteMachineOptions {
    override fun userClassesIn(cp: JcClasspath): Sequence<JcClassOrInterface> {
        return (userClasses + extraUserClasses).asSequence().mapNotNull { cp.findClassOrNull(it) }
    }

    override fun isUserClass(clazz: JcClassOrInterface): Boolean = userClasses.contains(clazz.name)

    private val userClasses: Set<String>

    companion object {
        private const val SPRING_BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes"
        private const val CLASS_SUFFIX = ".class"
    }

    init {
        val jarPath = jarFile.absolutePath
        JarFile(jarPath).use { jarFile ->
            val classesPrefix =
                jarFile.manifest.mainAttributes.getValue(SPRING_BOOT_CLASSES_ATTRIBUTE)
            check(classesPrefix.isNotBlank()) {
                "$SPRING_BOOT_CLASSES_ATTRIBUTE is blank"
            }
            userClasses = jarFile.entries().asSequence().mapNotNull { entry ->
                val entryName = entry.name
                if (entryName.startsWith(classesPrefix) && entryName.endsWith(CLASS_SUFFIX)) {
                    entryName.removePrefix(classesPrefix).removeSuffix(CLASS_SUFFIX).replace("/", ".")
                } else null
            }.toSet()
        }
    }
}