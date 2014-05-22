package com.parse.fbcheckin;

import android.app.Application;

import com.facebook.model.GraphPlace;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

public class FacebookCheckinApplication extends Application {

	static final String TAG = "log tag";
    static final int ACTIVITY_CREATE = 0;

    private GraphPlace selectedPlace;
	@Override
	public void onCreate() {
		super.onCreate();        
		//Initialize parse and facebook
		Parse.initialize(this, "wQuW1bXGTgbKunMT4NO13QaLgcxzk8KagHHlzzuj", "aMvWCPmtWXgE5fnYaqvskV7R6h8UQruK5oVLw8IV");
		ParseFacebookUtils.initialize(getString(R.string.app_id));
	}

    public GraphPlace getSelectedPlace() {
        return selectedPlace;
    }

    public void setSelectedPlace(GraphPlace selectedPlace) {
        this.selectedPlace = selectedPlace;
    }
}
