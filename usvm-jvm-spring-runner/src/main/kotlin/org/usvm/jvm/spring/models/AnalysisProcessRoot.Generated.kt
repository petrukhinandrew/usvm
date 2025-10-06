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
 * #### Generated from [AnalysisProcessRoot.kt:5]
 */
class AnalysisProcessRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            AnalysisProcessRoot.register(serializers)
            AnalysisProcessModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -8924074368138535117L
        
    }
    override val serializersOwner: ISerializersOwner get() = AnalysisProcessRoot
    override val serializationHash: Long get() = AnalysisProcessRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AnalysisProcessRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AnalysisProcessRoot   {
        return AnalysisProcessRoot(
        )
    }
    //contexts
}
