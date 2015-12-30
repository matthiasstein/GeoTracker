package de.mstein.geotracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.mstein.shared.GeoObject;

public class WearMainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "WearableActivity";
    private static final String START_GEOTRACKER_PATH = "/start_geotracker_mobile_app";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private GoogleApiClient mGoogleApiClient;
    private Node mNode;
    private BoxInsetLayout mContainerView;
    private TextView mClockView;
    private Handler mHandler = new Handler();
    private View mDot, mRecButton, mRecButtonDisabled;

    private double lat, lon;

    private static final long UPDATE_INTERVAL_MS = 5 * 1000;
    private static final long FASTEST_INTERVAL_MS = 5 * 1000;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    public static final String PREFS_TYPE_KEY = "type";
    public static final String PREFS_DESC_KEY = "description";
    //public static final String PREFS_NAME_KEY = "name";
    private static final String GO_KEY = "de.mstein.key.go";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        setAmbientEnabled();

        if (!hasGps()) {
            // If this hardware doesn't support GPS, we prefer to exit.
            // Note that when such device is connected to a phone with GPS capabilities, the
            // framework automatically routes the location requests to the phone. For this
            // application, this would not be desirable so we exit the app but for some other
            // applications, that might be a valid scenario.
            Log.w(TAG, "This hardware doesn't have GPS, so we exit");
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gps_not_available))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.cancel();
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }

        setupViews();
        updateVisibility(false);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.edit().clear().commit();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    public void onRecordButtonClick(View view) {
        flashDot();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String description = pref.getString(PREFS_DESC_KEY, "");
        String type = pref.getString(PREFS_TYPE_KEY, "GeoObject");
        //String name = pref.getString(PREFS_NAME_KEY, "name");
        //String type = "";
        String name = "New GeoObject";
        saveGeoObject(lat, lon, name, type, description);

        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.sent_to_phone));
        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                for (Node node : nodes.getNodes()) {
                    mNode = node;
                }
                startMobileApp();
            }
        });

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {

                    @Override
                    public void onResult(Status status) {
                        if (status.getStatus().isSuccess()) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Successfully requested location updates");
                            }
                        } else {
                            Log.e(TAG,
                                    "Failed in requesting location updates, "
                                            + "status code: "
                                            + status.getStatusCode() + ", message: " + status
                                            .getStatusMessage());
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Returns {@code true} if this device has the GPS capabilities.
     */
    private boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public void onLocationChanged(Location location) {
        updateVisibility(true);
        lat = location.getLatitude();
        lon = location.getLongitude();
    }

    /**
     * Adjusts the visibility of speed indicator based on the arrival of GPS data.
     */
    private void updateVisibility(boolean visible) {
        if (visible) {
            mRecButton.setVisibility(View.VISIBLE);
            mRecButtonDisabled.setVisibility(View.GONE);
        } else {
            mRecButton.setVisibility(View.GONE);
            mRecButtonDisabled.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackgroundColor(getResources().getColor(R.color.color1));
            mClockView.setVisibility(View.GONE);
        }
    }

    private void setupViews() {

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);
        mDot = findViewById(R.id.dot);
        mRecButton = findViewById(R.id.rec_button);
        mRecButtonDisabled = findViewById(R.id.rec_button_disabled);
        mRecButtonDisabled.setEnabled(false);

        ImageButton settingsButton = (ImageButton) findViewById(R.id.settings);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WearMainActivity.this,
                        TypeListActivity.class);
                startActivity(intent);
            }
        });

        ImageButton voiceInputButton = (ImageButton) findViewById(R.id.voice_input);

        voiceInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //displaySpeechRecognizer("name");
                Intent intent = new Intent(WearMainActivity.this,
                        VoiceInputActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Causes the (green) dot blinks when new GPS location data is acquired.
     */
    private void flashDot() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDot.setVisibility(View.VISIBLE);
            }
        });
        mDot.setVisibility(View.VISIBLE);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDot.setVisibility(View.INVISIBLE);
            }
        }, INDICATOR_DOT_FADE_AWAY_MS);
    }

    private void saveGeoObject(double lat, double lon, String name, String type, String description) {
        DataMap dm = new DataMap();
        GeoObject go = new GeoObject(lat, lon, name, type, description);
        go.putToDataMap(dm);

        SendToDataLayerThread t = new SendToDataLayerThread("/geoobject", dm, mGoogleApiClient, GO_KEY);
        t.run();
    }

    private void startMobileApp() {
        if (mNode != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mNode.getId(), START_GEOTRACKER_PATH, null).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("TAG", "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    }
            );
        } else {
            //Improve your code
        }

    }
}
