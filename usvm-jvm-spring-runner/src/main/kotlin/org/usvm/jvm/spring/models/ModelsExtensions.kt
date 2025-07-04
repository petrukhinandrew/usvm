package org.usvm.jvm.spring.models

import org.usvm.jmv.spring.models.ErrorDescriptor

fun Throwable.toDescriptor(): ErrorDescriptor = ErrorDescriptor(message ?: "", stackTraceToString())