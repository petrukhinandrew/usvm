package org.usvm.jvm.spring.models.definitions

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType
import com.jetbrains.rd.generator.nova.async
import com.jetbrains.rd.generator.nova.call
import com.jetbrains.rd.generator.nova.field
import com.jetbrains.rd.generator.nova.immutableList
import com.jetbrains.rd.generator.nova.nullable

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

    val analysisResponse = structdef {
        field("testClass", PredefinedType.string.nullable)
    }

    init {
        call("runAnalysis", analysisRequest, analysisResponse).apply {
            async
        }
    }
}