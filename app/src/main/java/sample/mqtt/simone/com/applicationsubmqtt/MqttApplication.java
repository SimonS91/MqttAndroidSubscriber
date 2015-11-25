package sample.mqtt.simone.com.applicationsubmqtt;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

/**
 * Created by Simone Sorge on 25/11/2015.
 */
public class MqttApplication extends Application
{
        private static MqttApplication mInstance = null;
        private String deviceId;

        public final String getDeviceId() {
            return deviceId;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            mInstance = this;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(this);
            deviceId = sp.getString("deviceId", "");


                deviceId = "1234";
                sp.edit().putString("deviceId", deviceId).commit();


                deviceId = "1234";
                sp.edit().putString("deviceId", deviceId).commit();
            }


        public static MqttApplication getInstance() {
            return mInstance;
        }

        private boolean mqttStart = Boolean.FALSE;

        public final void setMqttStart(boolean b) {
            this.mqttStart = b;
        }

        public final boolean isMqttStart() {
            return mqttStart;
        }
}

