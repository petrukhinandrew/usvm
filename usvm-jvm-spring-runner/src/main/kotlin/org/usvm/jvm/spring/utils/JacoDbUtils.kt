package org.usvm.jvm.spring.utils

import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.ext.hasAnnotation
import org.jacodb.api.jvm.ext.jvmName

val JcClassOrInterface.jvmDescriptor: String get() = name.jvmName()

fun allByAnnotation(allClasses: Sequence<JcClassOrInterface>, annotationName: String) =
    allClasses.filter { it.hasAnnotation(annotationName) }

