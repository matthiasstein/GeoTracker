package de.mstein.geotracker;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.Arrays;

public class TypeListActivity extends ListActivity {

    // Listview Adapter
    TypeListAdapter mAdapter;

    // Search EditText
    EditText mInputSearch;

    String[] itemTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_type_list);

        // Listview Data
        itemTypes = getApplicationContext().getResources().getStringArray(R.array.types);
        Arrays.sort(itemTypes);

        mInputSearch = (EditText) findViewById(R.id.inputSearch);

        // Adding items to listview
        mAdapter = new TypeListAdapter(this, itemTypes);
        setListAdapter(mAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        /**
         * Enabling Search Filter
         * */
        mInputSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                TypeListActivity.this.mAdapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = getIntent();
        intent.putExtra("type", itemTypes[position]);
        setResult(RESULT_OK, intent);
        finish();
    }

}
