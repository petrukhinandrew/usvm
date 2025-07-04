@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.usvm.jmv.spring.models

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [AnalysisProcessModel.kt:12]
 */
class AnalysisProcessModel private constructor(
    private val _runAnalysis: RdSignal<AnalysisRequest>,
    private val _newTestGenerated: RdSignal<String>,
    private val _errorOccured: RdSignal<ErrorDescriptor>,
    private val _generatedTests: RdList<String>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(AnalysisRequest)
            serializers.register(ErrorDescriptor)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): AnalysisProcessModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.analysisProcessModel or revise the extension scope instead", ReplaceWith("protocol.analysisProcessModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): AnalysisProcessModel  {
            AnalysisProcessRoot.register(protocol.serializers)
            
            return AnalysisProcessModel()
        }
        
        
        const val serializationHash = -5914040717084540285L
        
    }
    override val serializersOwner: ISerializersOwner get() = AnalysisProcessModel
    override val serializationHash: Long get() = AnalysisProcessModel.serializationHash
    
    //fields
    val runAnalysis: IAsyncSignal<AnalysisRequest> get() = _runAnalysis
    val newTestGenerated: IAsyncSignal<String> get() = _newTestGenerated
    val errorOccured: IAsyncSignal<ErrorDescriptor> get() = _errorOccured
    val generatedTests: IMutableViewableList<String> get() = _generatedTests
    //methods
    //initializer
    init {
        _generatedTests.optimizeNested = true
    }
    
    init {
        _runAnalysis.async = true
        _newTestGenerated.async = true
        _errorOccured.async = true
        _generatedTests.async = true
    }
    
    init {
        bindableChildren.add("runAnalysis" to _runAnalysis)
        bindableChildren.add("newTestGenerated" to _newTestGenerated)
        bindableChildren.add("errorOccured" to _errorOccured)
        bindableChildren.add("generatedTests" to _generatedTests)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdSignal<AnalysisRequest>(AnalysisRequest),
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<ErrorDescriptor>(ErrorDescriptor),
        RdList<String>(FrameworkMarshallers.String)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AnalysisProcessModel (")
        printer.indent {
            print("runAnalysis = "); _runAnalysis.print(printer); println()
            print("newTestGenerated = "); _newTestGenerated.print(printer); println()
            print("errorOccured = "); _errorOccured.print(printer); println()
            print("generatedTests = "); _generatedTests.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AnalysisProcessModel   {
        return AnalysisProcessModel(
            _runAnalysis.deepClonePolymorphic(),
            _newTestGenerated.deepClonePolymorphic(),
            _errorOccured.deepClonePolymorphic(),
            _generatedTests.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.analysisProcessModel get() = getOrCreateExtension(AnalysisProcessModel::class) { @Suppress("DEPRECATION") AnalysisProcessModel.create(lifetime, this) }



/**
 * #### Generated from [AnalysisProcessModel.kt:13]
 */
data class AnalysisRequest (
    val userClassPath: List<String>,
    val libsClassPath: List<String>,
    val analysisController: String?,
    val analysisHandle: String?,
    val analysisPath: List<String>,
    val analysisBootApp: String,
    val testClassName: String,
    val testClassPackage: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AnalysisRequest> {
        override val _type: KClass<AnalysisRequest> = AnalysisRequest::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AnalysisRequest  {
            val userClassPath = buffer.readList { buffer.readString() }
            val libsClassPath = buffer.readList { buffer.readString() }
            val analysisController = buffer.readNullable { buffer.readString() }
            val analysisHandle = buffer.readNullable { buffer.readString() }
            val analysisPath = buffer.readList { buffer.readString() }
            val analysisBootApp = buffer.readString()
            val testClassName = buffer.readString()
            val testClassPackage = buffer.readString()
            return AnalysisRequest(userClassPath, libsClassPath, analysisController, analysisHandle, analysisPath, analysisBootApp, testClassName, testClassPackage)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AnalysisRequest)  {
            buffer.writeList(value.userClassPath) { v -> buffer.writeString(v) }
            buffer.writeList(value.libsClassPath) { v -> buffer.writeString(v) }
            buffer.writeNullable(value.analysisController) { buffer.writeString(it) }
            buffer.writeNullable(value.analysisHandle) { buffer.writeString(it) }
            buffer.writeList(value.analysisPath) { v -> buffer.writeString(v) }
            buffer.writeString(value.analysisBootApp)
            buffer.writeString(value.testClassName)
            buffer.writeString(value.testClassPackage)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as AnalysisRequest
        
        if (userClassPath != other.userClassPath) return false
        if (libsClassPath != other.libsClassPath) return false
        if (analysisController != other.analysisController) return false
        if (analysisHandle != other.analysisHandle) return false
        if (analysisPath != other.analysisPath) return false
        if (analysisBootApp != other.analysisBootApp) return false
        if (testClassName != other.testClassName) return false
        if (testClassPackage != other.testClassPackage) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + userClassPath.hashCode()
        __r = __r*31 + libsClassPath.hashCode()
        __r = __r*31 + if (analysisController != null) analysisController.hashCode() else 0
        __r = __r*31 + if (analysisHandle != null) analysisHandle.hashCode() else 0
        __r = __r*31 + analysisPath.hashCode()
        __r = __r*31 + analysisBootApp.hashCode()
        __r = __r*31 + testClassName.hashCode()
        __r = __r*31 + testClassPackage.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AnalysisRequest (")
        printer.indent {
            print("userClassPath = "); userClassPath.print(printer); println()
            print("libsClassPath = "); libsClassPath.print(printer); println()
            print("analysisController = "); analysisController.print(printer); println()
            print("analysisHandle = "); analysisHandle.print(printer); println()
            print("analysisPath = "); analysisPath.print(printer); println()
            print("analysisBootApp = "); analysisBootApp.print(printer); println()
            print("testClassName = "); testClassName.print(printer); println()
            print("testClassPackage = "); testClassPackage.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:26]
 */
data class ErrorDescriptor (
    val message: String,
    val stackTrace: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ErrorDescriptor> {
        override val _type: KClass<ErrorDescriptor> = ErrorDescriptor::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ErrorDescriptor  {
            val message = buffer.readString()
            val stackTrace = buffer.readString()
            return ErrorDescriptor(message, stackTrace)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ErrorDescriptor)  {
            buffer.writeString(value.message)
            buffer.writeString(value.stackTrace)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ErrorDescriptor
        
        if (message != other.message) return false
        if (stackTrace != other.stackTrace) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + message.hashCode()
        __r = __r*31 + stackTrace.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ErrorDescriptor (")
        printer.indent {
            print("message = "); message.print(printer); println()
            print("stackTrace = "); stackTrace.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
