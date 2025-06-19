package machine

data class JcSpringMachineOptions(
    val springAnalysisMode: JcSpringAnalysisMode
)

object JcSpringConfigProvider {
    private var controllerName: String? = null
    private var handlerName: String? = null
    private var paths: MutableSet<String> = mutableSetOf()
    var bootApp: String? = null
    private set

    fun shoudlAnalyze(pathTemplate: String, controller: String, handler: String): Boolean {
        return pathTemplate in paths || controller == controllerName || handler == handlerName
    }

    fun analyzeController(name: String) {
        controllerName = name
    }

    fun analyzeHandler(name: String) {
        handlerName = name
    }

    fun analyzeBootApp(qualifiedName: String) {
        bootApp = qualifiedName
    }

    fun addAnalyzePath(value: String) {
        paths.add(value)
    }

    fun reset() {
        paths = mutableSetOf()
        controllerName = null
        handlerName = null
        bootApp = null
    }
}
