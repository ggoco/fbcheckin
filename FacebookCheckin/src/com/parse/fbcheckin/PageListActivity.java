package com.parse.fbcheckin;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.model.GraphPlace;
import com.facebook.widget.PickerFragment;
import com.facebook.widget.PlacePickerFragment;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PageListActivity extends FragmentActivity{
    PlacePickerFragment placePickerFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_list);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();
        placePickerFragment = (PlacePickerFragment) fragmentManager.findFragmentById(R.id.place_picker_fragment);
        if (savedInstanceState == null) {
            // If this is the first time we have created the fragment, update its properties based on
            // any parameters we received via our Intent.
            placePickerFragment.setSettingsFromBundle(getIntent().getExtras());
        }
        placePickerFragment.setShowTitleBar(false);

        // move to the next page when a place is
        // selected (since only a single place can be selected).
        placePickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(PickerFragment<?> fragment) {
            	if (placePickerFragment.getSelection() != null) {
            		createCheckin();
            	}
            }
        });
        placePickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> fragment, FacebookException error) {
                //PageListActivity.this.onError(error);
            }
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(resultCode){
            case RESULT_OK:
                if(data==null) return;
                setResult(RESULT_OK,data);
                finish();
                break;
            case RESULT_CANCELED:
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.page_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.action_next:
                createCheckin();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createCheckin(){
        GraphPlace selection = placePickerFragment.getSelection();
        if(selection==null) {
            /*
             * if the user did not choose a place, alert the user.
             */
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage("Please choose a location")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }else{
        	((FacebookCheckinApplication) getApplication()).setSelectedPlace(selection);
            placePickerFragment.setSearchText(selection.getName());
            Intent i = new Intent(this, CreateCheckinActivity.class);
            startActivityForResult(i, FacebookCheckinApplication.ACTIVITY_CREATE);
        }
    }
    public static void populateParams(Intent intent, Location location){
    	intent.putExtra(PlacePickerFragment.LOCATION_BUNDLE_KEY, location);
    }
}
