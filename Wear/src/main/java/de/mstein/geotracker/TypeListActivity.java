/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mstein.geotracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * An activity that presents a list of disorders to user and allows user to pick one, to be used as
 * the current disorder.
 */
public class TypeListActivity extends Activity implements WearableListView.ClickListener {

    /* Disorders, that will be shown on the list */
    private static ArrayList<ListItem> mItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.type_list_activity);

        String[] itemTypes = getResources().getStringArray(R.array.types);
        TypedArray itemIcons = getResources().obtainTypedArray(R.array.icons);

        mItems = new ArrayList<ListItem>();
        for(int i=0; i<itemTypes.length;i++) {
            String s[] = itemTypes[i].split(" ");
            mItems.add(new ListItem(s[1], itemIcons.getResourceId(i, -1)));
        }

        final TextView header = (TextView) findViewById(R.id.header);

        // Get the list component from the layout of the activity
        WearableListView listView = (WearableListView) findViewById(R.id.wearable_list);

        // Assign an adapter to the list
        listView.setAdapter(new ListAdapter(this, mItems));

        // Set a click listener
        listView.setClickListener(this);

        listView.addOnScrollListener(new WearableListView.OnScrollListener() {

            @Override
            public void onScroll(int i) {
            }

            @Override
            public void onAbsoluteScrollChange(int i) {
                // only scroll the header up from the base position, not down...
                if (i > 0) {
                    header.setY(-i);
                }
            }

            @Override
            public void onScrollStateChanged(int i) {
            }

            @Override
            public void onCentralPositionChanged(int i) {
            }
        });
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String name = mItems.get(viewHolder.getPosition()).getName();
        pref.edit().putString(WearMainActivity.PREFS_TYPE_KEY, name).apply();
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }
}
