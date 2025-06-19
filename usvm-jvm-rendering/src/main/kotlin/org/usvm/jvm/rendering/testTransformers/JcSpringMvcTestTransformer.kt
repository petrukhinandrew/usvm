package org.usvm.jvm.rendering.testTransformers

import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.JcClassType
import org.jacodb.api.jvm.JcMethod
import org.usvm.test.api.UTestCall
import org.usvm.test.api.UTestClassExpression
import org.usvm.test.api.UTestConstructorCall
import org.usvm.test.api.UTestMethodCall
import org.usvm.test.api.UTestStaticMethodCall

class JcSpringMvcTestTransformer: JcTestTransformer() {

    private var mvcTestClass: JcClassOrInterface? = null

    private val testContextManagerName = "org.springframework.test.context.TestContextManager"

    private val JcMethod.isIgnoreResultMethod: Boolean get() {
        return name == "ignoreResult" && enclosingClass.name == mvcTestClass?.name
    }

    private val JcMethod.isPrepareInstanceMethod: Boolean get() {
        return name == "prepareTestInstance" && enclosingClass.name == testContextManagerName
    }

    private val JcMethod.isBeforeTestClassMethod: Boolean get() {
        return name == "beforeTestClass" && enclosingClass.name == testContextManagerName
    }

    private val JcMethod.isAfterTestMethod: Boolean get() {
        return name == "afterTestMethod" && enclosingClass.name == testContextManagerName && parameters.size == 3
    }

    private val JcMethod.isBeforeTestMethod: Boolean get() {
        return name == "beforeTestMethod" && enclosingClass.name == testContextManagerName && parameters.size == 2
    }

    override fun transform(call: UTestMethodCall): UTestCall? {
        val method = call.method

        return when {
            method.isPrepareInstanceMethod -> {
                val instance = call.instance
                check(instance is UTestConstructorCall && instance.method.enclosingClass.name == testContextManagerName) {
                    "isPrepareInstanceMethod instance fail"
                }

                val arg = instance.args.singleOrNull() as? UTestClassExpression
                check(arg != null) {
                    "isPrepareInstanceMethod arg fail"
                }

                mvcTestClass = (arg.type as JcClassType).jcClass

                null
            }

            method.isBeforeTestClassMethod || method.isAfterTestMethod || method.isBeforeTestMethod -> {
                null
            }

            else -> super.transform(call)
        }
    }

    override fun transform(call: UTestStaticMethodCall): UTestCall? {
        val method = call.method

        if (method.isIgnoreResultMethod)
            return super.transform(call.args.single() as UTestMethodCall)

        return super.transform(call)
    }

    val testClass get() = mvcTestClass ?: error("testClass not found")
}
