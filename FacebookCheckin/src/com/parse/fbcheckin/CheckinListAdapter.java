package com.parse.fbcheckin;

import java.util.Date;
import java.util.List;

import com.facebook.widget.ProfilePictureView;
import com.github.curioustechizen.ago.RelativeTimeTextView;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CheckinListAdapter extends ArrayAdapter<ParseObject>{
	private List<ParseObject> objects;

	private int resource;
	private Context context;

	public CheckinListAdapter(Context context, int resource, List<ParseObject> objects){
		super(context, resource, objects);
		this.resource = resource;
		this.context = context;
		this.objects = objects;
	}

	@Override
	public View getView(int pos, View view, ViewGroup viewGroup){
		View row = view;
		LayoutInflater layoutInflater = ((Activity)context).getLayoutInflater();
        row = layoutInflater.inflate(resource,viewGroup,false);

		ListItemHolder holder = null;

		holder = new ListItemHolder();
		holder.Checkin = objects.get(pos);
		holder.ProfilePicture = (ProfilePictureView)row.findViewById(R.id.userPicture);
		holder.Name = (TextView) row.findViewById(R.id.name);
		holder.Message = (TextView) row.findViewById(R.id.message);
		holder.Time = (RelativeTimeTextView) row.findViewById(R.id.time);

		setupItem(holder);
        row.setTag(holder);
		return row;
	}
	private void setupItem(ListItemHolder holder) {

        holder.Message.setText(holder.Checkin.getString("message") + " at " + holder.Checkin.getString("page_name"));
        try {
            holder.Name.setText(holder.Checkin.getParseUser("author").fetchIfNeeded().getString("name"));
        } catch (ParseException e) {
            holder.Name.setText("Facebook User");
            e.printStackTrace();
        }
        holder.ProfilePicture.setProfileId(holder.Checkin.getParseUser("author").getString("facebookId"));
        holder.Time.setReferenceTime(holder.Checkin.getCreatedAt().getTime());
	}
	public class ListItemHolder {
		ParseObject Checkin;

		ProfilePictureView ProfilePicture;
		TextView Name;
		TextView Message;
		RelativeTimeTextView Time;
	}

}
