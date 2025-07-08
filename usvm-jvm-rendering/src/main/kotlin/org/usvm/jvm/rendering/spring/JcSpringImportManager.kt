package org.usvm.jvm.rendering.spring

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.SimpleName
import org.usvm.jvm.rendering.ReflectionUtilsInlineStrategy
import org.usvm.jvm.rendering.unsafeRenderer.JcUnsafeImportManager
import org.usvm.jvm.rendering.unsafeRenderer.ReflectionUtilName

class JcSpringImportManager(
    cu: CompilationUnit? = null,
    utilsInlineStrategy: ReflectionUtilsInlineStrategy = ReflectionUtilsInlineStrategy.NoInline
) : JcUnsafeImportManager(cu, utilsInlineStrategy) {

    var springUtilsImported = false
        private set

    val springUtilsName: SimpleName by lazy {
        springUtilsImported = true
        if (add(ReflectionUtilName.SPRING))
            SimpleName(ReflectionUtilName.SPRING_SIMPLE)
        else SimpleName(ReflectionUtilName.SPRING)
    }
}
