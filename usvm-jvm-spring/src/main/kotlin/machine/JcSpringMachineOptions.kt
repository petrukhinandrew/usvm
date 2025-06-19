package machine

import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.ext.hasAnnotation

import util.SpringApproximationPaths

data class JcSpringMachineOptions(
    val springTestGenerationMode: JcSpringTestGenerationMode = JcSpringTestGenerationMode.SpringBootTest,
    val springAnalysisMode: JcSpringAnalysisMode = JcSpringAnalysisMode.EdgeCases,
    val springApproximationPaths: SpringApproximationPaths = SpringApproximationPaths(),
    val sessionConfig: JcSpringAnalysisSessionConfig
)

interface JcSpringAnalysisSessionConfig  {
    val testClass: JcClassOrInterface?

    companion object {
        protected const val CONDITIONAL_ON_PROPERTY =
            "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
    }

    fun pathSubjectsToAnalysis(path: String, handlerName: String, controllerTypeName: String): Boolean

    fun controllerForbiddenForAnalysis(controller: JcClassOrInterface): Boolean {
        // TODO: support conditional controllers and dependent conditional beans
        return controller.hasAnnotation(CONDITIONAL_ON_PROPERTY)
    }
}

data class JcSpringAnyAnalysisConfig(
    override val testClass: JcClassOrInterface? = null
): JcSpringAnalysisSessionConfig {
    override fun pathSubjectsToAnalysis(path: String, handlerName: String, controllerTypeName: String): Boolean {
        return true
    }
}

data class JcSpringControllerAnalysisConfig(
    val controllerName: String,
    override val testClass: JcClassOrInterface
): JcSpringAnalysisSessionConfig {
    override fun pathSubjectsToAnalysis(
        path: String,
        handlerName: String,
        controllerTypeName: String
    ): Boolean {
        return controllerTypeName == controllerName
    }
}

data class JcSpringHandlerAnalysisConfig(
    val handler: String,
    override val testClass: JcClassOrInterface
) : JcSpringAnalysisSessionConfig {
    override fun pathSubjectsToAnalysis(
        path: String,
        handlerName: String,
        controllerTypeName: String
    ): Boolean {
        return handlerName == handler
    }
}

data class JcSpringPathAnalysisConfig (
    val paths: Set<String>,
    override val testClass: JcClassOrInterface
): JcSpringAnalysisSessionConfig {
    override fun pathSubjectsToAnalysis(
        path: String,
        handlerName: String,
        controllerTypeName: String
    ): Boolean {
        return path in paths
    }
}
