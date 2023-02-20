package com.dns.one.unblock.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.NotificationCompat;

import com.dns.one.unblock.MainActivity;
import com.dns.one.unblock.R;
import com.dns.one.unblock.utils.AbstractDNSServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DnsVpnService extends VpnService implements Runnable {
    public static final String ACTION_ACTIVATE = "com.dns.one.unblock.services.DnsVpnService.ACTION_ACTIVATE";
    public static final String ACTION_DEACTIVATE = "com.dns.one.unblock.services.DnsVpnService.ACTION_DEACTIVATE";

    private static final int NOTIFICATION_ACTIVATED = 0;
    private static final String TAG = DnsVpnService.class.getSimpleName();

    private List<Pair<String, Integer>> resolvedDNS;

    public static AbstractDNSServer primaryServer;

    private NotificationCompat.Builder notification = null;
    private boolean running = false;
    private long lastUpdate = 0;

    private Thread mThread = null;

    public HashMap<String, Pair<String, Integer>> dnsServers;
    private static boolean activated = false;

    private ParcelFileDescriptor descriptor;


    public static boolean isActivated() {
        return activated;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private List<Pair<String, Integer>> getResolvedDNS(AbstractDNSServer dnsServer) {
        List<Pair<String, Integer>> resolvedDNSServers = new ArrayList<>();
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(dnsServer.getAddress());
            for (InetAddress address : addresses) {
                if (checkDNSServer(address, dnsServer.getPort()))
                    resolvedDNSServers.add(new Pair<>(address.getHostAddress(), dnsServer.getPort()));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return resolvedDNSServers;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            switch (intent.getAction()){
                case ACTION_ACTIVATE:
                    activated = true;
                    NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, createNotificationChannel(false));
                    Intent nIntent = new Intent(this, MainActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, nIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setWhen(0)
                            .setContentTitle("VPN Actived")
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                            .setSmallIcon(R.drawable.ic_on_off_button)
                            .setColor(getResources().getColor(R.color.primary)) //backward compatibility
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setTicker("VPN Actived")
                            .setContentIntent(pIntent);
//                            .addAction(R.drawable.ic_on_off_button, "STOP",
//                                    PendingIntent.getBroadcast(this, 0,
//                                            new Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_DEACTIVATE_CLICK_ACTION), 0))
//                            .addAction(R.drawable.ic_settings, getResources().getString(R.string.action_settings),
//                                    PendingIntent.getBroadcast(this, 0,
//                                            new Intent(StatusBarBroadcastReceiver.STATUS_BAR_BTN_SETTINGS_CLICK_ACTION), 0));

                    Notification notification = builder.build();

                    manager.notify(NOTIFICATION_ACTIVATED, notification);

                    this.notification = builder;
                    if (this.mThread == null) {
                        this.mThread = new Thread(this, "VPNService");
                        this.running = true;
                        this.mThread.start();
                    }
//                    if (MainActivity.getInstance() != null) {
//                        MainActivity.getInstance().startActivity(new Intent(getApplicationContext(), MainActivity.class)
//                                .putExtra(MainActivity.LAUNCH_ACTION, MainActivity.LAUNCH_ACTION_SERVICE_DONE));
//                    }
                    return START_STICKY;
                case ACTION_DEACTIVATE:
                    stopThread();
                    return START_NOT_STICKY;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void run() {

    }

    private void stopThread() {
        Log.d(TAG, "stopThread");
        activated = false;
        boolean shouldRefresh = false;
        try {
            if (this.descriptor != null) {
                this.descriptor.close();
                this.descriptor = null;
            }
            if (mThread != null) {
                running = false;
                shouldRefresh = true;
//                if (provider != null) {
////                    provider.shutdown();
//                    mThread.interrupt();
////                    provider.stop();
//                } else {
                    mThread.interrupt();
//                }
                mThread = null;
            }
            if (notification != null) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ACTIVATED);
                notification = null;
            }
            dnsServers = null;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        stopSelf();

        if (shouldRefresh) {
            Log.d(TAG, "shecan service has stopped");
        }

    }

    public String createNotificationChannel(boolean allowHiding){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel("defaultchannel", "TEST NOTIFICATION", NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setDescription("TEST NOTIFICATION");
                notificationManager.createNotificationChannel(channel);
                return "defaultchannel";
        }else{
            return "defaultchannel";
        }
    }
}
