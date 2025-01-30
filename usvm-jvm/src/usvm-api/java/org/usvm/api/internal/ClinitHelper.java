package org.usvm.api.internal;

import java.util.function.Function;

// TODO refactor usage
public class ClinitHelper {
    public static Function<String, Void> afterClinitAction;

    public static void afterClinit(String className) {
        afterClinitAction.apply(className);
    }
}
