package checks;

/** Resolves the running JDK rather than a PATH shim that may spawn another process. */
public final class RuntimeHome {
    public static void main(String[] args) { System.out.println(System.getProperty("java.home")); }
}
