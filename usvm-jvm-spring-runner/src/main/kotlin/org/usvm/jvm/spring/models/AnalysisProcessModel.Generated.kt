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
 * #### Generated from [AnalysisProcessModel.kt:13]
 */
class AnalysisProcessModel private constructor(
    private val _prepareDb: RdSignal<PrepareDbRequest>,
    private val _runAnalysis: RdSignal<AnalysisRequest>,
    private val _stopAnalysis: RdSignal<Unit>,
    private val _processSignal: RdSignal<ProcNotification>,
    private val _serverReady: RdSignal<Unit>,
    private val _generatedTests: RdList<String>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(ClasspathSource.marshaller)
            serializers.register(PrepareDbRequest)
            serializers.register(AnalysisRequest)
            serializers.register(ProcStarted)
            serializers.register(ProcAnalysisStarted)
            serializers.register(ProcDbReady)
            serializers.register(ProcCpReady)
            serializers.register(ProcCtxReady)
            serializers.register(ProcError)
            serializers.register(ProcAnalysisFinished)
            serializers.register(ProcNotification_Unknown)
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
        
        
        const val serializationHash = 3658648698745594590L
        
    }
    override val serializersOwner: ISerializersOwner get() = AnalysisProcessModel
    override val serializationHash: Long get() = AnalysisProcessModel.serializationHash
    
    //fields
    val prepareDb: IAsyncSignal<PrepareDbRequest> get() = _prepareDb
    val runAnalysis: IAsyncSignal<AnalysisRequest> get() = _runAnalysis
    val stopAnalysis: IAsyncSignal<Unit> get() = _stopAnalysis
    val processSignal: IAsyncSignal<ProcNotification> get() = _processSignal
    val serverReady: IAsyncSignal<Unit> get() = _serverReady
    val generatedTests: IMutableViewableList<String> get() = _generatedTests
    //methods
    //initializer
    init {
        _generatedTests.optimizeNested = true
    }
    
    init {
        _prepareDb.async = true
        _runAnalysis.async = true
        _stopAnalysis.async = true
        _processSignal.async = true
        _serverReady.async = true
        _generatedTests.async = true
    }
    
    init {
        bindableChildren.add("prepareDb" to _prepareDb)
        bindableChildren.add("runAnalysis" to _runAnalysis)
        bindableChildren.add("stopAnalysis" to _stopAnalysis)
        bindableChildren.add("processSignal" to _processSignal)
        bindableChildren.add("serverReady" to _serverReady)
        bindableChildren.add("generatedTests" to _generatedTests)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdSignal<PrepareDbRequest>(PrepareDbRequest),
        RdSignal<AnalysisRequest>(AnalysisRequest),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdSignal<ProcNotification>(AbstractPolymorphic(ProcNotification)),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdList<String>(FrameworkMarshallers.String)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AnalysisProcessModel (")
        printer.indent {
            print("prepareDb = "); _prepareDb.print(printer); println()
            print("runAnalysis = "); _runAnalysis.print(printer); println()
            print("stopAnalysis = "); _stopAnalysis.print(printer); println()
            print("processSignal = "); _processSignal.print(printer); println()
            print("serverReady = "); _serverReady.print(printer); println()
            print("generatedTests = "); _generatedTests.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AnalysisProcessModel   {
        return AnalysisProcessModel(
            _prepareDb.deepClonePolymorphic(),
            _runAnalysis.deepClonePolymorphic(),
            _stopAnalysis.deepClonePolymorphic(),
            _processSignal.deepClonePolymorphic(),
            _serverReady.deepClonePolymorphic(),
            _generatedTests.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.analysisProcessModel get() = getOrCreateExtension(AnalysisProcessModel::class) { @Suppress("DEPRECATION") AnalysisProcessModel.create(lifetime, this) }



/**
 * #### Generated from [AnalysisProcessModel.kt:25]
 */
data class AnalysisRequest (
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
            val analysisController = buffer.readNullable { buffer.readString() }
            val analysisHandle = buffer.readNullable { buffer.readString() }
            val analysisPath = buffer.readList { buffer.readString() }
            val analysisBootApp = buffer.readString()
            val testClassName = buffer.readString()
            val testClassPackage = buffer.readString()
            return AnalysisRequest(analysisController, analysisHandle, analysisPath, analysisBootApp, testClassName, testClassPackage)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AnalysisRequest)  {
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
 * #### Generated from [AnalysisProcessModel.kt:14]
 */
enum class ClasspathSource {
    JAR, 
    BUILD_DIRS;
    
    companion object {
        val marshaller = FrameworkMarshallers.enum<ClasspathSource>()
        
    }
}


/**
 * #### Generated from [AnalysisProcessModel.kt:19]
 */
data class PrepareDbRequest (
    val classpathSource: ClasspathSource,
    val userClassPath: List<String>,
    val libsClassPath: List<String>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<PrepareDbRequest> {
        override val _type: KClass<PrepareDbRequest> = PrepareDbRequest::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): PrepareDbRequest  {
            val classpathSource = buffer.readEnum<ClasspathSource>()
            val userClassPath = buffer.readList { buffer.readString() }
            val libsClassPath = buffer.readList { buffer.readString() }
            return PrepareDbRequest(classpathSource, userClassPath, libsClassPath)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: PrepareDbRequest)  {
            buffer.writeEnum(value.classpathSource)
            buffer.writeList(value.userClassPath) { v -> buffer.writeString(v) }
            buffer.writeList(value.libsClassPath) { v -> buffer.writeString(v) }
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
        
        other as PrepareDbRequest
        
        if (classpathSource != other.classpathSource) return false
        if (userClassPath != other.userClassPath) return false
        if (libsClassPath != other.libsClassPath) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classpathSource.hashCode()
        __r = __r*31 + userClassPath.hashCode()
        __r = __r*31 + libsClassPath.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("PrepareDbRequest (")
        printer.indent {
            print("classpathSource = "); classpathSource.print(printer); println()
            print("userClassPath = "); userClassPath.print(printer); println()
            print("libsClassPath = "); libsClassPath.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:56]
 */
class ProcAnalysisFinished (
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcAnalysisFinished> {
        override val _type: KClass<ProcAnalysisFinished> = ProcAnalysisFinished::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcAnalysisFinished  {
            return ProcAnalysisFinished()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcAnalysisFinished)  {
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
        
        other as ProcAnalysisFinished
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcAnalysisFinished (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:39]
 */
class ProcAnalysisStarted (
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcAnalysisStarted> {
        override val _type: KClass<ProcAnalysisStarted> = ProcAnalysisStarted::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcAnalysisStarted  {
            return ProcAnalysisStarted()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcAnalysisStarted)  {
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
        
        other as ProcAnalysisStarted
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcAnalysisStarted (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:45]
 */
class ProcCpReady (
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcCpReady> {
        override val _type: KClass<ProcCpReady> = ProcCpReady::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcCpReady  {
            return ProcCpReady()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcCpReady)  {
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
        
        other as ProcCpReady
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcCpReady (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:47]
 */
class ProcCtxReady (
    val elapsedTime: Long
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcCtxReady> {
        override val _type: KClass<ProcCtxReady> = ProcCtxReady::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcCtxReady  {
            val elapsedTime = buffer.readLong()
            return ProcCtxReady(elapsedTime)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcCtxReady)  {
            buffer.writeLong(value.elapsedTime)
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
        
        other as ProcCtxReady
        
        if (elapsedTime != other.elapsedTime) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + elapsedTime.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcCtxReady (")
        printer.indent {
            print("elapsedTime = "); elapsedTime.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:41]
 */
class ProcDbReady (
    val elapsedTime: Int
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcDbReady> {
        override val _type: KClass<ProcDbReady> = ProcDbReady::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcDbReady  {
            val elapsedTime = buffer.readInt()
            return ProcDbReady(elapsedTime)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcDbReady)  {
            buffer.writeInt(value.elapsedTime)
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
        
        other as ProcDbReady
        
        if (elapsedTime != other.elapsedTime) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + elapsedTime.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcDbReady (")
        printer.indent {
            print("elapsedTime = "); elapsedTime.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:51]
 */
class ProcError (
    val message: String,
    val stackTrace: List<String>
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcError> {
        override val _type: KClass<ProcError> = ProcError::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcError  {
            val message = buffer.readString()
            val stackTrace = buffer.readList { buffer.readString() }
            return ProcError(message, stackTrace)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcError)  {
            buffer.writeString(value.message)
            buffer.writeList(value.stackTrace) { v -> buffer.writeString(v) }
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
        
        other as ProcError
        
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
        printer.println("ProcError (")
        printer.indent {
            print("message = "); message.print(printer); println()
            print("stackTrace = "); stackTrace.print(printer); println()
        }
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:35]
 */
abstract class ProcNotification (
) : IPrintable {
    //companion
    
    companion object : IAbstractDeclaration<ProcNotification> {
        override fun readUnknownInstance(ctx: SerializationCtx, buffer: AbstractBuffer, unknownId: RdId, size: Int): ProcNotification  {
            val objectStartPosition = buffer.position
            val unknownBytes = ByteArray(objectStartPosition + size - buffer.position)
            buffer.readByteArrayRaw(unknownBytes)
            return ProcNotification_Unknown(unknownId, unknownBytes)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    //deepClone
    //contexts
}


class ProcNotification_Unknown (
    override val unknownId: RdId,
    val unknownBytes: ByteArray
) : ProcNotification (
), IUnknownInstance {
    //companion
    
    companion object : IMarshaller<ProcNotification_Unknown> {
        override val _type: KClass<ProcNotification_Unknown> = ProcNotification_Unknown::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcNotification_Unknown  {
            throw NotImplementedError("Unknown instances should not be read via serializer")
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcNotification_Unknown)  {
            buffer.writeByteArrayRaw(value.unknownBytes)
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
        
        other as ProcNotification_Unknown
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcNotification_Unknown (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}


/**
 * #### Generated from [AnalysisProcessModel.kt:37]
 */
class ProcStarted (
) : ProcNotification (
) {
    //companion
    
    companion object : IMarshaller<ProcStarted> {
        override val _type: KClass<ProcStarted> = ProcStarted::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ProcStarted  {
            return ProcStarted()
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ProcStarted)  {
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
        
        other as ProcStarted
        
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProcStarted (")
        printer.print(")")
    }
    
    override fun toString() = PrettyPrinter().singleLine().also { print(it) }.toString()
    //deepClone
    //contexts
}
