package de.mstein.geotracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.mstein.shared.GeoObject;

public class GeoObjectActivity extends AppCompatActivity {

    GeoObject mGeoObject;
    int i;
    EditText mNameText, mDescriptionText;
    TextView mTypeText, mDateText;
    FeatureHandler fh;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss", Locale.GERMANY);

    static final int TYPE_REQUEST = 1;  // The request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_object);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle b = this.getIntent().getExtras();
        if (b != null) {
            mGeoObject = (GeoObject) b.getSerializable("list");
            i = b.getInt("index");
        }

        fh = new FeatureHandler(this);

        this.setTitle(mGeoObject.toString());

        mNameText = (EditText) findViewById(R.id.nameText);
        mTypeText = (TextView) findViewById(R.id.typeText);
        mDateText = (TextView) findViewById(R.id.dateText);
        mDescriptionText = (EditText) findViewById(R.id.descriptionText);

        mNameText.setText(String.valueOf(mGeoObject.getName()));
        mTypeText.setText(String.valueOf(mGeoObject.getType()));
        Date d = new Date(mGeoObject.getDate());
        mDateText.setText(DATE_FORMAT.format(d));
        mDescriptionText.setText(String.valueOf(mGeoObject.getDescription()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // create map
        MapView mMapView = (MapView) findViewById(R.id.mapView);
        ArcGISMap mMainMap = new ArcGISMap(Basemap.createLightGrayCanvas());
        mMainMap.setInitialViewpoint(new Viewpoint(mGeoObject.getLat(), mGeoObject.getLon(), 5000));

        mMapView.setMap(mMainMap);
        GraphicsOverlay grOverlay = new GraphicsOverlay();
        SimpleRenderer simpleRenderer = new SimpleRenderer();
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 16);
        simpleRenderer.setSymbol(pointSymbol);
        grOverlay.setRenderer(simpleRenderer);
        mMapView.getGraphicsOverlays().add(grOverlay);
        Graphic locationGraphic = new Graphic();
        Point point = new Point(mGeoObject.getLon(), mGeoObject.getLat(), SpatialReferences.getWgs84());
        Point projectedPoint = (Point) GeometryEngine.project(point, SpatialReferences.getWebMercator());
        locationGraphic.setGeometry(projectedPoint);
        grOverlay.getGraphics().add(locationGraphic);
    }

    public void completeSaveAction(final boolean b) {
        if (b) {
            Toast.makeText(GeoObjectActivity.this, R.string.upload_success, Toast.LENGTH_SHORT).show();
            MainActivity.geoObjectList.remove(i);
        } else {
            Toast.makeText(GeoObjectActivity.this, R.string.upload_error, Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    public void saveGeoObject(View view) {
        GeoObject geoObject = new GeoObject(mGeoObject.getLat(), mGeoObject.getLon(), mNameText.getText().toString(), mTypeText.getText().toString(), mDescriptionText.getText().toString(), mGeoObject.getDate());
        MainActivity.geoObjectList.set(i, geoObject);
        this.finish();
    }

    public void removeGeoObject(View view) {
        MainActivity.geoObjectList.remove(i);
        this.finish();
    }

    public void uploadFeature(View view) {
        if (!mTypeText.getText().toString().equals("GeoObject")) {
            mGeoObject = new GeoObject(mGeoObject.getLat(), mGeoObject.getLon(), mNameText.getText().toString(), mTypeText.getText().toString(), mDescriptionText.getText().toString(), mGeoObject.getDate());
            fh.createFeature(mGeoObject);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GeoObjectActivity.this, R.string.change_type, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void changeDatatype(View view) {
        Intent intent = new Intent(this, TypeListActivity.class);
        startActivityForResult(intent, TYPE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == TYPE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                String type = b.getString("type");
                mTypeText.setText(type);
            }
        }
    }
}
