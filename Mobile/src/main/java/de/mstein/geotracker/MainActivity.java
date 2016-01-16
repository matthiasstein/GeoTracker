package de.mstein.geotracker;

import android.content.Context;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import de.mstein.shared.GeoObject;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GeoObjectListFragment.OnFragmentInteractionListener {

    private GoogleApiClient mGoogleApiClient;
    public static ArrayList<GeoObject> geoObjectList = new ArrayList<GeoObject>();
    public static final String PREFS_LIST_KEY = "geoObjectList";

    Toolbar mToolbar;
    private String[] mDrawerTitles;
    private CharSequence mTitle;
    protected DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // drawer
        mTitle = getTitle();
        mDrawerTitles = getResources().getStringArray(R.array.drawer_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_drawer);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        loadList();

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    @Override
    protected void onResume() {
        super.onStart();
        mGoogleApiClient.connect();
        this.refreshList();
        this.saveList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = "GeoTracker - " + title;
        mToolbar.setTitle(mTitle);
    }

    @Override
    public void onFragmentInteraction(int i) {
        GeoObject g = geoObjectList.get(i);
        Bundle b = new Bundle();
        b.putSerializable("list", g);
        b.putInt("index", i);
        Intent intent = new Intent(this, GeoObjectActivity.class);
        intent.putExtras(b);
        startActivity(intent);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager(); // For AppCompat use getSupportFragmentManager
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment oldFragment = getActiveFragment();

        switch (position) {
            default:
            case 0:
                fragment = fragmentManager.findFragmentByTag("geoObjectFragment");
                if (oldFragment != null)
                    transaction.hide(oldFragment);
                if (fragment == null) {
                    transaction.add(R.id.content_frame, new GeoObjectListFragment(), "geoObjectFragment");
                } else {
                    transaction.remove(fragment);
                    fragment = new GeoObjectListFragment();
                    transaction.add(R.id.content_frame, fragment, "geoObjectFragment");
                }
                //transaction.replace(R.id.content_frame, new GeoObjectListFragment(), "geoObjectFragment");
                transaction.commit();
                break;
            case 1:
                fragment = fragmentManager.findFragmentByTag("webSiteFragment");
                if (oldFragment != null)
                    transaction.hide(oldFragment);
                if (fragment == null) {
                    transaction.add(R.id.content_frame, new WebSiteFragment(), "webSiteFragment");
                } else {
                    transaction.show(fragment);
                }
                //transaction.replace(R.id.content_frame, new WebSiteFragment(), "webSiteFragment");
                transaction.commit();
                break;
            case 2:
                fragment = fragmentManager.findFragmentByTag("infoFragment");
                if (oldFragment != null)
                    transaction.hide(oldFragment);
                if (fragment == null) {
                    transaction.add(R.id.content_frame, new InfoFragment(), "infoFragment");
                } else {
                    transaction.show(fragment);
                }
                //transaction.replace(R.id.content_frame, new InfoFragment(), "infoFragment");
                transaction.commit();
                break;
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    public Fragment getActiveFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment;
        fragment = fm.findFragmentByTag("geoObjectFragment");
        if (fragment != null)
            if (fragment.isVisible())
                return fragment;
        fragment = fm.findFragmentByTag("webSiteFragment");
        if (fragment != null)
            if (fragment.isVisible())
                return fragment;
        fragment = fm.findFragmentByTag("infoFragment");
        if (fragment != null)
            if (fragment.isVisible())
                return fragment;
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
                return true;
            case R.id.clear_list:
                this.geoObjectList.clear();
                this.refreshList();
                this.saveList();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                /*GeoObjectListFragment goFragment = (GeoObjectListFragment) getSupportFragmentManager().findFragmentByTag("geoObjectFragment");
                if (goFragment != null)
                    goFragment.refresh();*/
                refreshList();
            }
        }, 2000);
    }

    public void updateGOFragment() {
        GeoObjectListFragment goFragment = (GeoObjectListFragment) getSupportFragmentManager().findFragmentByTag("geoObjectFragment");
        if (goFragment != null)
            goFragment.refresh();
    }

    private void saveList() {
        SharedPreferences appSharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor prefsEditor = appSharedPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(geoObjectList);
        prefsEditor.putString(PREFS_LIST_KEY, json);
        prefsEditor.commit();
    }

    private void loadList() {
        Context appCtx = this.getApplicationContext();
        SharedPreferences appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(appCtx);
        Gson gson = new Gson();
        String json = appSharedPrefs.getString(PREFS_LIST_KEY, "");
        Type type = new TypeToken<ArrayList<GeoObject>>() {
        }.getType();
        geoObjectList = gson.fromJson(json, type);
        if (geoObjectList == null)
            geoObjectList = new ArrayList<GeoObject>();
    }

    public void refreshList() {
        FragmentManager fm = getSupportFragmentManager();
        GeoObjectListFragment fragment = (GeoObjectListFragment) fm.findFragmentByTag("geoObjectFragment");
        if (fragment != null)
            fragment.refresh();
    }
}
