package de.mstein.geotracker;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Mattes on 02.11.2015.
 */
public class SendGeoObjectToDataLayerThread extends Thread {
    String path;
    DataMap dataMap;
    GoogleApiClient mGoogleApiClient;
    String key;
    WearMainActivity activity;

    // Constructor
    public SendGeoObjectToDataLayerThread(String p, DataMap data, GoogleApiClient googleApiClient, String k, WearMainActivity a) {
        path = p;
        dataMap = data;
        mGoogleApiClient = googleApiClient;
        key = k;
        activity = a;
    }

    public void run() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putDataMap(key, dataMap);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        DataApi.DataItemResult result = pendingResult.await();
        if(result.getStatus().isSuccess()) {
            activity.showOpenOnPhoneConfirmationActivity(activity.getApplicationContext().getString(R.string.sent_to_phone));
        }
    }
}
