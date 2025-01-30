package org.usvm

import bench.analyzeBench
import bench.loadKlawBench
import bench.logTime
import org.jacodb.api.jvm.JcClasspath
import org.usvm.machine.JcMachine

fun main() {
//    val benchCp = logTime("Init jacodb") {
//        loadKlawBench()
//    }
//
//    logTime("Analysis ALL") {
//        benchCp.use { analyzeBench(it) }
//    }
    JcMachine(JcClasspath())
}