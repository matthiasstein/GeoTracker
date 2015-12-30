package de.mstein.geotracker;

/**
 * Created by Mattes on 25.12.2015.
 */

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.mstein.shared.GeoObject;

public class TypeListAdapter extends ArrayAdapter<String> {

    public TypeListAdapter(Context context, String[] types) {
        super(context, 0, types);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            /*LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.type_list_item, null);*/
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.type_list_item, parent, false);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.icon);
        TextView txtType = (TextView) convertView.findViewById(R.id.type);

        String type = getType(getItem(position));
        // setting the image resource and title
        imgIcon.setImageResource(getIcon(type));
        txtType.setText(type);

        return convertView;
    }

    public int getIcon(String type) {
        String[] itemTypes = getContext().getResources().getStringArray(R.array.types);
        TypedArray itemIcons = getContext().getResources().obtainTypedArray(R.array.icons);

        int result = R.drawable.xe_servicestelle_infopoint;
        for(int i=0;i<itemTypes.length;i++) {
            String t = getType(itemTypes[i]);
            if(t.equals(type)) {
                result = itemIcons.getResourceId(i,-1);
            }
        }
        return result;
    }

    private String getType(String item) {
        String[] s = item.split(" ");
        return s[1];
    }

}
