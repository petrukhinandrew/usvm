package org.usvm.samples.renderer;

import java.util.*;
import java.nio.BufferOverflowException;

import org.usvm.samples.*;

public class SimpleInstance {
    public void nonStatic() {
        System.out.println(org.usvm.samples.renderer.StaticInstance.str + AnotherStaticInstance.intVar);
    }

    public static int staticMethod() {
        return StaticInstance.intVar * 2;
    }
}
