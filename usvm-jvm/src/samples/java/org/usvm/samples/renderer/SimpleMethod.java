package org.usvm.samples.renderer;

import sun.misc.Unsafe;

class AnotherInstanceKind {
    public int x = 2;
}

class SimpleMethod {

    public int y = 2;

    public int yExplicitGet() {
        return y;
    }

    public void yExplicitSet(int y) {
        this.y = y;
    }

    public int simpleLol(int x) {
//        ((AnotherInstanceKind) Unsafe.getUnsafe().allocateInstance(AnotherInstanceKind.class)).x = -131;

        final int a = 1;
        if (y > 0)
            return a + y * x;
        else if (y == 0) throw new ArithmeticException();
        else
            return a;
    }

    public void throwsIllegalState() {
        throw new IllegalStateException();
    }

    public int const10() {
        var sm = new SimpleMethod();
        if (sm.y > 0) return 1;
        return 10;
    }

    public void lol() {
        SimpleMethod s1 = new SimpleMethod();
        s1.y = 0;
        SimpleMethod s2 = new SimpleMethod();
        s2.y = 0;

    }

    public void manyInstanceAccess(SimpleMethod s, AnotherInstanceKind k) {
        if (s.y + k.x < 10) return;
        k.x += 1;
    }

    public int manyArg(SimpleMethod s1, SimpleMethod s2) {
        if (s1.y + s2.y >= 0) return 1;
        return 2;
    }

    public void kek() throws InstantiationException {
        sun.misc.Unsafe.getUnsafe().allocateInstance(org.usvm.samples.renderer.SimpleMethod.class);
    }
}