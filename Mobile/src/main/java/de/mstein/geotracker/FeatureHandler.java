package de.mstein.geotracker;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureTemplate;
import com.esri.arcgisruntime.data.FeatureType;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
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
                if (featureTable.canAdd()) {
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
                                final ListenableFuture<Void> addFeatureFuture = featureTable.addFeatureAsync(feature);
                                addFeatureFuture.addDoneListener(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            // check the result of the future to find out if/when the addFeatureAsync call succeeded - exception will be
                                            // thrown if the edit failed
                                            addFeatureFuture.get();

                                            // if using an ArcGISFeatureTable, call getAddedFeaturesCountAsync to check the total number of features
                                            // that have been added since last sync

                                            // if dealing with ServiceFeatureTable, apply edits after making updates; if editing locally, then edits can
                                            // be synchronized at some point using the SyncGeodatabaseTask.
                                            if (featureTable instanceof ServiceFeatureTable) {
                                                ServiceFeatureTable serviceFeatureTable = (ServiceFeatureTable)featureTable;
                                                // apply the edits
                                                final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = serviceFeatureTable.applyEditsAsync();
                                                applyEditsFuture.addDoneListener(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            final List<FeatureEditResult> featureEditResults = applyEditsFuture.get();
                                                            geoObjectActivity.completeSaveAction(true);
                                                        } catch (InterruptedException | ExecutionException e) {
                                                            e.printStackTrace();
                                                            geoObjectActivity.completeSaveAction(false);
                                                        }
                                                    }
                                                });
                                            }

                                        } catch (InterruptedException | ExecutionException e) {
                                            // executionException may contain an ArcGISRuntimeException with edit error information.
                                            if (e.getCause() instanceof ArcGISRuntimeException) {
                                                ArcGISRuntimeException agsEx = (ArcGISRuntimeException)e.getCause();
                                            } else {
                                                e.printStackTrace();
                                                geoObjectActivity.completeSaveAction(false);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        });
    }
}
