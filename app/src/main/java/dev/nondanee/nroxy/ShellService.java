package dev.nondanee.nroxy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellService extends Service {
    private static final String TAG = Constant.DEBUG_TAG + ".ShellService";

    public Process shellProcess = null;
    public Thread stdoutThread = null;
    public Thread stderrThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_STRING, CHANNEL_NAME_STRING, NotificationManager.IMPORTANCE_HIGH);
//            notificationManager.createNotificationChannel(channel);
//            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
//            startForeground(1, notification);
//        }
    }

    private Thread outputReader(final InputStream input) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
//                        Log.d(TAG, "output: " + line);
                        sendMessage(line);
                    }
                }
                catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return thread;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(TAG, "onStartCommand() executed");

        if (intent == null) {
            return START_STICKY;
        }

        if (Constant.ACTION_START.equals(intent.getAction())) {
            start(
                intent.getStringExtra("command"),
                intent.getStringArrayExtra("env")
            );
        }
        if (Constant.ACTION_STOP.equals(intent.getAction())) {
            stop();
        }
        return START_STICKY;
    }

    public boolean running() {
        return shellProcess != null;
    }

    public static void start(Context context, String command, String[] env) {
        Intent intent = new Intent(context, ShellService.class);
        intent.setAction(Constant.ACTION_START);
        intent.putExtra("command", command);
        intent.putExtra("env", env);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ShellService.class);
        intent.setAction(Constant.ACTION_STOP);
        context.startService(intent);
    }

    private void start(String command, String[] env) {
        if (shellProcess != null) return;
        String packageFiles = getApplicationContext().getFilesDir().getAbsolutePath();
        String[] shell = {"/system/bin/sh", "-c", command};
        try{
            shellProcess = Runtime.getRuntime().exec(shell, env, new File(packageFiles));
            stdoutThread = outputReader(shellProcess.getInputStream());
            stderrThread = outputReader(shellProcess.getErrorStream());
        }
        catch (Exception e) {
            Log.d(TAG, "error: " + e.toString());
            sendMessage(e.toString());
        }
    }

    private void stop() {
        if (shellProcess != null) shellProcess.destroy();
        if (stdoutThread != null) stdoutThread.interrupt();
        if (stderrThread != null) stderrThread.interrupt();
        shellProcess = null;
        stdoutThread = null;
        stderrThread = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    private Messenger remoteMessager = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            remoteMessager = message.replyTo;
        }
    };
    public Messenger receiver = new Messenger(handler);
    private void sendMessage(String string) {
        Message message = new Message();
        message.obj = string;
        try {
            remoteMessager.send(message);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    class ServiceBinder extends Binder {
        public ShellService getService(){
            return ShellService.this;
        }
    }
}