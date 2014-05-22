package com.parse.fbcheckin;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

public class CreateCheckinActivity extends Activity {
	
    private EditText messageText;

    private TextView locationTextView;
    private String location;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_checkin);
        setTitle("Checkin");
        messageText = (EditText)findViewById(R.id.inputMessage);

        location = ((FacebookCheckinApplication) getApplication()).getSelectedPlace().getName();
        locationTextView = (TextView) findViewById(R.id.location);
        locationTextView.setText(location);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_checkin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_post) {
            final String message = messageText.getText().toString();
           
            Request request = Request
                    .newStatusUpdateRequest(Session.getActiveSession(), message, ((FacebookCheckinApplication) getApplication()).getSelectedPlace(), null, new Request.Callback() { //null,null = place, tags
                        @Override
                        public void onCompleted(Response response) {
                            //showPublishResult(message, response.getGraphObject(), response.getError());
                        }
                    });
            request.executeAsync();

        	Bundle bundle = new Bundle();
        	bundle.putString("message",message);

            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(RESULT_OK,intent);
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

}
