package org.usvm.jvm.spring.utils

import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.ext.jvmName
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode

fun addSecurityConfigs(testClassNode: ClassNode, nonAbstractClasses: Sequence<JcClassOrInterface>) {
    val importAnnotationName = "org.springframework.context.annotation.Import".jvmName()
    val securityConfigs = allByAnnotation(
        nonAbstractClasses,
        "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity"
    )
    val importAnnotationNode = AnnotationNode(importAnnotationName)
    val securityConfigsAsm = securityConfigs.map { Type.getType(it.jvmDescriptor) }.toList()
    importAnnotationNode.values = listOf("value", securityConfigsAsm)
    testClassNode.visibleAnnotations.add(importAnnotationNode)
}

fun replaceTypeInClassNode(
    classNode: ClassNode,
    oldClassName: String,
    newClassName: String
) {
    check(!oldClassName.contains('/')) {
        "bad old class name $oldClassName"
    }

    check(!newClassName.contains('/')) {
        "bad new class name $newClassName"
    }

    val oldClassSlashName = oldClassName.replace(".", "/")
    val oldClassJvmName = "L$oldClassSlashName;"
    val newClassSlashName = newClassName.replace(".", "/")
    val newClassJvmName = "L$newClassSlashName;"

    // Transform all field descriptors and signatures
    classNode.fields?.forEach { field ->
        field.desc = field.desc.replace(oldClassJvmName, newClassJvmName)
        field.signature = field.signature?.replace(oldClassJvmName, newClassJvmName)
    }

    // Transform all methods
    classNode.methods?.forEach { method ->
        // Update method descriptor and signature
        method.desc = method.desc.replace(oldClassJvmName, newClassJvmName)
        method.signature = method.signature?.replace(oldClassJvmName, newClassJvmName)

        // Process all instructions in the method
        for (inst in method.instructions) {
            when (inst) {
                is TypeInsnNode -> {
                    // NEW, ANEWARRAY, CHECKCAST, INSTANCEOF
                    if (inst.desc == oldClassSlashName) {
                        inst.desc = newClassSlashName
                    }
                }
                is FieldInsnNode -> {
                    // GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC
                    if (inst.owner == oldClassSlashName) {
                        inst.owner = newClassSlashName
                    }
                    inst.desc = inst.desc.replace(oldClassJvmName, newClassJvmName)
                }
                is MethodInsnNode -> {
                    // INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE
                    if (inst.owner == oldClassSlashName) {
                        inst.owner = newClassSlashName
                    }
                    inst.desc = inst.desc.replace(oldClassJvmName, newClassJvmName)
                }
                is LdcInsnNode -> {
                    // LDC Class constants
                    if (inst.cst is Type) {
                        val type = inst.cst as Type
                        if (type.className == oldClassName) {
                            inst.cst = Type.getType(newClassJvmName)
                        }
                    }
                }
                is MultiANewArrayInsnNode -> {
                    // MULTIANEWARRAY
                    inst.desc = inst.desc.replace(oldClassJvmName, newClassJvmName)
                }
            }
        }

        // Transform local variable types
        method.localVariables?.forEach { localVar ->
            localVar.desc = localVar.desc.replace(oldClassJvmName, newClassJvmName)
            localVar.signature = localVar.signature?.replace(oldClassJvmName, newClassJvmName)
        }

        // Transform exception types in try-catch blocks
        method.tryCatchBlocks?.forEach { tryCatch ->
            if (tryCatch.type == oldClassSlashName) {
                tryCatch.type = newClassSlashName
            }
        }
    }

    // Transform class signature (for generics)
    classNode.signature = classNode.signature?.replace(oldClassJvmName, newClassJvmName)
}