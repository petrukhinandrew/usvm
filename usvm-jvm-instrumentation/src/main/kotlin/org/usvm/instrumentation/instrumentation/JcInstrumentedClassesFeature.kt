package org.usvm.instrumentation.instrumentation

import org.jacodb.api.jvm.JcClasspathFeature

data class JcInstrumentedClassesFeature(val classNames: List<String>): JcClasspathFeature