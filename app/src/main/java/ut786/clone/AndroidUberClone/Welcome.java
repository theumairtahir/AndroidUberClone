package ut786.clone.AndroidUberClone;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ut786.clone.AndroidUberClone.Common.Common;
import ut786.clone.AndroidUberClone.Remote.IGoogleApi;

import static ut786.clone.AndroidUberClone.R.id.location_switch;

//import android.location.LocationListener;

public class Welcome extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;

    //Play Services
    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static final int PLAY_SERVICE_RES_REQUEST=7001;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static int UPDATE_INTERVAL=5000;
    private static int FASTEST_INTERVAL=3000;
    private static int DISPLACEMENT=10;

    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;
    Switch locationSwitch;
    SupportMapFragment mapFragment;

    //for car animation
    private List<LatLng> lstPolyLine;
    private Marker carMarker;
    float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition,endPosition,currentPosition;
    private int index,next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions greyPolylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;
    private IGoogleApi mService;
    Runnable drawPathRunnable=new Runnable() {
        @Override
        public void run() {
            //this code will provide the animation to the car on map
            if(index<lstPolyLine.size()-1){
                //if there's no route
                index++;
                next=index+1;
            }
            if(index<lstPolyLine.size()-1){
                startPosition=lstPolyLine.get(index);
                endPosition=lstPolyLine.get(next);
            }
            final ValueAnimator valueAnimator=ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    //updates the animation position wrt the position
                    v=valueAnimator.getAnimatedFraction();
                    //calculates the updating points
                    lng=v*endPosition.longitude+(1-v)+startPosition.longitude;
                    lat=v*endPosition.latitude+(1-v)*startPosition.latitude;
                    LatLng newPos=new LatLng(lng,lat); //new location
                    //set marker to the new location
                    carMarker.setPosition(newPos);
                    carMarker.setAnchor(0.5f,0.5f);
                    carMarker.setRotation(getBearing(startPosition,newPos));
                    //move camera wrt to the cab pointer
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(newPos).zoom(15.5f).build()));
                }
            });
            valueAnimator.start();
            handler.postDelayed(this,3000); //creates a delay of updation
        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        //method to get thee rotation angle of the cab marker
        double lat =Math.abs(startPosition.latitude-endPosition.latitude);
        double lng=Math.abs(startPosition.longitude-endPosition.longitude);
        if(startPosition.latitude<endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        }
        else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude<endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        }
        else if(startPosition.latitude>=endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);
        }
        else if(startPosition.latitude<endPosition.latitude && startPosition.longitude>=endPosition.longitude){
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //initiate view
        locationSwitch= findViewById(location_switch);
        btnGo= findViewById(R.id.btnGo);
        edtPlace= findViewById(R.id.edtPlace);
        lstPolyLine=new ArrayList<>();
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination=edtPlace.getText().toString();
                destination.replace(" ","+"); //replace spaces to + to fetch data
                Log.d("ut786",destination);
                getDirection();

            }
        });
        locationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOnline) {
                if(isOnline)
                {
                    startLocationUpdates();
                    displayLocation();
                    locationSwitch.setText("Online");
                    Toast.makeText(Welcome.this,"You are Online",Toast.LENGTH_SHORT);
                }
                else{
                    stopLocationUpdates();
                    mCurrent.remove();
                    mMap.clear();
                    ///Will be uncommented after the issues resolved
                    //handler.removeCallbacks(drawPathRunnable);
                    locationSwitch.setText("Offline");
                    Toast.makeText(Welcome.this,"You are Offline",Toast.LENGTH_SHORT);
                }
            }
        });
        //START OF DATABASE ACTIVITY
        //Geo Fire Settings
        drivers= FirebaseDatabase.getInstance().getReference("Drivers"); //CREATES DRIVER INSTANCE IN FIREBASE DATABASE
        geoFire=new GeoFire(drivers); //update location on the database
        //END OF DATABASE ACTIVITY
        setUpLocation(); //set the location of the driver on the map
    }

    private void getDirection() {
        //Method to get direction to some destination
        currentPosition = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()); //getting the current position from the db
        String requestApi=null; //API request to get the routes and direction
        try{
            requestApi="https://maps.googleapis.com/maps/api/directions/json?mode=driving&transit_routing_preference=less_driving&origin="+currentPosition.latitude+","+currentPosition.longitude+"&destination="+destination+"&key="+getResources().getString(R.string.google_direction_api);
            Log.d("ut786",requestApi); //print into the log ot test
            /*rest is the code to get the API response
            in the form of JSON Object
            and convert to the android object
             */
            mService.gotPath(requestApi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray=jsonObject.getJSONArray("routes");
                        for(int i=0; i<jsonArray.length();i++){
                            JSONObject route=jsonArray.getJSONObject(i);
                            JSONObject poly=route.getJSONObject("overview_polyline");
                            String polyline=poly.getString("points");
                            lstPolyLine=decodePoly(polyline); //decoding route points into poly line list
                        }
                        //adjusting bounds
                        LatLngBounds.Builder builder=new LatLngBounds.Builder(); //building the location bounds
                        for(LatLng latLng:lstPolyLine)
                            builder.include(latLng);
                        LatLngBounds bounds=builder.build(); //assigning the bounds
                        CameraUpdate mCameraUpdate=CameraUpdateFactory.newLatLngBounds(bounds,2); //updates the camera according to the bounds
                        mMap.animateCamera(mCameraUpdate); //camera animation
                        //polyline settings for grey polyline
                        greyPolylineOptions =new PolylineOptions();
                        greyPolylineOptions.color(Color.GRAY);
                        greyPolylineOptions.width(5);
                        greyPolylineOptions.startCap(new SquareCap());
                        greyPolylineOptions.endCap(new SquareCap());
                        greyPolylineOptions.jointType(JointType.ROUND);
                        greyPolylineOptions.addAll(lstPolyLine);
                        greyPolyline =mMap.addPolyline(greyPolylineOptions);
                        //polyline settings for black polyline
                        blackPolylineOptions =new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK); //route color
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.endCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolyline =mMap.addPolyline(blackPolylineOptions);

                        mMap.addMarker(new MarkerOptions().position(lstPolyLine.get(lstPolyLine.size()-1)).title("Pickup Location"));

                        //animation
                        ValueAnimator polylineAnimator=ValueAnimator.ofInt(0,100);
                        polylineAnimator.setDuration(2000);
                        polylineAnimator.setInterpolator(new LinearInterpolator());
                        polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                List<LatLng> points=greyPolyline.getPoints();
                                int percentValue=(int)animation.getAnimatedValue();
                                int size=points.size();
                                int newPoints=(int)(size*percentValue/100.0f);
                                List<LatLng> p=points.subList(0,newPoints);
                                blackPolyline.setPoints(p);
                            }
                        });
                        polylineAnimator.start();
                        carMarker=mMap.addMarker(new MarkerOptions().position(currentPosition).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.cab_marker)));


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(Welcome.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private List decodePoly(String encoded) {
        /*
        Method to decode polyline from JSON Object downloaded from gitHub
        https://github.com/gripsack/android/blob/master/app/src/main/java/com/github/gripsack/android/data/model/DirectionsJSONParser.java
         */
        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void setUpLocation() {
        //method to set the current location of the driver
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            //Request runtime permission
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_CODE);
        }
        else {
            //when request granted
            if(checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();
                if(locationSwitch.isChecked()){
                    displayLocation();
                }
            }
        }
    }
    private void buildGoogleApiClient(){
        //connect to the google location API to get the location
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();

    }
    private void createLocationRequest() {
        //getting the location updates
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        //METHOD TO CHECK THE PLAY SERVICES ON THE DRIVER'S PHONE
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode!=ConnectionResult.SUCCESS){
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICE_RES_REQUEST).show();
            else{
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void stopLocationUpdates() {
        //method to stop getting location updates
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,(com.google.android.gms.location.LocationListener) this);
    }

    private void displayLocation() {
        //METHOD TO SHOW THE CURRENT LOCATION ON THE MAP WITH A CAB MARKER
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            //if permission of accessing location is not granted the method stops here
            return;
        }
        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient); //getting the last location of the driver stored on the cloud
        if(mLastLocation!=null){
            if(locationSwitch.isChecked()){
                final double LONGITUDE=mLastLocation.getLongitude();
                final double LATITUDE=mLastLocation.getLatitude();
                //update the location to firebase
                //START OF DATABASE ACTIVITY
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(LATITUDE, LONGITUDE), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(mCurrent!=null){
                            mCurrent.remove();
                        }
                        mCurrent=mMap.addMarker(new MarkerOptions().position(new LatLng(LATITUDE,LONGITUDE)).title("Your Location"));
                        //move camera to upper described position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(LATITUDE,LONGITUDE),15.0f));
                        //draw animation rotate marker
                        //rotateMarker(mCurrent,-360,mMap);
                    }
                });
                //END OF DATABASE ACTIVITY
            }
        }
        else
            Log.d("Error","Can't get your Location");

    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        //METHOD TO SET THE MARKER ROTATION ACCORDING TO THE DIRECTIONS
        final Handler handler=new Handler();
        final long start= SystemClock.uptimeMillis();
        final float startRotation=mCurrent.getRotation();
        final long duration=1500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed =SystemClock.uptimeMillis()-start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot=t*i+(1-t)*startRotation;
                mCurrent.setRotation(-rot>180?rot/2:rot);
                if(t<1.0){
                    handler.postDelayed(this,16);
                }
            }
        });
    }

    private void startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(checkPlayServices()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        if(locationSwitch.isChecked()){
                            displayLocation();
                        }
                    }
                }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //map settings
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location; //save current location to last location
        displayLocation();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
