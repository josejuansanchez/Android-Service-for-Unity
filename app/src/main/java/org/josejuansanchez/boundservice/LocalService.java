package org.josejuansanchez.boundservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class LocalService extends Service {

    public static final String TAG = LocalService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private MqttClient mMqttClient = null;

    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        try {
            mMqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(final String uri, final String topic, final String clientId,
                        final String username, final String password, final String payload) {

        new Thread(new Runnable() {
            public void run() {
                MqttClient client = null;
                try {
                    client = new MqttClient(uri, clientId, null);
                    client.setCallback(new MyMqttCallback());
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                MqttConnectOptions options = new MqttConnectOptions();
                //options.setUserName(username);
                //options.setPassword(password.toCharArray());

                try {
                    client.connect(options);
                } catch (MqttException e) {
                    Log.d(TAG, "Connection attempt failed with reason code: " + e.getReasonCode() + ":" + e.getCause());
                }

                try {
                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(payload.getBytes());
                    client.publish(topic, mqttMessage);
                    client.disconnect();
                }
                catch (MqttException e) {
                    Log.d(TAG, "Publish failed with reason code: " + e.getReasonCode());
                }
            }
        }).start();
    }

    public void subscribe(final String uri, final String topic, final String clientId, int qos) {

        try {
            mMqttClient = new MqttClient(uri, clientId, null);
            mMqttClient.setCallback(new MyMqttCallback());
        } catch (MqttException e) {
            e.printStackTrace();
        }

        MqttConnectOptions options = new MqttConnectOptions();
        //options.setUserName(username);
        //options.setPassword(password.toCharArray());

        try {
            mMqttClient.connect(options);
        } catch (MqttException e) {
            Log.d(TAG, "Connection attempt failed with reason code: " + e.getReasonCode() + ":" + e.getCause());
        }

        try {
            mMqttClient.subscribe(topic, qos);
        }
        catch (MqttException e) {
            Log.d(TAG, "Publish failed with reason code: " + e.getReasonCode());
        }
    }

    public class MyMqttCallback implements MqttCallback {

        public void connectionLost(Throwable cause) {
            Log.d(TAG, "MQTT Server connection lost: " + cause.toString());
        }

        public void messageArrived(String topic, MqttMessage message) {
            Log.d(TAG, "Message arrived: " + topic + ":" + message.toString());
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "Delivery complete");
        }
    }

    public boolean isMqttClientConnected() {
        if (mMqttClient ==  null) return false;
        return mMqttClient.isConnected();
    }
}