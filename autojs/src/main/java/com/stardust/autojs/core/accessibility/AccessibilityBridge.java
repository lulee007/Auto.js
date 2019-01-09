package com.stardust.autojs.core.accessibility;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.stardust.app.AppOpsKt;
import com.stardust.autojs.runtime.accessibility.AccessibilityConfig;
import com.stardust.util.IntentUtil;
import com.stardust.util.UiHandler;
import com.stardust.view.accessibility.AccessibilityInfoProvider;
import com.stardust.view.accessibility.AccessibilityNotificationObserver;
import com.stardust.view.accessibility.AccessibilityService;


/**
 * Created by Stardust on 2017/4/2.
 */

public abstract class AccessibilityBridge {

    public interface WindowFilter {
        boolean filter(AccessibilityWindowInfo info);
    }

    public static final int MODE_NORMAL = 0;
    public static final int MODE_FAST = 1;

    public static final int FLAG_FIND_ON_UI_THREAD = 1;
    public static final int FLAG_USE_USAGE_STATS = 2;

    private int mMode = MODE_NORMAL;
    private int mFlags = 0;
    private final AccessibilityConfig mConfig;
    private WindowFilter mWindowFilter;
    private final UiHandler mUiHandler;
    private final Context mContext;

    public AccessibilityBridge(Context context, AccessibilityConfig config, UiHandler uiHandler) {
        mConfig = config;
        mUiHandler = uiHandler;
        mConfig.seal();
        mContext = context;
    }

    public abstract void ensureServiceEnabled();

    public abstract void waitForServiceEnabled();

    public void post(Runnable r) {
        mUiHandler.post(r);
    }

    @Nullable
    public abstract AccessibilityService getService();

    @Nullable
    public AccessibilityNodeInfo getRootInCurrentWindow() {
        AccessibilityService service = getService();
        if (service == null)
            return null;
        if (mWindowFilter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityWindowInfo activeWindow = null;
            for (AccessibilityWindowInfo window : service.getWindows()) {
                if (mWindowFilter.filter(window)) {
                    return window.getRoot();
                }
                if (window.isActive()) {
                    activeWindow = window;
                }
            }
            if (activeWindow != null) {
                return activeWindow.getRoot();
            }
        }
        if ((mMode & MODE_FAST) != 0) {
            return service.fastRootInActiveWindow();
        }
        return service.getRootInActiveWindow();
    }

    public void setWindowFilter(WindowFilter windowFilter) {
        mWindowFilter = windowFilter;
    }

    public abstract AccessibilityInfoProvider getInfoProvider();

    public void setMode(int mode) {
        mMode = mode;
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int flags) {
        mFlags = flags;
        if ((mFlags & FLAG_USE_USAGE_STATS) != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!AppOpsKt.isOpPermissionGranted(mContext, AppOpsManager.OPSTR_GET_USAGE_STATS)) {
                IntentUtil.requestAppUsagePermission(mContext);
                throw new SecurityException("没有\"查看使用情况\"权限");
            }
        }
        getInfoProvider().setUseUsageStats((mFlags & FLAG_USE_USAGE_STATS) != 0);
    }

    @NonNull
    public abstract AccessibilityNotificationObserver getNotificationObserver();

    public AccessibilityConfig getConfig() {
        return mConfig;
    }
}
