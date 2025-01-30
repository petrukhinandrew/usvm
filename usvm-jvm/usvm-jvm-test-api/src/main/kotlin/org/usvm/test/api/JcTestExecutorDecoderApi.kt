package org.usvm.test.api

import org.jacodb.api.jvm.*
import org.jacodb.api.jvm.ext.*
import org.usvm.api.decoder.DecoderApi
import org.usvm.machine.JcContext

class JcTestExecutorDecoderApi(
    private val ctx: JcContext
) : DecoderApi<UTestExpression> {
    private val instructions = mutableListOf<UTestInst>()

    fun initializerInstructions(): List<UTestInst> = instructions

    override fun setField(field: JcField, instance: UTestExpression, value: UTestExpression) {
        instructions += if (field.isStatic) {
            UTestSetStaticFieldStatement(field, value)
        } else {
            UTestSetFieldStatement(instance, field, value)
        }
    }

    override fun getField(field: JcField, instance: UTestExpression): UTestExpression =
        if (field.isStatic) {
            UTestGetStaticFieldExpression(field)
        } else {
            UTestGetFieldExpression(instance, field)
        }

    override fun invokeMethod(method: JcMethod, args: List<UTestExpression>): UTestExpression =
        when {
            method.isConstructor -> UTestConstructorCall(method, args)
            method.isStatic -> UTestStaticMethodCall(method, args)
            else -> UTestMethodCall(args.first(), method, args.drop(1))
        }.also {
            instructions += it
        }

    override fun createBoolConst(value: Boolean): UTestExpression =
        UTestBooleanExpression(value, ctx.cp.boolean)

    override fun createByteConst(value: Byte): UTestExpression =
        UTestByteExpression(value, ctx.cp.byte)

    override fun createShortConst(value: Short): UTestExpression =
        UTestShortExpression(value, ctx.cp.short)

    override fun createIntConst(value: Int): UTestExpression =
        UTestIntExpression(value, ctx.cp.int)

    override fun createLongConst(value: Long): UTestExpression =
        UTestLongExpression(value, ctx.cp.long)

    override fun createFloatConst(value: Float): UTestExpression =
        UTestFloatExpression(value, ctx.cp.float)

    override fun createDoubleConst(value: Double): UTestExpression =
        UTestDoubleExpression(value, ctx.cp.double)

    override fun createCharConst(value: Char): UTestExpression =
        UTestCharExpression(value, ctx.cp.char)

    override fun createStringConst(value: String): UTestExpression =
        UTestStringExpression(value, ctx.cp.stringType())

    override fun createClassConst(type: JcType): UTestExpression =
        UTestClassExpression(type)

    override fun createNullConst(type: JcType): UTestExpression =
        UTestNullExpression(type)

    override fun setArrayIndex(array: UTestExpression, index: UTestExpression, value: UTestExpression) {
        instructions += UTestArraySetStatement(array, index, value)
    }

    override fun getArrayIndex(array: UTestExpression, index: UTestExpression): UTestExpression =
        UTestArrayGetExpression(array, index)

    override fun getArrayLength(array: UTestExpression): UTestExpression =
        UTestArrayLengthExpression(array)

    override fun createArray(elementType: JcType, size: UTestExpression): UTestExpression =
        UTestCreateArrayExpression(elementType, size)

    override fun castClass(type: JcClassOrInterface, obj: UTestExpression): UTestExpression =
        UTestCastExpression(obj, type.toType())
}

private fun JcClasspath.stringType(): JcType {
    return findClassOrNull("java.lang.String")!!.toType()
}