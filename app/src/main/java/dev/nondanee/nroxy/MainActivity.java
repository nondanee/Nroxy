package dev.nondanee.nroxy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = Constant.DEBUG_TAG + ".MainActivity";

    private TunnelService tunnelService;
    private ShellService shellService;
    private Handler handler;
    private SharedPreferences preferences;
    private Switch toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final TextContainer container = findViewById(R.id.container);
        final TextView output = findViewById(R.id.output);

        handler = new Handler(){
            @Override
            public void handleMessage(Message message) {
                boolean follow = container.getScrollY() + container.getHeight() + 120 > container.getChildAt(0).getHeight();
                output.append(message.obj.toString() + "\n");
                if (follow) {
                    container.post(new Runnable() {
                        @Override
                        public void run() {
                            container.smoothScrollTo(0, container.getChildAt(0).getHeight());
                        }
                    });
                }
            }
        };

//        checkOnStart();
    }
/*
    private void checkOnStart() {
        int version = preferences.getInt("version", 0);
        if (version < BuildConfig.VERSION_CODE) {
            setupNode();
            preferences.edit().putInt("version", BuildConfig.VERSION_CODE).apply();
        }
    }

    private void setupNode() {
        try{
            File bin = new File(getFilesDir().getAbsolutePath() + File.separator + "usr" + File.separator + "bin");
            if (!bin.exists()) bin.mkdirs();

            File node = new File(bin + File.separator + "node");
            Utility.copyFile(getAssets().open("node"), new FileOutputStream(node));
            Runtime.getRuntime().exec("chmod 700 " + node).waitFor();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    private boolean tunnelRunning() {
        return tunnelService != null && tunnelService.running();
    }

    private boolean shellRunning() {
        return shellService != null && shellService.running();
    }

    private void syncToggle() {
        if (toggle != null) toggle.setChecked(tunnelRunning() || shellRunning());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem menuItem = menu.findItem(R.id.action_switch);
        menuItem.setActionView(R.layout.switch_item);

        toggle = menuItem.getActionView().findViewById(R.id.main_switch);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean shell = preferences.getBoolean("shell", false);
                boolean vpn = preferences.getBoolean("vpn", false);

                if (isChecked) {
                    if (!tunnelRunning() && vpn) startVpn();
                    if (!shellRunning() && shell) startShell();
                    if (!shell && !vpn) toggle.setChecked(false);
                }
                else {
                    if (tunnelRunning()) stopVpn();
                    if (shellRunning()) stopShell();
                }
            }
        });

        syncToggle();
        return true;
    }

    private void stopShell() {
        ShellService.stop(this);
    }

    private void stopVpn() {
        TunnelService.stop(this);
    }

    private void startShell() {
        String bin = "libnode.so"; // binary file with fake name (only libxxx.so can be extracted)
        String path = preferences.getString("path", "");
        String argv = preferences.getString("argv", "");
        String command = bin + " " + path + " " + argv;
        List<String> env = new ArrayList(Arrays.asList(preferences.getString("env", "").split("\\s*,\\s*")));
        env.add("PATH=$PATH:" + getApplicationInfo().nativeLibraryDir);
        env.removeAll(Arrays.asList("", null));
//        Log.d(TAG, "startShell: command: " + command + " env: " + env + " env length: " + env.size());

        ShellService.start(this, command, env.toArray(new String[0]));
    }

    private void startVpn() {
        if (!prepareVpn()) return;;
        String host = "127.0.0.1";
        int port = -1;
        try {
            URL url = new URL(preferences.getString("proxy", null));
            host = url.getHost();
            port = url.getPort();
            if (port == -1) port = url.getDefaultPort();
        }
        catch (MalformedURLException e) {}

        boolean type = "allow".equals(preferences.getString("type", "allow"));
        String[] applications = preferences.getStringSet("applications", new HashSet<String>()).toArray(new String[0]);
//        Log.d(TAG, "startVpn: host: " + host + " port: " + port + " type: " + type + " :applications" + Arrays.toString(applications) + " length: " + applications.length);

        TunnelService.start(this, host, port, type, applications);
    }

    private boolean prepareVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, Constant.CODE_PREPARE_VPN);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == Constant.CODE_PREPARE_VPN && resultCode == Activity.RESULT_OK) startVpn();
    }

    private ServiceConnection shellConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ShellService.ServiceBinder serviceBinder = (ShellService.ServiceBinder) binder;
            shellService = serviceBinder.getService();
            syncToggle();

            Messenger messenger = new Messenger(handler);
            Message message = new Message();
            message.replyTo = messenger;
            try {
                shellService.receiver.send(message);
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            shellService = null;
            syncToggle();
            Log.d(TAG, "ShellService Disconnected");
        }
    };

    private ServiceConnection tunnelConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            TunnelService.ServiceBinder serviceBinder = (TunnelService.ServiceBinder) binder;
            tunnelService = serviceBinder.getService();
            syncToggle();
        }

        public void onServiceDisconnected(ComponentName className) {
            tunnelService = null;
            syncToggle();
            Log.d(TAG, "TunnelService Disconnected");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = null;
        intent = new Intent(this, TunnelService.class);
        bindService(intent, tunnelConnection, Context.BIND_AUTO_CREATE);

        intent = new Intent(this, ShellService.class);
        bindService(intent, shellConnection, Context.BIND_AUTO_CREATE);

        syncToggle();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(tunnelConnection);
        unbindService(shellConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.action_settings) startActivity(new Intent(this, PreferenceActivity.class));
        return false;
    }
}