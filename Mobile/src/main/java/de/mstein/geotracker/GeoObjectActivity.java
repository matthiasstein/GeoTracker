package de.mstein.geotracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.Graphic;

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

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void completeSaveAction(final FeatureEditResult[][] results) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (results != null) {
                    if (results[0][0].isSuccess()) {
                        Toast.makeText(GeoObjectActivity.this, R.string.upload_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GeoObjectActivity.this, R.string.upload_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        MainActivity.geoObjectList.remove(i);
        this.finish();
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
        fh.createFeature(mGeoObject);
    }

    public void changeType(View view) {
        Intent intent = new Intent(this, TypeListActivity.class);
        //startActivity(intent);
        startActivityForResult(intent, TYPE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == TYPE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                String type= b.getString("type");
                mTypeText.setText(type);
            }
        }
    }
}
