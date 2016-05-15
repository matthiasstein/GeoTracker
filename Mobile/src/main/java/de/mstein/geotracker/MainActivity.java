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
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.NavigationView;
import android.view.Menu;
import android.view.MenuItem;

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
        GeoObjectListFragment.OnFragmentInteractionListener,
        NavigationView.OnNavigationItemSelectedListener {

    private GoogleApiClient mGoogleApiClient;
    public static ArrayList<GeoObject> geoObjectList = new ArrayList<GeoObject>();
    public static final String PREFS_LIST_KEY = "geoObjectList";

    Toolbar mToolbar;
    private CharSequence mTitle;
    protected DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();

        // toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_drawer);

        // drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        loadList();

        if (savedInstanceState == null) {
            navigationView.getMenu().findItem(R.id.nav_new_poi).setChecked(true);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.content_frame, new GeoObjectListFragment(), "geoObjectFragment");
            //Fragment webSiteFragment = new WebSiteFragment();
            //transaction.add(R.id.content_frame, webSiteFragment, "webSiteFragment");
            //transaction.hide(webSiteFragment);
            Fragment infoFragment = new InfoFragment();
            transaction.add(R.id.content_frame, infoFragment, "infoFragment");
            transaction.hide(infoFragment);
            transaction.commit();
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager(); // For AppCompat use getSupportFragmentManager
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        GeoObjectListFragment geoObjectFragment = (GeoObjectListFragment) fragmentManager.findFragmentByTag("geoObjectFragment");
        //WebSiteFragment webSiteFragment = (WebSiteFragment) fragmentManager.findFragmentByTag("webSiteFragment");
        InfoFragment infoFragment = (InfoFragment) fragmentManager.findFragmentByTag("infoFragment");

        hideAllFragments();

        if (id == R.id.nav_new_poi) {
            transaction.show(geoObjectFragment);
        } /*else if (id == R.id.nav_map_apps) {
            transaction.show(webSiteFragment);
        }*/ else if (id == R.id.nav_info) {
            transaction.show(infoFragment);
        }

        transaction.commit();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void hideAllFragments() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        for (Fragment fragment : fm.getFragments()) {
            if (fragment.isVisible()) {
                transaction.hide(fragment);
            }
        }
        transaction.commit();
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
