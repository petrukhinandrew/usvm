package machine

import org.jacodb.api.jvm.JcClassOrInterface

data class JcSpringMachineOptions(
    val springAnalysisMode: JcSpringAnalysisMode
)

object JcSpringConfigProvider {
    enum class SpringCpSource {
        JAR, BUILD_DIRS
    }

    var classpathSource: SpringCpSource? = null
    private set

    fun bindClasspathSource(source: SpringCpSource) {
        classpathSource = source
    }

    private var controllerName: String? = null

    fun analyzeController(name: String) {
        controllerName = name
    }

    private var handlerName: String? = null

    fun analyzeHandler(name: String) {
        handlerName = name
    }

    private var paths: MutableSet<String> = mutableSetOf()

    fun addAnalyzePath(value: String) {
        paths.add(value)
    }


    var bootApp: String? = null
        private set

    fun analyzeBootApp(qualifiedName: String) {
        bootApp = qualifiedName
    }

    private var testClassStub: JcClassOrInterface? = null

    fun getTestClassStub(): JcClassOrInterface = testClassStub!!

    fun bindTestClassStub(clazz: JcClassOrInterface) {
        testClassStub = clazz
    }

    fun shouldAnalyze(pathTemplate: String, controller: String, handler: String): Boolean {
        return pathTemplate in paths || controller == controllerName || handler == handlerName
    }

    fun reset() {
        paths = mutableSetOf()
        controllerName = null
        handlerName = null
        bootApp = null
    }
}
