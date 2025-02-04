package org.usvm.samples.renderer;

class SimpleMethod {

    private int y = 2;

    public int yExplicitGet() {
        return y;
    }

    public void yExplicitSet(int y) {
        this.y = y;
    }

    public int simpleLol(int x) {
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
        return 10;
    }
}

