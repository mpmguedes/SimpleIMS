-repackageclasses
-allowaccessmodification
-overloadaggressively

-assumenosideeffects class java.util.Objects{
    ** requireNonNull(...);
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
