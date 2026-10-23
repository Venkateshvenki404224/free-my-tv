package io.venkitv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A message over whatever is on screen, sent from a computer:
 * adb shell am broadcast -a io.venkitv.MESSAGE -n io.venkitv/.MessageReceiver --es title "Hi" --es text "Dinner's ready" --ei seconds 8
 * Only the shell can send it (the receiver asks for DUMP). It draws a banner at the top with SYSTEM_ALERT_WINDOW
 * (adb shell appops set io.venkitv SYSTEM_ALERT_WINDOW allow), or falls back to a toast.
 */
public final class MessageReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        int seconds = Math.max(2, Math.min(9, intent.getIntExtra("seconds", 8)));   // a broadcast may stay open ~10 s
        if (text == null || text.trim().isEmpty()) return;
        Context app = c.getApplicationContext();
        PendingResult pending = goAsync();
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                show(app, title, text, seconds, pending);
            } catch (RuntimeException e) {
                AppLog.w("message: " + e.getMessage());
                pending.finish();
            }
        });
    }

    private static void show(Context c, String title, String text, int seconds, PendingResult pending) {
        AppLog.w("message: " + (title != null ? title + ": " : "") + text);
        if (!Settings.canDrawOverlays(c)) {
            Toast.makeText(c, title != null ? title + "\n" + text : text, Toast.LENGTH_LONG).show();
            pending.finish();
            return;
        }
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        float k = c.getResources().getDisplayMetrics().widthPixels / 1920f;
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xF21F5FBF, 0xF20F2A66});
        bg.setCornerRadius(30 * k);
        bg.setStroke(Math.round(2 * k), 0x40FFFFFF);
        card.setBackground(bg);
        card.setPadding(Math.round(44 * k), Math.round(30 * k), Math.round(44 * k), Math.round(32 * k));
        if (title != null && !title.trim().isEmpty()) card.addView(label(c, title, 40 * k, Typeface.BOLD, 0xFFFFFFFF));
        card.addView(label(c, text, 34 * k, Typeface.NORMAL, 0xFFDCE8FF));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                Math.round(1100 * k), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Math.round(60 * k);
        lp.windowAnimations = android.R.style.Animation_Toast;
        wm.addView(card, lp);
        card.postDelayed(() -> {
            try {
                wm.removeView(card);
            } catch (RuntimeException ignored) {
            }
            pending.finish();
        }, seconds * 1000L);
    }

    private static View label(Context c, String s, float px, int style, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
        t.setTextColor(color);
        t.setLineSpacing(0, 1.1f);
        return t;
    }
}
