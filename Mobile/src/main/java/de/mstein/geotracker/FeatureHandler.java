package de.mstein.geotracker;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.datasource.Feature;
import com.esri.arcgisruntime.datasource.arcgis.FeatureTemplate;
import com.esri.arcgisruntime.datasource.arcgis.FeatureType;
import com.esri.arcgisruntime.datasource.arcgis.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.mstein.shared.GeoObject;

/**
 * Created by Mattes on 05.12.2015.
 */
public class FeatureHandler {

    GeoObjectActivity geoObjectActivity;
    GeoObject go;

    private ServiceFeatureTable featureTable;
    private static String FEATURE_SERVICE_URL = "http://services5.arcgis.com/WQdAIjtIvpizzS3U/arcgis/rest/services/XErleben/FeatureServer/0";

    public FeatureHandler(GeoObjectActivity goa) {
        geoObjectActivity = goa;
        featureTable = new ServiceFeatureTable(FEATURE_SERVICE_URL);
        featureTable.loadAsync();
    }

    public void createFeature(final GeoObject go) {
        this.go = go;
        featureTable.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                Point point = new Point(go.getLon(), go.getLat());
                if (featureTable.isEditable()) {
                    List<FeatureType> types = featureTable.getFeatureTypes();
                    if (types != null) {
                        List<FeatureTemplate> templates = new ArrayList<FeatureTemplate>();
                        for (FeatureType type : types) {
                            List<FeatureTemplate> t = type.getTemplates();
                            templates.addAll(t);
                        }
                        for (FeatureTemplate template : templates) {
                            if (template.getName().equals(go.getType())) {
                                Feature feature = featureTable.createFeature(template, point);
                                java.util.Map<String, Object> attributes = feature.getAttributes();
                                Date date = new Date();
                                attributes.put("O_NAME", go.getName());
                                attributes.put("O_UUID", "go_" + date.getTime());
                                //attributes.put("M_BEARBTAM", date);
                                //attributes.put("M_LEBZSTAR", new Date(go.getDate()));
                                attributes.put("M_INFOQUEL", "Anwender");
                                attributes.put("M_PFLGSTEL", "GeoTracker");
                                attributes.put("I_BESCHR", go.getDescription());
                                feature = featureTable.createFeature(attributes, point);
                                //add the new feature
                                final ListenableFuture<Boolean> result = featureTable.addFeatureAsync(feature);

                                result.addDoneListener(new Runnable() {

                                    @Override
                                    public void run() {
                                        //was it successful?
                                        try {
                                            if (result.get() == true) {
                                                geoObjectActivity.completeSaveAction(true);
                                            }
                                        } catch (InterruptedException e) {
                                            // Code to catch exception
                                            e.printStackTrace();
                                            geoObjectActivity.completeSaveAction(false);
                                        } catch (ExecutionException e) {
                                            // Code to catch exception
                                            e.printStackTrace();
                                            geoObjectActivity.completeSaveAction(false);
                                        }
                                    }
                                });

                                //apply edits to the server
                                featureTable.applyEditsAsync();
                            }
                        }
                    }
                }
            }
        });
    }
}
