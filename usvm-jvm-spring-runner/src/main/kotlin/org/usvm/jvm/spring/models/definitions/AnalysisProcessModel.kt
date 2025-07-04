package org.usvm.jvm.spring.models.definitions

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType
import com.jetbrains.rd.generator.nova.async
import com.jetbrains.rd.generator.nova.field
import com.jetbrains.rd.generator.nova.immutableList
import com.jetbrains.rd.generator.nova.list
import com.jetbrains.rd.generator.nova.nullable
import com.jetbrains.rd.generator.nova.signal

object AnalysisProcessModel: Ext(AnalysisProcessRoot) {
    val analysisRequest = structdef {
        field("userClassPath", immutableList(PredefinedType.string))
        field("libsClassPath", immutableList(PredefinedType.string))

        field("analysisController", PredefinedType.string.nullable)
        field("analysisHandle", PredefinedType.string.nullable)
        field("analysisPath", immutableList(PredefinedType.string))
        field("analysisBootApp", PredefinedType.string)

        field("testClassName", PredefinedType.string)
        field("testClassPackage", PredefinedType.string)
    }

    val errorDescriptor = structdef {
        field("message", PredefinedType.string)
        field("stackTrace", PredefinedType.string)
    }

    init {
        signal("runAnalysis", analysisRequest).async
        signal("newTestGenerated", PredefinedType.string).async
        signal("errorOccured", errorDescriptor).async
        list("generatedTests", PredefinedType.string).async
    }
}