package org.usvm.jvm.spring.models.definitions

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType
import com.jetbrains.rd.generator.nova.async
import com.jetbrains.rd.generator.nova.field
import com.jetbrains.rd.generator.nova.immutableList
import com.jetbrains.rd.generator.nova.list
import com.jetbrains.rd.generator.nova.nullable
import com.jetbrains.rd.generator.nova.signal

@Suppress("unused")
object AnalysisProcessModel: Ext(AnalysisProcessRoot) {
    val classpathSource = enum {
        +"JAR"
        + "BUILD_DIRS"
    }

    val prepareDbRequest = structdef {
        field("classpathSource", classpathSource)
        field("userClassPath", immutableList(PredefinedType.string))
        field("libsClassPath", immutableList(PredefinedType.string))
    }

    val analysisRequest = structdef {
        field("analysisController", PredefinedType.string.nullable)
        field("analysisHandle", PredefinedType.string.nullable)
        field("analysisPath", immutableList(PredefinedType.string))
        field("analysisBootApp", PredefinedType.string)

        field("testClassName", PredefinedType.string)
        field("testClassPackage", PredefinedType.string)
    }

    val procNotification = basestruct { }

    val procStarted = structdef extends procNotification { }

    val procAnalysisStarted = structdef extends procNotification { }

    val procDbReady = structdef extends procNotification {
        field("elapsedTime", PredefinedType.int)
    }

    val procCpReady = structdef extends procNotification { }

    val procCtxReady = structdef extends procNotification {
        field("elapsedTime", PredefinedType.long)
    }

    val procError = structdef extends procNotification {
        field("message", PredefinedType.string)
        field("stackTrace", immutableList(PredefinedType.string))
    }

    val procTerminated = structdef extends procNotification {

    }

    val procAnalysisFinished = structdef extends procNotification { }

    init {
        signal("prepareDb", prepareDbRequest).async

        signal("runAnalysis", analysisRequest).async

        signal("stopAnalysis", PredefinedType.void).async

        signal("processSignal", procNotification).async

        signal("serverReady", PredefinedType.void).async

        signal("refreshContext", PredefinedType.void).async

        list("generatedTests", PredefinedType.string).async
    }
}