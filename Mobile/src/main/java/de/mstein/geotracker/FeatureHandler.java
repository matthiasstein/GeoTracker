package de.mstein.geotracker;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureTemplate;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.Symbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.mstein.shared.GeoObject;

/**
 * Created by Mattes on 05.12.2015.
 */
public class FeatureHandler implements CallbackListener<FeatureEditResult[][]> {

    GeoObjectActivity geoObjectActivity;

    private ArcGISFeatureLayer featureLayer;
    private static String FEATURE_SERVICE_URL = "http://services5.arcgis.com/WQdAIjtIvpizzS3U/arcgis/rest/services/XErleben/FeatureServer/0";

    public FeatureHandler(GeoObjectActivity goa) {
        geoObjectActivity = goa;
        featureLayer = new ArcGISFeatureLayer(FEATURE_SERVICE_URL, new ArcGISFeatureLayer.Options());
    }

    public void createFeature(GeoObject go) {
        Point point = new Point(go.getLon(), go.getLat());
        if (featureLayer != null) {
            FeatureType[] types = featureLayer.getTypes();
            if (types != null) {
                List<FeatureTemplate> templates = new ArrayList<FeatureTemplate>();
                for (FeatureType type : types) {
                    FeatureTemplate[] t = type.getTemplates();
                    templates.addAll(new ArrayList<FeatureTemplate>(Arrays.asList(t)));
                }
                for (FeatureTemplate template : templates) {
                    if (template.getName().equals(go.getType())) {
                        Graphic newFeatureGraphic = featureLayer.createFeatureWithTemplate(template, point);
                        Geometry geometry = newFeatureGraphic.getGeometry();
                        Symbol symbol = newFeatureGraphic.getSymbol();
                        Map attributes = newFeatureGraphic.getAttributes();
                        attributes.put("O_NAME", go.getName());
                        attributes.put("O_UUID", "go_" + new Date().getTime());
                        attributes.put("M_BEARBTAM", new Date());
                        attributes.put("M_LEBZSTAR", new Date(go.getDate()));
                        attributes.put("M_INFOQUEL", "Anwender");
                        attributes.put("M_PFLGSTEL", "GeoTracker");
                        attributes.put("I_BESCHR", go.getDescription());
                        Graphic g = new Graphic(geometry, symbol, attributes);
                        Graphic[] adds = {g};
                        featureLayer.applyEdits(adds, null, null, this);
                    }
                }
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
