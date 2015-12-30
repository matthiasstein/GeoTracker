package de.mstein.geotracker;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Mattes on 02.11.2015.
 */
public class SendToDataLayerThread extends Thread {
    String path;
    DataMap dataMap;
    GoogleApiClient mGoogleApiClient;
    String key;

    // Constructor for sending data objects to the data layer
    public SendToDataLayerThread(String p, DataMap data, GoogleApiClient googleApiClient, String k) {
        path = p;
        dataMap = data;
        mGoogleApiClient = googleApiClient;
        key = k;
    }

    public void run() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/geoobject");
        putDataMapReq.getDataMap().putDataMap(key, dataMap);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }
}
