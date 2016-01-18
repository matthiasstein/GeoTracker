package de.mstein.geotracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;

import de.mstein.shared.GeoObject;

public class ListenerServiceFromWear extends WearableListenerService {

    private static final String START_GEOTRACKER_PATH = "/start_geotracker_mobile_app";
    public static final String PREFS_LIST_KEY = "geoObjectList";
    private static final String GO_KEY = "de.mstein.key.go";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(START_GEOTRACKER_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/geoobject") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    GeoObject go = new GeoObject(dataMap.getDataMap(GO_KEY));
                    MainActivity.geoObjectList.add(go);
                    saveList();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private void saveList() {
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor prefsEditor = appSharedPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(MainActivity.geoObjectList);
        prefsEditor.putString(PREFS_LIST_KEY, json);
        prefsEditor.commit();
    }

}