package com.sinhvien.orderdrinkapp.Utils;
 
import android.content.Context;
import android.util.Log;
 
/**
 * Legacy Sample Data Initializer.
 * DISABLED as the application has migrated to a 100% Cloud-Native architecture.
 */
public class SampleDataInitializer {
 
    private static final String TAG = "DEBUG_DATA";
 
    public static void init(Context context) {
        Log.d(TAG, "SampleDataInitializer.init() called but is DISABLED (Cloud-Native Mode active).");
        // All SQLite operations have been removed to standardize on Cloud API.
    }
}
