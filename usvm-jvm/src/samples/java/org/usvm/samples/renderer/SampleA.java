package org.usvm.samples.renderer;

public class SampleA {
    public void SomeMethod(int a, double b) {
        if (b * a > 0) {
            var x = b - a;
        } else {
            var y = b + a;
        }
    }

    public static int add2(int x) {
        return x + 2;
    }

    public Integer genericUsage(Wg<Integer> wg1, Wg<Integer> wg2) {
        wg1.value = 1;
        wg2.value = 2;
        return (wg1.value + wg2.value);
    }
}

