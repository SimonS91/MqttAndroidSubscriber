package sample.mqtt.simone.com.applicationsubmqtt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Simone Sorge on 25/11/2015.
 */
public class PushMessage extends Service implements MqttCallback
{
    private String TAG = "PushMsgService";

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    private MqttClient mqttClient;
    private static String Client_ID = "MqttExample";
    private static final String broker = "tcp://10.0.2.2:1883";
    public static boolean CLEAN_SESSION = Boolean.TRUE;
    private String SUBSCRIBE_MESSAGE = "android_message";
    private static final String ACTION_START = Client_ID + ".START";
    private static final String ACTION_STOP = Client_ID + ".STOP";

    public static void actionStart(Context ctx)
    {
        Intent i = new Intent(ctx,PushMessage.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    public static void actionStop(Context ctx)
    {
        Intent i = new Intent(ctx,PushMessage.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }
    private static List<onPushMessageListener> onPushMesageListeners = new ArrayList<onPushMessageListener>();

    public final static List<onPushMessageListener> getOnPushMesageListeners()
    {
        return Collections.unmodifiableList(onPushMesageListeners);
    }

    private final static void notifyOnPushMessage(String msg)
    {
        List<onPushMessageListener> mLm = getOnPushMesageListeners();
        for (onPushMessageListener l:mLm)
        {
            try {
                if(l!=null)
                {
                    l.onNewPushMessage(msg);
                }
            }catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
    public final static void addPushMessageListeners(onPushMessageListener mOnPushMesageListeners)
    {
        PushMessage.onPushMesageListeners.add(mOnPushMesageListeners);
    }

    public final static void removePushMessageListeners(onPushMessageListener mOnPushMessageListeners)
    {
        PushMessage.onPushMesageListeners.remove(mOnPushMessageListeners);
    }

    private NotificationManager notificationManager;
    @Override
    public void onCreate()
    {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Client_ID = MqttApplication.getInstance().getDeviceId();
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId)
    {
        super.onStartCommand(intent, flags, startId);

        if(intent.getAction().equals(ACTION_STOP)== true)
        {
            stop();
            stopSelf();
        }else if (intent.getAction().equals(ACTION_START)==true)
        {
            onStartConnectioned();
        }
        onStartConnectioned();
        return START_STICKY;
    }
    private void onStartConnectioned()
    {
        Log.d(TAG, "Sono dentro ONSTARTCONNECTION!!!!!!!!!!!!");
        MqttConnectOptions conOpitions = new MqttConnectOptions();
        conOpitions.setCleanSession(CLEAN_SESSION);
        //conOpitions.setKeepAliveInterval(30);
        try
        {
            mqttClient = new MqttClient(broker,Client_ID,new MemoryPersistence());
            mqttClient.setCallback(this);
            mqttClient.connect(conOpitions);
            Log.d(TAG,"Connessione avvenuta");
            String myTopic = "pahodemo/test";
            MqttTopic topic = mqttClient.getTopic(myTopic);
            int subQoS = 1;
            mqttClient.subscribe(myTopic, subQoS);
            MqttApplication.getInstance().setMqttStart(Boolean.TRUE);
            Toast.makeText(this, "PushMsgService start... SUCC", Toast.LENGTH_LONG).show();
        } catch (MqttException ex)
        {
            ex.printStackTrace();
            MqttApplication.getInstance().setMqttStart(Boolean.FALSE);
        }
        try {
            Thread.sleep(5000);
            mqttClient.disconnect();
            Log.d(TAG,"disconnesso!!!!!!!");
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    private void stop()
    {
        if(mqttClient!= null && mqttClient.isConnected())
        {
            try {
                mqttClient.disconnect();
                MqttApplication.getInstance().setMqttStart(Boolean.FALSE);
                Toast.makeText(this, "PushMsgService stop... SUCC", Toast.LENGTH_LONG).show();
            } catch (MqttException ex)
            {
                ex.printStackTrace();
            }
        }
    }
    private void showNotification(String text)
    {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification myNotication;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(""),
                0);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("MQTT");
        builder.setContentText(text);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.icon);
        builder.setOngoing(true);

        myNotication = builder.build();
        nm.notify(1,myNotication);
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    @Override
    public void connectionLost(Throwable arg0)
    {
        Log.d(TAG, "Connection Lost");
        Log.e(TAG, " ", arg0);
    }
    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0)
    {
        Log.d(TAG, "deliveryComplete()");
        try
        {
            MqttMessage msg = arg0.getMessage();
            if(msg!=null)
            {
                byte bytes[] = msg.getPayload();
                if(bytes != null)
                {
                    String message = new String(bytes);
                    Log.d(TAG,"deliveryMessage(): "+message);
                    showNotification(message);
                    notifyOnPushMessage(message);
                }
            }
        }catch (MqttException ex)
        {
            ex.printStackTrace();
        }
    }
    @Override
    public void messageArrived(String sub, MqttMessage arg1) throws Exception
    {
        Log.d(TAG,"messageArraived()"+sub);
        if(arg1!=null)
        {
            byte bytes[] = arg1.getPayload();
            if(bytes!=null)
            {
                String message = new String(bytes);
                Log.d(TAG,"deliveryComplete"+message);
                showNotification(message);
                notifyOnPushMessage(message);
            }
        }
    }
}
