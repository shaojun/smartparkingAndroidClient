package com.mycompany.myfirstindoorsapp;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.customlbs.coordinates.GeoCoordinate;
import com.customlbs.library.IndoorsException;
import com.customlbs.library.IndoorsFactory;
import com.customlbs.library.IndoorsLocationListener;
import com.customlbs.library.model.Building;
import com.customlbs.library.model.Zone;
import com.customlbs.shared.Coordinate;
import com.customlbs.surface.library.IndoorsSurfaceFactory;
import com.customlbs.surface.library.IndoorsSurfaceFragment;

/**
 * Sample Android project, powered by indoo.rs :)
 *
 * @author indoo.rs | Philipp Koenig
 *
 */
public class MainActivity extends FragmentActivity implements IndoorsLocationListener {

	private IndoorsSurfaceFragment indoorsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		IndoorsFactory.Builder indoorsBuilder = new IndoorsFactory.Builder();
		IndoorsSurfaceFactory.Builder surfaceBuilder = new IndoorsSurfaceFactory.Builder();
		indoorsBuilder.setContext(this);
		// TODO: replace this with your API-key
		indoorsBuilder.setApiKey("3ca6652c-f885-4ea3-8ed4-d53a4215f20a");
		// TODO: replace 12345 with the id of the building you uploaded to
		// our cloud using the MMT
		indoorsBuilder.setBuildingId((long) 336977324);
		// callback for indoo.rs-events
		indoorsBuilder.setUserInteractionListener(this);
		surfaceBuilder.setIndoorsBuilder(indoorsBuilder);
		indoorsFragment = surfaceBuilder.build();
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(android.R.id.content, indoorsFragment, "indoors");
		transaction.commit();
	}

	public void positionUpdated(Coordinate userPosition, int accuracy) {
		GeoCoordinate geoCoordinate = indoorsFragment.getCurrentUserGpsPosition();

		if (geoCoordinate != null) {
			Toast.makeText(
			    this,
			    "User is located at " + geoCoordinate.getLatitude() + ","
			    + geoCoordinate.getLongitude(), Toast.LENGTH_SHORT).show();
		}
	}

	public void buildingLoaded(Building building) {
		// indoo.rs SDK successfully loaded the building you requested and
		// calculates a position now
		Toast.makeText(
		    this,
		    "Building is located at " + building.getLatOrigin() / 1E6 + ","
		    + building.getLonOrigin() / 1E6, Toast.LENGTH_SHORT).show();
	}

	public void onError(IndoorsException indoorsException) {
		Toast.makeText(this, indoorsException.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void changedFloor(int floorLevel, String name) {
		// user changed the floor
	}

	public void leftBuilding(Building building) {
		// user left the building
	}

	public void loadingBuilding(int progress) {
		// indoo.rs is still downloading or parsing the requested building
	}

	public void orientationUpdated(float orientation) {
		// user changed the direction he's heading to
	}

	public void enteredZones(List<Zone> zones) {
		// user entered one or more zones
	}
}
