package de.mstein.geotracker;

/**
 * Created by Mattes on 25.12.2015.
 */

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.mstein.shared.GeoObject;

public class GeoObjectListAdapter extends BaseAdapter {
    Context context;
    List<GeoObject> geoObject;

    GeoObjectListAdapter(Context context, List<GeoObject> geoObject) {
        this.context = context;
        this.geoObject = geoObject;
    }

    @Override
    public int getCount() {

        return geoObject.size();
    }

    @Override
    public Object getItem(int position) {
        return geoObject.get(position);
    }

    @Override
    public long getItemId(int position) {

        return geoObject.indexOf(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.geoobject_list_item, null);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.title);

        GeoObject go = geoObject.get(position);
        // setting the image resource and title
        imgIcon.setImageResource(getIcon(go.getType()));
        txtTitle.setText(go.getName() + " - " + go.getType());

        return convertView;
    }

    public int getIcon(String type) {
        String[] itemTypes = context.getResources().getStringArray(R.array.types);
        TypedArray itemIcons = context.getResources().obtainTypedArray(R.array.icons);

        int result = R.drawable.xe_servicestelle_infopoint;
        for(int i=0;i<itemTypes.length;i++) {
            String t = itemTypes[i];
            if(t.equals(type)) {
                result = itemIcons.getResourceId(i,-1);
            }
        }
        return result;
    }

}
