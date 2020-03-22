package dev.nondanee.nroxy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class TunnelService extends VpnService {
    private static final String TAG = Constant.DEBUG_TAG + ".TunnelService";

    private TunnelService.Builder builder = null;
    private ParcelFileDescriptor vpn = null;

    static {
        System.loadLibrary("tun2http");
    }

    private native void jni_init();
    private native void jni_start(int tun, boolean fwd53, int rcode, String proxyIp, int proxyPort);
    private native void jni_stop(int tun);
    private native int jni_get_mtu();
    private native void jni_done();

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public class ServiceBinder extends Binder {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            // see Implementation of android.net.VpnService.Callback.onTransact()
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        public TunnelService getService() {
            return TunnelService.this;
        }
    }

    private Builder initBuilder(boolean type, String[] applications) {
        // Build VPN service
        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));

        // VPN address
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0.0.0.0", 0);

        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        // MTU
        int mtu = jni_get_mtu();
        builder.setMtu(mtu);

        // Add list of allowed applications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (String application: applications){
                try {
                    if (type) {
                        builder.addAllowedApplication(application);
                    }
                    else {
                        builder.addDisallowedApplication(application);
                    }
                }
                catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return builder;
    }

    private ParcelFileDescriptor startVPN(Builder builder) throws SecurityException {
        try {
            return builder.establish();
        }
        catch (SecurityException ex) {
            throw ex;
        }
        catch (Throwable e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(e));
            return null;
        }
    }

    private void stopVPN(ParcelFileDescriptor pfd) {
        Log.i(TAG, "Stopping");
        try {
            pfd.close();
        }
        catch (IOException e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(e));
        }
    }


    private void startNative(ParcelFileDescriptor vpn, String proxyHost, int proxyPort) {
        jni_start(vpn.getFd(), false, 3, proxyHost, proxyPort);
    }

    private void stopNative(ParcelFileDescriptor vpn) {
        try {
            jni_stop(vpn.getFd());
        }
        catch (Throwable e) {
            // File descriptor might be closed
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(e));
            jni_stop(-1);
        }
    }

    // Called from native code
    private void nativeExit(String reason) {
        Log.w(TAG, "Native exit reason: " + reason);
    }

    // Called from native code
    private void nativeError(int error, String message) {
        Log.w(TAG, "Native error " + error + ": " + message);
    }

    private boolean isSupported(int protocol) {
        return (protocol == 1 /* ICMPv4 */ ||
                protocol == 59 /* ICMPv6 */ ||
                protocol == 6 /* TCP */ ||
                protocol == 17 /* UDP */);
    }

    private void start(String host, int port, boolean type, String[] applications) {
        if (vpn != null) return;
        builder = initBuilder(type, applications);
        vpn = startVPN(builder);
        if (vpn == null) throw new IllegalStateException("start failed");
        startNative(vpn, host, port);
    }

    private void stop() {
        if (vpn == null) return;
        try {
            stopNative(vpn);
            stopVPN(vpn);
            builder = null;
            vpn = null;
        }
        catch (Throwable e) {
            Log.e(TAG, e.toString() + "\n" + Log.getStackTraceString(e));
        }
        stopForeground(true);
    }

    public static void start(Context context, String host, int port, boolean type, String[] applications) {
        Intent intent = new Intent(context, TunnelService.class);
        intent.setAction(Constant.ACTION_START);
        intent.putExtra("host", host);
        intent.putExtra("port", port);
        intent.putExtra("type", type);
        intent.putExtra("applications", applications);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, TunnelService.class);
        intent.setAction(Constant.ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        // Native init
        jni_init();
        super.onCreate();
    }

    public boolean running() {
        return vpn != null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle service restart
        if (intent == null) {
            return START_STICKY;
        }

        if (Constant.ACTION_START.equals(intent.getAction())) {
            start(
                intent.getStringExtra("host"),
                intent.getIntExtra("port", 0),
                intent.getBooleanExtra("type", false),
                intent.getStringArrayExtra("applications")
            );
        }
        if (Constant.ACTION_STOP.equals(intent.getAction())) {
            stop();
        }
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
//        Log.i(TAG, "Revoke");
        stop();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
//        Log.i(TAG, "Destroy");
        stop();
        jni_done();
        super.onDestroy();
    }

    private class Builder extends VpnService.Builder {
        private NetworkInfo networkInfo;
        private int mtu;
        private List<String> listAddress = new ArrayList<>();
        private List<String> listRoute = new ArrayList<>();
        private List<InetAddress> listDns = new ArrayList<>();

        private Builder() {
            super();
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }

        @Override
        public VpnService.Builder setMtu(int mtu) {
            this.mtu = mtu;
            super.setMtu(mtu);
            return this;
        }

        @Override
        public Builder addAddress(String address, int prefixLength) {
            listAddress.add(address + "/" + prefixLength);
            super.addAddress(address, prefixLength);
            return this;
        }

        @Override
        public Builder addRoute(String address, int prefixLength) {
            listRoute.add(address + "/" + prefixLength);
            super.addRoute(address, prefixLength);
            return this;
        }

        @Override
        public Builder addDnsServer(InetAddress address) {
            listDns.add(address);
            super.addDnsServer(address);
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            Builder other = (Builder) obj;

            if (other == null)
                return false;

            if (this.networkInfo == null || other.networkInfo == null ||
                    this.networkInfo.getType() != other.networkInfo.getType())
                return false;

            if (this.mtu != other.mtu)
                return false;

            if (this.listAddress.size() != other.listAddress.size())
                return false;

            if (this.listRoute.size() != other.listRoute.size())
                return false;

            if (this.listDns.size() != other.listDns.size())
                return false;

            for (String address : this.listAddress)
                if (!other.listAddress.contains(address))
                    return false;

            for (String route : this.listRoute)
                if (!other.listRoute.contains(route))
                    return false;

            for (InetAddress dns : this.listDns)
                if (!other.listDns.contains(dns))
                    return false;

            return true;
        }
    }
}