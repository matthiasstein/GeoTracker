package de.mstein.geotracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Locale;

import de.mstein.shared.GeoObject;

public class WearMainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "WearableActivity";
    private static final String START_GEOTRACKER_PATH = "/start_geotracker_mobile_app";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Node mNode;
    private BoxInsetLayout mContainerView;
    private TextView mClockView;
    private Handler mHandler = new Handler();
    private View mDot, mRecButton, mRecButtonDisabled;

    private static final long UPDATE_INTERVAL_MS = 5 * 1000;
    private static final long FASTEST_INTERVAL_MS = 5 * 1000;
    private static final long INDICATOR_DOT_FADE_AWAY_MS = 500L;

    public static final String PREFS_TYPE_KEY = "type";
    public static final String PREFS_DESC_KEY = "description";
    private static final String GO_KEY = "de.mstein.key.go";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);

        final GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new GridViewPagerAdapter());
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setPager(pager);

        setAmbientEnabled();

        if (!hasGps()) {
            Log.w(TAG, "This hardware doesn't have GPS! The Application will finish.");
        }
        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

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

    public void onRecordButtonClick() {
        flashDot();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String description = pref.getString(PREFS_DESC_KEY, "");
        String type = pref.getString(PREFS_TYPE_KEY, "GeoObject");
        String name = "Neuer POI";
        saveGeoObject(mLastLocation.getLatitude(), mLastLocation.getLongitude(), name, type, description);
        showOpenOnPhoneConfirmationActivity(getString(R.string.sent_to_phone));
    }

    public void showOpenOnPhoneConfirmationActivity(String s) {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, s);
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
            }
        });
        startLocationUpdates();
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
        mLastLocation = location;
    }

    /**
     * Adjusts layout of the record button based on the arrival of GPS data.
     */
    private void updateVisibility(boolean visible) {
        if (mRecButton != null && mRecButtonDisabled != null) {
            if (visible) {
                mRecButton.setVisibility(View.VISIBLE);
                mRecButtonDisabled.setVisibility(View.GONE);
            } else {
                mRecButton.setVisibility(View.GONE);
                mRecButtonDisabled.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Adjusts the clock view and the container background color in case of ambiend mode.
     */
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

    /**
     * Causes the (green) dot blinks when new POI was tracked.
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

    /**
     * Starts the SentGeoObjectToDataLayerThread that sends the tracked GeoObject to the connected smartphone.
     */
    private void saveGeoObject(double lat, double lon, String name, String type, String description) {
        DataMap dm = new DataMap();
        GeoObject go = new GeoObject(lat, lon, name, type, description);
        go.putToDataMap(dm);

        SendGeoObjectToDataLayerThread t = new SendGeoObjectToDataLayerThread("/geoobject", dm, mGoogleApiClient, GO_KEY);
        t.run();
    }

    /**
     * Starts the mobile app.
     */
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
                            } else {
                                showOpenOnPhoneConfirmationActivity(getString(R.string.opend_on_phone));
                            }
                        }
                    }
            );
        } else {
            //Improve your code
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    startLocationUpdates();
                } else {
                    // permission denied, boo!
                }
            }
        }
    }

    public void startLocationUpdates() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        } else {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)
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
    }

    /*
     * GridViewAdapeter Class
     */
    public class GridViewPagerAdapter extends GridPagerAdapter {
        @Override
        public int getColumnCount(int arg0) {
            return 4;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int row, int col) {
            if (col == 0) {
                final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.grid_view_page_1, container, false);
                mDot = view.findViewById(R.id.dot);
                mRecButton = view.findViewById(R.id.rec_button);
                mRecButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onRecordButtonClick();
                    }
                });
                mRecButtonDisabled = view.findViewById(R.id.rec_button_disabled);
                mRecButtonDisabled.setEnabled(false);
                mRecButton.setVisibility(View.GONE);
                mRecButtonDisabled.setVisibility(View.VISIBLE);
                container.addView(view);
                return view;
            } else if (col == 1) {
                final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.grid_view_page_2, container, false);
                final CircledImageView voiceInputButton = (CircledImageView) view.findViewById(R.id.voice_input);

                voiceInputButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(WearMainActivity.this,
                                VoiceInputActivity.class);
                        startActivity(intent);
                    }
                });

                container.addView(view);
                return view;
            } else if (col == 2) {
                final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.grid_view_page_3, container, false);
                final CircledImageView settingsButton = (CircledImageView) view.findViewById(R.id.settings_list);

                settingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(WearMainActivity.this,
                                TypeListActivity.class);
                        startActivity(intent);
                    }
                });

                container.addView(view);
                return view;
            } else if (col == 3) {
                final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.grid_view_page_4, container, false);
                final CircledImageView smartPhoneButton = (CircledImageView) view.findViewById(R.id.smartphone);

                smartPhoneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startMobileApp();
                    }
                });

                container.addView(view);
                return view;
            } else {
                return null;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int row, int col, Object view) {
            container.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
