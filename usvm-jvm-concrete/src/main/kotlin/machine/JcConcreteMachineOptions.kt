package machine

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
    jarPath: String
): JcConcreteMachineOptions {
    override fun userClassesIn(cp: JcClasspath): Sequence<JcClassOrInterface> {
        return userClasses.asSequence().mapNotNull { cp.findClassOrNull(it) }
    }

    override fun isUserClass(clazz: JcClassOrInterface): Boolean = userClasses.contains(clazz.name)

    private val userClasses: Set<String>

    companion object {
        private const val SPRING_BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes"
        private const val CLASS_SUFFIX = ".class"
    }

    init {
        JarFile(jarPath).use { jarFile ->
            val classesPrefix =
                checkNotNull(jarFile.manifest.mainAttributes[SPRING_BOOT_CLASSES_ATTRIBUTE] as? String) {
                    "cannot fetch $SPRING_BOOT_CLASSES_ATTRIBUTE from manifest"
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