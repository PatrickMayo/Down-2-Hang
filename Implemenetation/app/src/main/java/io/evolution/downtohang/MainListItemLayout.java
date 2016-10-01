package io.evolution.downtohang;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

/**
 * A MainListItemLayout, class representing a main list item. These list items
 * have an expanded and collapsed view which are switched between when the user
 * taps the list item.
 */
public class MainListItemLayout extends RelativeLayout implements OnClickListener {
    private User you;

    private final String NO_HANGOUT = "0";
    private final int AVAILABLE = 1;
    private final int BUSY = 0;
    private final int OFFLINE = -1;

    // Collapsed View
    private RelativeLayout mainListItemExpandedView;
    private RelativeLayout mainListItemCollapsedView;
    private ImageView userStatusImageView;
    private TextView usernameLabel;

    // Expanded View
    private ImageView userIconImageView;
    private TextView expandedUsernameLabel;
    private TextView expandedLocationLabel;


    private boolean expanded;
    private User user;

    /**
     * Constructor for a MainListItemLayout
     * @param context a context
     * @param user a user
     * @param expanded whether or not the view is expanded
     * @param appUser the current user of this app.
     */
    public MainListItemLayout(Context context, User user, boolean expanded, User appUser) {
        super(context);
        this.user = user;
        this.expanded = expanded;

        this.you = appUser;

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.main_list_layout,this,true);

        // Collapsed
        mainListItemCollapsedView = (RelativeLayout) findViewById(R.id.mainListItemCollapsedView);
        mainListItemCollapsedView.setOnClickListener(this);

        usernameLabel = (TextView) findViewById(R.id.usernameLabel);
        usernameLabel.setText(user.getUsername());

        userStatusImageView = (ImageView) findViewById(R.id.userStatusImageView);
        if(user.getStatus() < OFFLINE || user.getStatus() > AVAILABLE) {
            userStatusImageView.setImageResource(R.mipmap.gray_circle_question_icone_6920_128);
        }
        else if(!user.getHangoutStatus().equals(NO_HANGOUT)) {
            userStatusImageView.setImageResource(R.mipmap.orange_circle_icone_6032_128);
        }
        else if(user.getStatus().equals(AVAILABLE)) {
            userStatusImageView.setImageResource(R.mipmap.green_circle_icone_4156_128);
        }
        else if(user.getStatus().equals(BUSY)) {
            userStatusImageView.setImageResource(R.mipmap.red_circle_icone_5751_128);
        }
        else{
            userStatusImageView.setImageResource(R.mipmap.gray_circle_icone_6920_128);
        }

        // Expanded View
        mainListItemExpandedView = (RelativeLayout) findViewById(R.id.mainListItemExpandedView);
        mainListItemExpandedView.setOnClickListener(this);

        expandedUsernameLabel = (TextView) findViewById(R.id.expandedUsernameLabel);
        expandedUsernameLabel.setText(user.getUsername());

        userIconImageView = (ImageView) findViewById(R.id.userIconImageView);
        if(user.getStatus() < -1 || user.getStatus() > 2) {
            userIconImageView.setImageResource(R.mipmap.gray_circle_question_trans);
        }
        else if(!user.getHangoutStatus().equals(NO_HANGOUT)) {
            userIconImageView.setImageResource(R.mipmap.orange_trans);
        }
        else if(user.getStatus().equals(AVAILABLE)) {
            userIconImageView.setImageResource(R.mipmap.green_trans);
        }
        else if(user.getStatus().equals(BUSY)) {
            userIconImageView.setImageResource(R.mipmap.red_trans);
        }
        else {
            userIconImageView.setImageResource(R.mipmap.grey_trans);
        }
        userIconImageView.setBackgroundResource(R.mipmap.default_profile_icon);

        expandedLocationLabel = (TextView) findViewById(R.id.expandedLocationLabel);

        StringBuilder locationStringBuilder = new StringBuilder();

        float[] resultArray = new float[1];

        Location.distanceBetween(you.getLatitude(), you.getLongitude(), user.getLatitude(), user.getLongitude(), resultArray);

        double distanceFeet = resultArray[0] * 3.28084;

        DecimalFormat df = new DecimalFormat("#.##");


        //displays how far away your contacts are from you in feet/miles
        if (distanceFeet < 5280) {
            locationStringBuilder.append("Is ")
                    .append(df.format(distanceFeet))
                    .append(" feet away.")
                    .append("\n");
        }
        else {
            locationStringBuilder.append("Is ")
                    .append(df.format(distanceFeet/5280))
                    .append(" miles away.")
                    .append("\n");
        }

        expandedLocationLabel.setText(locationStringBuilder.toString());

        if (expanded) {
            expand();
        }
        else {
            collapse();
        }
    }


    /**
     * Expand the layout
     */
    public void expand() {
        expanded = true;
        mainListItemCollapsedView.setVisibility(View.GONE);
        mainListItemExpandedView.setVisibility(VISIBLE);
    }

    /**
     * Collapse the layout
     */
    public void collapse() {
        expanded = false;
        mainListItemCollapsedView.setVisibility(VISIBLE);
        mainListItemExpandedView.setVisibility(View.GONE);
    }

    /**
     * Given a view, perform the appropriate action when the view is clicked.
     * @param v a view
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.mainListItemExpandedView:
                collapse();
                break;
            case R.id.mainListItemCollapsedView:
                expand();
                break;
            default:
                break;
        }
    }
}
