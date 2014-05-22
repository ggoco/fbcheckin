package com.parse.fbcheckin;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.GraphUserListCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class CheckinListActivity extends ListActivity implements LocationListener{

	/**
	 * constants for requesting post permission
	 */
	private static final String PERMISSION = "publish_actions";
	private static final int REAUTH_ACTIVITY_CODE = 100;
	
	private final int LIST_LIMIT = 10;
	/**
	 * list items
	 */
	private ProfilePictureView userProfilePictureView;
	private TextView userNameView;
	private ListView lv;
	private Button btnLoadMore;
	private List<ParseObject> checkinList;
	private CheckinListAdapter adapter;
	
	private Dialog progressDialog;
	
	private Location lastKnownLocation;
	private LocationManager locationManager;
	private List<String> friendIds = new ArrayList<String>();
	
	private boolean pendingRequest = false;	//prevent error or double pending request
    private static final Location PHILS_LOCATION = new Location("") {
        {
            setLatitude(14.5833);
            setLongitude(121.0000);
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_checkin_list);

		userProfilePictureView = (ProfilePictureView) findViewById(R.id.userPicture);
		userNameView = (TextView) findViewById(R.id.userName);
		
		// Fetch Facebook user info if the session is active
		Session session = ParseFacebookUtils.getSession();
		if (session != null && session.isOpened()) {
			getFriends(session);
			makeMeRequest();
		}

        ImageButton imageButton = (ImageButton) findViewById(R.id.create_checkin);
        imageButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                createCheckin();
            }
        });
        
        lv = (ListView) findViewById(android.R.id.list);
		btnLoadMore = new Button(this);
        //btnLoadMore.setId(id);
        btnLoadMore.setText("Load More");
 
        // Add Load More button to list view at bottom
        lv.addFooterView(btnLoadMore);
        /*
         * Listen to Load More button click event
         */
        btnLoadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Start async task
                new CheckinListTask(CheckinListTask.MORE).execute();
            }
        });
	}
    private void getFriends(Session session) {
    	Request.newMyFriendsRequest(session,
            new GraphUserListCallback() {

                @Override
                public void onCompleted(List<GraphUser> users,
                        Response response) {
                    for (GraphUser user : users){
                    	friendIds.add(user.getId());
                    }   
                    new CheckinListTask(CheckinListTask.START).execute();
                }
            }
		).executeAsync();
	}
	private void createCheckin(){
    	try {
			if (lastKnownLocation == null) {
                Criteria criteria = new Criteria();
                String bestProvider = locationManager.getBestProvider(criteria, false);
                if (bestProvider != null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
                }
            }
            if (lastKnownLocation == null) {
                lastKnownLocation = PHILS_LOCATION;
            }
        } catch (Exception ex) {
        	lastKnownLocation = PHILS_LOCATION;
        }
    	
        requestPublishPermissions(ParseFacebookUtils.getSession());
    }

    private void requestPublishPermissions(Session session) {
		if(!pendingRequest&&!session.getPermissions().contains(PERMISSION)&&(session != null)){
			pendingRequest = true;
	            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSION)
	                    // demonstrate how to set an audience for the publish permissions,
	                    // if none are set, this defaults to `
	                    .setDefaultAudience(SessionDefaultAudience.FRIENDS)
	                    .setRequestCode(REAUTH_ACTIVITY_CODE);
	            session.requestNewPublishPermissions(newPermissionsRequest);
        }

		Intent i = new Intent(this, PageListActivity.class);
        PageListActivity.populateParams(i,lastKnownLocation);
        startActivityForResult(i, FacebookCheckinApplication.ACTIVITY_CREATE);
	}        
    
	@Override
	public void onResume() {
		super.onResume();

		ParseUser currentUser = ParseUser.getCurrentUser();
		if (currentUser != null) {
			// Check if the user is currently logged
			// and show any cached content
			updateViewsWithProfileInfo();
		} else {
			// If the user is not logged in, go to the
			// activity showing the login view.
			startLoginActivity();
		}
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; add items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
	@Override
	public void onBackPressed(){
		onLogoutButtonClicked();
	}
	@Override
	public void onPause() {
	    super.onPause();

	    if(progressDialog != null)		// dismiss dialog when screen is rotated to prevent errors
	    	progressDialog.dismiss();
	    progressDialog = null;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks.
        switch(item.getItemId()){
            case R.id.action_logout:
            	onLogoutButtonClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        
    }
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){
		super.onActivityResult(requestCode, resultCode, intent);
		if(intent==null) return;
		final Bundle extras = intent.getExtras();
		
		switch(requestCode){
			case FacebookCheckinApplication.ACTIVITY_CREATE:
				new CheckinListTask(CheckinListTask.START){
					protected Void doInBackground(Void... params) {
						ParseObject checkin = new ParseObject("Checkin");
						checkin.put("author", ParseUser.getCurrentUser());
						checkin.put("author_uid", ParseUser.getCurrentUser().getObjectId());
						checkin.put("facebookId", ParseUser.getCurrentUser().getString("facebookId"));
						checkin.put("message", extras.getString("message"));
                        FacebookCheckinApplication application = (FacebookCheckinApplication) getApplication();

						checkin.put("page_id", application.getSelectedPlace().getId());
						checkin.put("page_name", application.getSelectedPlace().getName());
					try {
                        Log.i("checkin","saving checkin");
						checkin.save();
					} catch (ParseException e) {
                        Log.i("checkin","not saved");
                        e.printStackTrace();
					}

					super.doInBackground();
					return null;
					}
				}.execute();
				break;
		}
	}
	private void makeMeRequest() {
		Request request = Request.newMeRequest(ParseFacebookUtils.getSession(), new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
                    //userProfile.get
                    ParseUser currentUser = ParseUser
                            .getCurrentUser();
                    friendIds.add(user.getId());
                    currentUser.put("facebookId", user.getId());
                    currentUser.put("name", user.getName());
                    currentUser.saveInBackground();

                    // Show the user info
                    updateViewsWithProfileInfo();
                    
                    new CheckinListTask(CheckinListTask.START).execute(); //populate list
                } else if (response.getError() != null) {
                    if ((response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_RETRY)
                            || (response.getError().getCategory() == FacebookRequestError.Category.AUTHENTICATION_REOPEN_SESSION)) {
                        Log.d(FacebookCheckinApplication.TAG,
                                "The facebook session was invalidated.");
                        onLogoutButtonClicked();
                    } else {
                        Log.d(FacebookCheckinApplication.TAG,
                                "Some other error: "
                                        + response.getError()
                                                .getErrorMessage());
                    }
                }
            }
        });
		request.executeAsync();
				
	}

	private void updateViewsWithProfileInfo() {
		ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser.getString("facebookId") != null) {
            String facebookId = currentUser.get("facebookId")
                    .toString();
            userProfilePictureView.setProfileId(facebookId);
        } else {
            // Show the default, blank user profile picture
            userProfilePictureView.setProfileId(null);
        }
        if (currentUser.getString("name") != null) {
            userNameView.setText(currentUser.getString("name"));
        } else {
            userNameView.setText("You");
        }
	}

	private void onLogoutButtonClicked() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(R.drawable.ic_action_warning);
		alertDialog.setTitle("Log Out");
		alertDialog.setMessage("Are you sure?");
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alertDialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Logout();				
			}
		});
		alertDialog.show();
	}
	private void Logout() {
		// Log the user out
		ParseUser.logOut();
		
		// destroy facebook session
		com.facebook.Session fbs = com.facebook.Session.getActiveSession();
		if (fbs == null) {
		    fbs = new com.facebook.Session(this);
		    com.facebook.Session.setActiveSession(fbs);
		}
		fbs.closeAndClearTokenInformation();
		  
		// Go to the login view
		startLoginActivity();
	}

	private void startLoginActivity() {
		Intent intent = new Intent(this, LoginActivity.class);
		Toast.makeText(this, "You have logged out.", Toast.LENGTH_LONG).show();
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	private class CheckinListTask extends AsyncTask<Void,Void,Void>{
		private int source;
		public static final int START	= 0;
		public static final int MORE	= 2;
		
		private List<ParseObject> moreCheckinList;
		
		public CheckinListTask(int source) {
			this.source = source;
		}
		
		/**
		 * Get the list of checkins in sorted order
		 */
		@Override
		protected Void doInBackground(Void... params) {
			ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Checkin");
			query.orderByDescending("_created_at");
			query.setLimit(LIST_LIMIT);
			Log.i("ids:",friendIds.toString());
            try {
            	switch(this.source){
            		case MORE:
            			query.whereContainedIn("facebookId", friendIds);	// limit visible checkins to posts made by the current user and his/her friends
            			query.setSkip(adapter.getCount());
                    	moreCheckinList = query.find();
                        break;
            		default:
            			query.whereContainedIn("facebookId", friendIds);	// limit visible checkins to posts made by the current user and his/her friends
            			checkinList = query.find();
                        break;
            	}
            } catch (ParseException e) {
            	Toast.makeText(CheckinListActivity.this,"Could not load checkins at the moment.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
			
			return null;
		}

		@Override
		protected void onPreExecute() {
			if(CheckinListActivity.this.progressDialog==null)
				CheckinListActivity.this.progressDialog = ProgressDialog.show(CheckinListActivity.this, "",
					"Loading...", true);
			 
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		/**
		 * Put the list of checkins into the view
		 */
		@Override
		protected void onPostExecute(Void result) {
			switch(this.source){
				case MORE:
					if(moreCheckinList!=null){
						if(moreCheckinList.size()!=0){
			                for (ParseObject parseObject : moreCheckinList)
			                    adapter.add(parseObject);
			                TextView empty = (TextView) findViewById(R.id.empty);
							empty.setVisibility(View.INVISIBLE);
						}else{
							// end of list has been reached
							lv.removeFooterView(btnLoadMore);
						}
		            }
					break;
					
				default:
					adapter = new CheckinListAdapter(CheckinListActivity.this, R.layout.checkin_row,checkinList);
					if(checkinList.size()!=0){
						TextView empty = (TextView) findViewById(R.id.empty);
						empty.setVisibility(View.INVISIBLE);
					}
					
					break;
			}
			
            setListAdapter(adapter);
            adapter.notifyDataSetChanged();
            if(CheckinListActivity.this.progressDialog!=null)
            	CheckinListActivity.this.progressDialog.dismiss();

            super.onPostExecute(result);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		lastKnownLocation = location;	
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	@Override
	public void onProviderEnabled(String provider) {}
	@Override
	public void onProviderDisabled(String provider) {}
}
