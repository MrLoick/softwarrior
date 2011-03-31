package com.softwarrior.cashpointviewer;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.location.Location;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlacemarkView extends LinearLayout implements GPSObserver, CompassObserver {
    private List<Placemark> mVisiblePlacemarks = new LinkedList<Placemark>();
    private TextView mTextView;
        
    private float mFOV;
    private int	mMaxDistanceKm;
    
    public PlacemarkView(Context context, float fov) {
		super(context);
		setOrientation(LinearLayout.VERTICAL);
		
		mFOV = fov;
	
		//Text view where placemark name will be shown
		mTextView = new TextView(context);
		mTextView.setTextSize(16.0f);
		mTextView.setTextColor(0xFFFF7F27);
		mTextView.setTypeface(Typeface.create((String)null, Typeface.BOLD));
		mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		addView(mTextView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		//mTextView.setText(pm.getName() + String.format("(%.1f km)", pm.getDistance() / 1000.0f));
	
		GPS.getInstance().addObserver(this);
		CompassSensor.getInstance().addObserver(this);
	
		mMaxDistanceKm = SettingsActivity.GetMaxDistanceKm(context);
		
		setWillNotDraw(false);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
    	super.onDraw(canvas);

		if(!mVisiblePlacemarks.isEmpty()) {
		    
		    float azimuth = CompassSensor.getInstance().getLastData();
		    int canvasWidth = canvas.getWidth();		    
		    // Draw placemarks
		    for(Placemark pm : mVisiblePlacemarks) {
				float a = pm.getBearing() - azimuth;
				int x = canvasWidth / 2 + angleToPixels(canvasWidth, a);
				int y = (canvas.getHeight() * 2 / 3); //default position
				float distance_km = pm.getDistance() / 1000.0f;
				float picture_scale_percent = 1; //100 %
				int canvas_height = (canvas.getHeight() * 2 / 3); //default position
				
				if(mMaxDistanceKm == SettingsActivity.INFINITY_KM){
					picture_scale_percent = 0.1f;
				}				
				if(distance_km < 1){
					picture_scale_percent = 1;
				} else if(distance_km > 1 && distance_km < 2){
					picture_scale_percent = 0.9f;
				}else if(distance_km > 2 && distance_km < 3){
					picture_scale_percent = 0.8f;
				}else if(distance_km > 3 && distance_km < 4){
					picture_scale_percent = 0.7f;
				}else if(distance_km > 4 && distance_km < 5){
					picture_scale_percent = 0.6f;					
				}else if(distance_km > 5 && distance_km < 6){
					picture_scale_percent = 0.5f;					
				}else if(distance_km > 6 && distance_km < 7){
					picture_scale_percent = 0.4f;					
				}else if(distance_km > 7 && distance_km < 8){
					picture_scale_percent = 0.3f;					
				}else if(distance_km > 8 && distance_km < 9){
					picture_scale_percent = 0.2f;					
				}else if(distance_km > 9){
					picture_scale_percent = 0.1f;					
				}
				y = (int)(canvas_height * picture_scale_percent);
				pm.SetResizePercent(picture_scale_percent);
				pm.draw(x, y, canvas);
		    }
		}
    }
    
    //Private methods
    private synchronized void updateVisiblePlacemarks() {	
		// Determine visibility, fill in the list
		LinkedList<Placemark> visiblePlacemarks = new LinkedList<Placemark>();
		List<Placemark> placemarks = PlacemarkCollection.getInstance().getNearestPlacemarks();
		float azimuth = CompassSensor.getInstance().getLastData();
		for (Placemark pm : placemarks) {
		    if (Math.abs(azimuth - pm.getBearing()) < mFOV)
			visiblePlacemarks.add(pm);
		}
		
		// Keep sorted by bearing
		Collections.sort(visiblePlacemarks, new Comparator<Placemark>() {
		    @Override
		    public int compare(Placemark p1, Placemark p2) {
			float b1 = p1.getBearing();
			float b2 = p2.getBearing();
			return b1 < b2 ? -1 : (b1 == b2 ? 0 : 1);
		    }
		});
		
		// Update only when actually changed
		if(!mVisiblePlacemarks.equals(visiblePlacemarks)) {
		    mVisiblePlacemarks = visiblePlacemarks;		    
		}
    }
    
    private int angleToPixels(int canvasWidth, float angle) {
    	return (int) (angle * canvasWidth / (mFOV * 2));
    }
    //GPS observer    
    @Override
    public void onAvailabilityChanged(boolean available) {
    }

    @Override
    public void onLocationChanged(Location location) {
		PlacemarkCollection.getInstance().updateNearestPlacemarks(location);
		updateVisiblePlacemarks();
		invalidate();
    }
    //Compass observer
    @Override
    public void onNewSensorData(int sensorType, Float azimuth) {
		updateVisiblePlacemarks();
		invalidate();
    }
}
