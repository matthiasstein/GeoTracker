package de.mstein.geotracker;

import android.content.Context;
import android.widget.Toast;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.Field;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.Symbol;

import java.util.Date;
import java.util.Map;

import de.mstein.shared.GeoObject;

/**
 * Created by Mattes on 05.12.2015.
 */
public class FeatureHandler implements CallbackListener<FeatureEditResult[][]> {

    GeoObjectActivity geoObjectActivity;

    private ArcGISFeatureLayer featureLayer;
    private static String FEATURE_SERVICE_URL = "http://services5.arcgis.com/WQdAIjtIvpizzS3U/ArcGIS/rest/services/GeoTracker/FeatureServer/0";

    public FeatureHandler(GeoObjectActivity goa) {
        geoObjectActivity = goa;
        featureLayer = new ArcGISFeatureLayer(FEATURE_SERVICE_URL, new ArcGISFeatureLayer.Options());
    }

    public void createFeature(GeoObject go) {
        Point point = new Point(go.getLon(), go.getLat());
        if (featureLayer != null) {
            FeatureTemplate[] templates = featureLayer.getTemplates();
            for (FeatureTemplate template : templates) {
                Graphic newFeatureGraphic = featureLayer.createFeatureWithTemplate(template, point);
                Geometry geometry = newFeatureGraphic.getGeometry();
                Symbol symbol = newFeatureGraphic.getSymbol();
                Map attributes = newFeatureGraphic.getAttributes();
                attributes.put("name", go.getName());
                attributes.put("type", go.getType());
                attributes.put("date", new Date(go.getDate()));
                attributes.put("description", go.getDescription());
                Graphic g = new Graphic(geometry,symbol,attributes);
                Graphic[] adds = {g};
                featureLayer.applyEdits(adds, null, null, this);
            }
        }
    }

    public void onError(Throwable error) {
        geoObjectActivity.completeSaveAction(null);
    }

    public void onCallback(FeatureEditResult[][] editResult) {
        geoObjectActivity.completeSaveAction(editResult);

    }
}
