package com.cm.rxandroidble.util;

import android.text.TextUtils;
import android.util.Log;

public class L {
    private static final String ct = System.getProperty("line.separator");
    private static String TAG = "b2c";
    public static boolean LOG_ENABLED = true;
    private static boolean cu = false;
    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int ASSET = 6;
    public static final int JSON = 7;
    public static final int XML = 8;
    public static int LOG_LEVEL = 1;

    public L() {
    }

    public static void initialize(String var0, boolean var1) {
        TAG = var0;
        LOG_ENABLED = var1;
    }

    public static void v(String var0) {
        a(LOG_ENABLED, 1, TAG, var0);
    }

    public static void v(boolean var0, String var1) {
        a(var0, 1, TAG, var1);
    }

    public static void v(boolean var0, String var1, String var2) {
        a(var0, 1, var1, var2);
    }

    public static void d(String var0) {
        a(LOG_ENABLED, 2, TAG, var0);
    }

    public static void d(boolean var0, String var1) {
        a(var0, 2, TAG, var1);
    }

    public static void d(boolean var0, String var1, String var2) {
        a(var0, 2, var1, var2);
    }

    public static void i(String var0) {
        a(LOG_ENABLED, 3, TAG, var0);
    }

    public static void i(boolean var0, String var1) {
        a(var0, 3, TAG, var1);
    }

    public static void i(boolean var0, String var1, String var2) {
        a(var0, 3, var1, var2);
    }

    public static void w(String var0) {
        a(LOG_ENABLED, 4, TAG, var0);
    }

    public static void w(boolean var0, String var1) {
        a(var0, 4, TAG, var1);
    }

    public static void w(boolean var0, String var1, String var2) {
        a(var0, 4, var1, var2);
    }

    public static void e(String var0) {
        a(LOG_ENABLED, 5, TAG, var0);
    }

    public static void e(boolean var0, String var1) {
        a(var0, 5, TAG, var1);
    }

    public static void e(boolean var0, String var1, String var2) {
        a(var0, 5, var1, var2);
    }

    private static void a(boolean var0, int var1, String var2, Object var3) {
        if (var0) {
            String[] var4 = a(var2, var3);
            if (var4 != null && var4.length >= 3) {
                String var5 = var4[0];
                String var6 = var4[1];
                String var7 = var4[2];
                a(var1, var5, var7 + var6);
            } else {
                a(var1, var2, (String) var3);
            }
        }
    }

    private static void a(int var0, String var1, String var2) {
        int var3 = var0 | LOG_LEVEL;
        if ((var3 & 6) == 6) {
            Log.wtf(var1, var2);
        } else if ((var3 & 5) == 5) {
            Log.e(var1, var2);
        } else if ((var3 & 4) == 4) {
            Log.w(var1, var2);
        } else if ((var3 & 3) == 3) {
            Log.i(var1, var2);
        } else if ((var3 & 2) == 2) {
            Log.d(var1, var2);
        } else if ((var3 & 1) == 1) {
            Log.v(var1, var2);
        }

    }

    private static String[] a(String var0, Object... var1) {
        StackTraceElement[] var2 = Thread.currentThread().getStackTrace();
        if (var2 != null && var2.length >= 6) {
            byte var3 = 5;
            String var4 = var2[var3].getClassName();
            String[] var5 = var4.split("\\.");
            if (var5.length > 0) {
                var4 = var5[var5.length - 1] + ".kt";
            }

            if (var4.contains("$")) {
                var4 = var4.split("\\$")[0] + ".kt";
            }

            String var6 = var2[var3].getMethodName();
            int var7 = var2[var3].getLineNumber();
            if (var7 < 0) {
                var7 = 0;
            }

            String var8 = var6.substring(0, 1).toUpperCase() + var6.substring(1);
            String var9 = var0 == null ? var4 : var0;
            if (TextUtils.isEmpty(var9)) {
                var9 = TAG;
            }

            String var10 = var1 == null ? "" : a(var1);
            StringBuilder var11 = new StringBuilder();
            var11.append("[ (").append(var4).append(":").append(var7).append(")#").append(var8).append(" ]");
            String var12 = var11.toString();
            return new String[]{var9, var10, var12};
        } else {
            return null;
        }
    }

    private static String a(Object... var0) {
        if (var0.length > 1) {
            StringBuilder var4 = new StringBuilder();
            var4.append("\n");

            for (int var2 = 0; var2 < var0.length; ++var2) {
                Object var3 = var0[var2];
                if (var3 == null) {
                    var4.append("Param").append("[").append(var2).append("]").append(" = ").append("null").append("\n");
                } else {
                    var4.append("Param").append("[").append(var2).append("]").append(" = ").append(var3.toString()).append("\n");
                }
            }

            return var4.toString();
        } else {
            Object var1 = var0[0];
            return var1 == null ? "null" : var1.toString();
        }
    }
}
