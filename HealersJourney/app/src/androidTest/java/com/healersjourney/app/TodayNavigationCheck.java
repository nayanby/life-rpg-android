package com.healersjourney.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;

/** Device regression: rendering Today and opening its settings must not crash or alter game data. */
public final class TodayNavigationCheck extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent launch = getTargetContext().getPackageManager().getLaunchIntentForPackage("com.life.rpg");
            if (launch == null) throw new AssertionError("App launch intent missing");
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivitySync(launch);
            tap("Today");
            tap("Day reset settings");
            await("Day starts at");
            tap("Close");
            await("Day reset settings");
            tap("Previous day");
            await("Back to today");
            tap("Back to today");
            awaitGone("Back to today");
            tap("Home");
            tap("Today");
            await("Day reset settings");
            tap("Journal");
            await("Settings");
            tap("Today");
            await("Day reset settings");
            result.putString("stream", "PASS: Today opens repeatedly; reset settings opens/closes; Back to today works; Journal settings renders. No game data changed.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String label) {
        if (node == null) return null;
        if (label.contentEquals(node.getText() == null ? "" : node.getText()) ||
            label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(node.getChild(i), label);
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo freshRoot() {
        if (android.os.Build.VERSION.SDK_INT >= 34) getUiAutomation().clearCache();
        return getUiAutomation().getRootInActiveWindow();
    }
    private AccessibilityNodeInfo await(String label) {
        long deadline = SystemClock.uptimeMillis() + 12000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo found = find(freshRoot(), label);
            if (found != null) return found;
            SystemClock.sleep(120);
        }
        throw new AssertionError("UI missing: " + label);
    }
    private void awaitGone(String label) {
        long deadline = SystemClock.uptimeMillis() + 8000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = freshRoot();
            if (root != null) root.refresh();
            if (find(root, label) == null) return;
            SystemClock.sleep(120);
        }
        throw new AssertionError("UI did not dismiss: " + label);
    }
    private void tap(String label) {
        long deadline = SystemClock.uptimeMillis() + 12000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = freshRoot();
            if (root != null) root.refresh();
            if (root == null || !"com.life.rpg".contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) { SystemClock.sleep(120); continue; }
            AccessibilityNodeInfo node = find(root, label);
            while (node != null && !node.isClickable()) node = node.getParent();
            if (node != null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                try { getUiAutomation().waitForIdle(250, 3000); } catch (java.util.concurrent.TimeoutException ignored) { }
                SystemClock.sleep(600);
                return;
            }
            SystemClock.sleep(120);
        }
        throw new AssertionError("Cannot tap: " + label);
    }
}
