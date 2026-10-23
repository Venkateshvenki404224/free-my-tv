package io.venkitv;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * The WireGuard tunnel that reaches the TV from outside the home (Settings > WireGuard VPN). WireGuard's app does the
 * work: it is the TV's always-on VPN (so it starts at boot, with the tunnels that were on), and with "Allow remote
 * control apps" it takes SET_TUNNEL_UP/DOWN from apps granted its CONTROL_TUNNELS permission
 * (adb shell pm grant io.venkitv com.wireguard.android.permission.CONTROL_TUNNELS).
 */
final class Vpn {
    private Vpn() {}

    static final String PKG = "com.wireguard.android";
    /** The tunnel's name in WireGuard. */
    static final String TUNNEL = "TV";

    static boolean installed(Context c) {
        try {
            c.getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** A VPN is connected (WireGuard is the only one on the TV). */
    static boolean up(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        try {
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    static void set(Context c, boolean on) {
        c.sendBroadcast(new Intent(PKG + ".action.SET_TUNNEL_" + (on ? "UP" : "DOWN")).setPackage(PKG).putExtra("tunnel", TUNNEL));
        AppLog.w("wireguard: " + (on ? "up" : "down"));
    }
}
