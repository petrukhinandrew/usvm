package org.usvm.samples.renderer;

import sun.misc.Unsafe;
import org.usvm.samples.renderer.SimpleMethod;
import org.usvm.samples.renderer.AnotherInstanceKind;

public class SampleGenClass {

    public void sample() throws InstantiationException {
        ((org.usvm.samples.renderer.SimpleMethod) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.SimpleMethod.class)).y = -225;
        ((org.usvm.samples.renderer.SimpleMethod) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.SimpleMethod.class)).y = -225;
        ((org.usvm.samples.renderer.AnotherInstanceKind) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.AnotherInstanceKind.class)).x = 234;
        ((org.usvm.samples.renderer.SimpleMethod) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.SimpleMethod.class)).manyInstanceAccess(((org.usvm.samples.renderer.SimpleMethod) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.SimpleMethod.class)), ((org.usvm.samples.renderer.AnotherInstanceKind) Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.AnotherInstanceKind.class)));
    }
}