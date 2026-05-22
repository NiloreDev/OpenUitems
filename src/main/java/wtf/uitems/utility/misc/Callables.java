package wtf.uitems.utility.misc;

// prot will replace calls to these methods with the method bodies
// all methods that call these methods should be natived
// do not use lambdas or create anonymous classes in these methods, they will break obfuscation
public final class Callables {

    private Callables() {
    }

    public static void segfault() {
    }

    public static void sendIntegrityAlert(final int marker) {
    }

    // packets: 1001 = 10_0_1 (packetId_0_location)
    // others:    11 = 1_1    (classSpecificId_location)
    public static void throwError(final int marker) {
        System.out.println("Blocked error report: " + marker);
    }
}
