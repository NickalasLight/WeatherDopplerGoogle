package com.weather.doppler;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.gms.tasks.OnSuccessListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private boolean mPermissionDenied = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private int animationIterator;
    private GoogleMap mMap;
    private long lastUpdateTimeStamp;
    private Timer timer;
    private Timer animationTimer;
    private Timer waitLoadInTimer;
    private TimerTask timerTask;
    private TimerTask animationTimerTask;
    private TileOverlay tileOverlay;
    private TileOverlay[] tileOverlays;
    private FusedLocationProviderClient fusedLocationClient;
    private CameraPosition mlastCameraPosition;

    public long getLastTenMinutes(){

        long timeStamp = Instant.now().getEpochSecond();
        timeStamp = timeStamp - (timeStamp % 600);
        return timeStamp;

}
public boolean allTilesLoaded(){

    //for (int i = 0; i < tileOverlays.length; i++) {
    //    if(!tileOverlays[i].isVisible()){
    //        return false;
     //   }
   // }

    return true;
}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        getSupportActionBar().hide();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


public void animateMap(Activity activity) {

    activity.runOnUiThread(new Runnable() {
        public void run() {

            try {
                if (allTilesLoaded()) {
                    for (int i = 0; i < tileOverlays.length; i++) {

                        if (tileOverlays[i].getTransparency() == 0 && i < tileOverlays.length - 1) {
                            //tileOverlays[i].setVisible(false);
                            tileOverlays[i].setTransparency(1);
                            //tileOverlays[i + 1].setVisible(true);
                            tileOverlays[i + 1].setTransparency(0);
                            break;
                        } else if (tileOverlays[i].getTransparency() == 0 && i == tileOverlays.length - 1) {
                            // tileOverlays[i].setVisible(false);
                            // tileOverlays[0].setVisible(true);
                            tileOverlays[i].setTransparency(1);
                            tileOverlays[0].setTransparency(0);
                            break;
                        }
                    }
                }
            }
            catch(Exception ex){ex.printStackTrace();}

        }
    });

}

public TileProvider buildTileProvider(final long timeStamp) {
    TileProvider tileProvider = new UrlTileProvider(256, 256) {
        @Override
        public URL getTileUrl(int x, int y, int zoom) {

            /* Define the URL pattern for the tile images */
            //https://tilecache.rainviewer.com/v2/radar/1561885800/512/2/1/1/2/1_1.png
         //   long timeStamp = Instant.now().getEpochSecond();
          //  timeStamp = timeStamp - (timeStamp % 600);
           // lastUpdateTimeStamp = timeStamp;


            String s = String.format("https://tilecache.rainviewer.com/v2/radar/%d/256/%d/%d/%d/2/1_1.png",
                    timeStamp,zoom, x, y);

            if (!checkTileExists(x, y, zoom)) {
                return null;
            }

            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }

        /*
         * Check that the tile server supports the requested x, y and zoom.
         * Complete this stub according to the tile range you support.
         * If you support a limited range of tiles at different zoom levels, then you
         * need to define the supported x, y range at each zoom level.
         */
        private boolean checkTileExists(int x, int y, int zoom) {
            int minZoom = 0;
            int maxZoom = 16;

            if ((zoom < minZoom || zoom > maxZoom)) {
                return false;
            }

            return true;
        }
    };

    return tileProvider;

}

    public void updateMap(Activity activity) {


        long timeStamp = getLastTenMinutes();

        if (timeStamp > lastUpdateTimeStamp && tileOverlay != null) {

            activity.runOnUiThread(new Runnable() {
                public void run() {
                    for(TileOverlay tile:tileOverlays)
                        tile.clearTileCache();


                }
            });
        }
    }

    public void onPause(){
        super.onPause();
        animationTimer.cancel();
        timer.cancel();
        mMap.stopAnimation();
       // mlastCameraPosition = mMap.getCameraPosition();
        //TODO: Save last camera position to a file used by the application. onfinish() destroys the activity, so save
    }

    public void onResume(){
        super.onResume();
        try {


try {

    Context context = getApplicationContext();
    SharedPreferences sharedPref = context.getSharedPreferences("TEST_PREF",Context.MODE_PRIVATE);

    Double latitude = Double.parseDouble(sharedPref.getString(getString(R.string.saved_camera_lat),"0.0"));
    Double longitude =  Double.parseDouble(sharedPref.getString(getString(R.string.saved_camera_long),"0.0"));
    float zoom = Float.parseFloat(sharedPref.getString(getString(R.string.saved_camera_zoom),"0.0"));
    float bearing = Float.parseFloat(sharedPref.getString(getString(R.string.saved_camera_bearing),"0.0"));
    float tilt = Float.parseFloat(sharedPref.getString(getString(R.string.saved_camera_tilt),"0.0"));

   mlastCameraPosition = new CameraPosition(new LatLng(latitude, longitude), zoom, tilt, bearing);
}
catch(
        Exception ex){ex.printStackTrace();
}

            if(mlastCameraPosition != null && mMap != null){
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mlastCameraPosition));}

            //update map if resuming app after over 10 minutes away.
            if( lastUpdateTimeStamp < getLastTenMinutes()) {
                updateMap(MapsActivity.this);
            }

            animationTimer = new Timer();
            animationTimerTask = new TimerTask(){
                @Override
                public void run() {

                    animateMap(MapsActivity.this);
                }
            };

            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {

try {

    long timeStamp = getLastTenMinutes();

    if (timeStamp > lastUpdateTimeStamp) {

    updateMap(MapsActivity.this);
    lastUpdateTimeStamp = timeStamp;

    }
}
                    catch(Exception ex){
    ex.printStackTrace();
                    }
                }
            };
            timer.schedule(timerTask, 30000, 30000);
            timer.schedule(animationTimerTask, 150,150);

        } catch (IllegalStateException e){
            android.util.Log.i("Dang:", "resume error");
        }
    }

    public void onStop(){
        CameraPosition myCam = mMap.getCameraPosition();
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences("TEST_PREF",Context.MODE_PRIVATE);
        //.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(getString(R.string.saved_camera_lat), String.valueOf(myCam.target.latitude));
        editor.putString(getString(R.string.saved_camera_long), String.valueOf(myCam.target.longitude));
        editor.putString(getString(R.string.saved_camera_zoom),String.valueOf(myCam.zoom));
        editor.putString(getString(R.string.saved_camera_bearing),String.valueOf(myCam.bearing));
        editor.putString(getString(R.string.saved_camera_tilt),String.valueOf(myCam.tilt));
      
        editor.commit();
        super.onStop();
        finish();
    }





//tileOverlay.clearTileCache(); every ten minutes.



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
        if(mlastCameraPosition != null) {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mlastCameraPosition));}
try {
    boolean success = googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json));
}
catch(Exception ex){
    ex.printStackTrace();

        }
        TileProvider tileProvider = buildTileProvider(getLastTenMinutes()-6000);
        TileProvider tileProvider2 = buildTileProvider(getLastTenMinutes()-5400);
        TileProvider tileProvider3 = buildTileProvider(getLastTenMinutes()-4800);
        TileProvider tileProvider4 = buildTileProvider(getLastTenMinutes()-4200);
        TileProvider tileProvider5 = buildTileProvider(getLastTenMinutes()-3600);
        TileProvider tileProvider6 = buildTileProvider(getLastTenMinutes()-3000);
        TileProvider tileProvider7 = buildTileProvider(getLastTenMinutes()-2400);
        TileProvider tileProvider8 = buildTileProvider(getLastTenMinutes()-1800);
        TileProvider tileProvider9 = buildTileProvider(getLastTenMinutes()-1200);
        TileProvider tileProvider10 = buildTileProvider(getLastTenMinutes()-600);
        TileProvider tileProvider11 = buildTileProvider(getLastTenMinutes());


        TileProvider[] tileProvidersArray = new TileProvider[]{tileProvider,tileProvider2,tileProvider3,tileProvider4,tileProvider5,tileProvider6,tileProvider7,tileProvider8,tileProvider9,tileProvider10,tileProvider11};

        TileOverlay[] tileOverlaysArray = new TileOverlay[tileProvidersArray.length];



        try {
            for (int i = 0; i < tileProvidersArray.length; i++) {
                tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                        .tileProvider(tileProvidersArray[i]));


                tileOverlay.setFadeIn(true);
                tileOverlay.setVisible(true);
                tileOverlay.setTransparency(1);

          if(i == 0)
              tileOverlay.setTransparency(0);
    //            else
      //              tileOverlay.setVisible(false);
                tileOverlaysArray[i] = tileOverlay;

            }

            tileOverlays = tileOverlaysArray;
//TODO: add animate tileOverlays method.
           // tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
               //     .tileProvider(tileProvider2));
           // tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                //  .tileProvider(tileProvider3));
          // tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                  //  .tileProvider(tileProvider4));
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }


        // Add a marker in Sydney and move the camera
       // LatLng sydney = new LatLng(-34, 151);
       // mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));



        }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "Centering Map on Your Current Location", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),location.getLongitude())));
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(5));
                            }
                        }
                    });
        }
        catch(SecurityException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
           // mPermissionDenied = true;
        }
    }

}

