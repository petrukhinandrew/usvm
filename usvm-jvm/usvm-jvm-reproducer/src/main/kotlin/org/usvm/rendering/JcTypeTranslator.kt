package org.usvm.org.usvm.rendering

import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.PrimitiveType.Primitive
import com.github.javaparser.ast.type.Type
import org.jacodb.api.jvm.JcArrayType
import org.jacodb.api.jvm.JcClassType
import org.jacodb.api.jvm.JcPrimitiveType
import org.jacodb.api.jvm.JcType
import org.usvm.util.name

class JcTypeTranslator(private val importManager: JcImportManager) {

    fun typeReprOf(type: JcType): Type = when (type) {
        is JcPrimitiveType -> PrimitiveType(Primitive.byTypeName(type.typeName).get())
        is JcArrayType -> ArrayType(typeReprOf(type.elementType))
        is JcClassType -> typeReprOf(type)
        else -> error("throw IllegalStateException()")
    }

    fun typeReprOf(type: JcClassType): ClassOrInterfaceType {
        importManager.tryAdd(type.name, type.jcClass.simpleName)
        return ClassOrInterfaceType(
            type.outerType?.let { typeReprOf(it) }, type.name
        )
    }
}

