package com.example.sca.Maps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.sca.History.DriverHistoryActivity;
import com.example.sca.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public static final int LOCATION_REQUEST_CODE = 100;
    private boolean driverDetected = false;
    private RatingBar mRatingBar;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private Handler handler = new Handler();
    private Runnable runnable;
    private long refreshTime = 500;
    private boolean updateLocation = false;
    private boolean isInitialUpdate = true;
    private boolean isPickupMarker = false;
    private Button mLogout, mRequest, mCurrentBTN, mDriverBTN;
    Location mLastLocation;
    private LatLng pickUpLocation;
    private boolean requestBol = false;
    private Marker pickUpMarker;
    private String destination, reqService;
    private String TAG = "XOXO";
    private LinearLayout mDriverInfo;
    private ImageView mDriverPFP;
    private TextView mDriverName, mDriverPhone,mDriverCar;
    private RadioGroup mRadioVehicleType;
    private LatLng destinationLatLng, mCurrentLatLng, mDriverLatLng;
    private List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);

        destinationLatLng = new LatLng(0.0,0.0);

        mLogout = findViewById(R.id.btnLogout);
        mRequest = findViewById(R.id.btnSummon);
        mCurrentBTN = findViewById(R.id.btnCurrentMark);
        mDriverBTN = findViewById(R.id.btnDriverMark);


        mDriverInfo = findViewById(R.id.llDriverInfo);
        mDriverPFP = findViewById(R.id.ivDriverPFP);
        mDriverName = findViewById(R.id.tvDriverName);
        mDriverPhone = findViewById(R.id.tvDriverNo);
        mDriverCar = findViewById(R.id.tvDriverCar);

        mRadioVehicleType = findViewById(R.id.rgVehicleType);
        mRadioVehicleType.check(R.id.rbPickUp);

        mRatingBar = findViewById(R.id.rbRating);

        mCurrentBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
            }
        });

        mDriverBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(driverDetected){
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mDriverLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                }
                else{
                    Toast.makeText(CustomerMapsActivity.this,
                            "No Connected Drivers Detected", Toast.LENGTH_LONG).show();
                }
            }
        });

        mLogout.setOnClickListener(view -> finish());

        mRequest.setOnClickListener(view -> {
            if(requestBol){
                endRide();
            }
            else if(destinationLatLng.latitude == 0.0 && destinationLatLng.longitude == 0.0){
                Toast.makeText(this, "Please Pick Destination", Toast.LENGTH_LONG).show();
            }else{

                int selectID = mRadioVehicleType.getCheckedRadioButtonId();

                final RadioButton radioButton = findViewById(selectID);

                if(radioButton.getText() == null){
                    return;
                }

                reqService = radioButton.getText().toString();

                requestBol = true;
                String userID = FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid();

                DatabaseReference ref = FirebaseDatabase
                        .getInstance()
                        .getReference("customerRequest");

                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userID, new GeoLocation(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude()));

                pickUpLocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                isPickupMarker = true;

                mRequest.setText("Finding Your Driver...");

                getClosestDriver();
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        initializeRunnable();

        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getString(R.string.my_api_key));
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destination = place.getName();
                destinationLatLng = place.getLatLng();
                Log.i(TAG, "Place: " + place.getName() + ", ID: " + place.getId() + "," +
                        " LatLng: " + place.getLatLng());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundID;
    GeoQuery geoQuery;
    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude,
                pickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String s, GeoLocation geoLocation) {
                if(!driverFound && requestBol){

                    DatabaseReference mCustomerDatabase = FirebaseDatabase
                            .getInstance()
                            .getReference()
                            .child("Users")
                            .child("Drivers")
                            .child(s);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                                Map<String, Object> driverMap = (Map<String, Object>) snapshot.getValue();
                                if(driverFound){
                                    return;
                                }
                                if(driverMap.get("service").equals(reqService)){
                                    driverFound = true;
                                    driverFoundID = snapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase
                                            .getInstance()
                                            .getReference()
                                            .child("Users")
                                            .child("Drivers")
                                            .child(driverFoundID)
                                            .child("customerRequest");

                                    String customerID = FirebaseAuth
                                            .getInstance()
                                            .getCurrentUser()
                                            .getUid();

                                    HashMap map = new HashMap();
                                    map.put("customerRideID",customerID);
                                    map.put("destination",destination);
                                    map.put("destinationLat",destinationLatLng.latitude);
                                    map.put("destinationLong",destinationLatLng.longitude);
                                    driverRef.updateChildren(map);

                                    getDriverLocation();
                                    getDriverInfo();
                                    getHasRideEnded();
                                    mRequest.setText("Driver Location?");
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }

            }

            @Override
            public void onKeyExited(String s) {

            }

            @Override
            public void onKeyMoved(String s, GeoLocation geoLocation) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError databaseError) {

            }
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mDriverName.setText("Name: "+map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        mDriverPhone.setText("Phone No: "+map.get("phone").toString());
                    }
                    if(map.get("car") != null){
                        mDriverCar.setText("Car Plate: "+map.get("phone").toString());
                    }

                    int ratingSum = 0;
                    float ratingAvg = 0, ratingTotal = 0;

                    for(DataSnapshot child : snapshot.child("rating").getChildren()){
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }
                    if(ratingTotal != 0){
                        ratingAvg = ratingSum / ratingTotal;
                        mRatingBar.setRating(ratingAvg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        getCurrentProfileImageStorageRef().getDownloadUrl().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Uri uri = task.getResult();
                setProfilePic(getBaseContext(), uri, mDriverPFP);
            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;

    private void getHasRideEnded() {

        driveHasEndedRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverFoundID)
                .child("customerRequest")
                .child("customerRideID");

        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){

                }
                else{
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void endRide() {
        requestBol = false;
        geoQuery.removeAllListeners();
        driverLocationRef.removeEventListener(driverLocationRefListener);
        driveHasEndedRef.removeEventListener(driveHasEndedRefListener);

        if(driverFoundID != null){
            DatabaseReference driverRef = FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(driverFoundID)
                    .child("customerRequest");
            driverRef.removeValue();

            driverFoundID = null;

        }

        driverFound = false;
        radius = 1;

        String userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("customerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID);

        if(pickUpMarker != null){
            pickUpMarker.remove();;
        }
        mRequest.setText("Call Driver");

        mDriverInfo.setVisibility(View.GONE);
        mDriverName.setText("");
        mDriverPhone.setText("");
        mDriverPFP.setImageResource(R.mipmap.ic_default_user_foreground);
        mDriverCar.setText("");
        mMap.clear();
        driverDetected = false;
    }

    private StorageReference getCurrentProfileImageStorageRef() {
        if (driverFoundID == null || driverFoundID.isEmpty()) {
            return null;
        }
        return FirebaseStorage.getInstance().getReference()
                .child("profile_image")
                .child(driverFoundID);
    }
    private static void setProfilePic(Context context, Uri selectedImageUri, ImageView imageView){
        Glide.with(context)
                .load(selectedImageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("driversWorking")
                .child(driverFoundID)
                .child("l");

        driverLocationRefListener = driverLocationRef
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && requestBol){
                            List<Object> map = (List<Object>) snapshot.getValue();
                            double locationLat = 0;
                            double locationLong = 0;
                            mRequest.setText("Driver Found");
                            if(map.get(0) != null){
                                locationLat = Double.parseDouble(map.get(0).toString());
                            }
                            if(map.get(1) != null){
                                locationLong = Double.parseDouble(map.get(1).toString());
                            }
                            LatLng driverLatLng = new LatLng(locationLat,locationLong);
                            if(mDriverMarker != null){
                                mDriverMarker.remove();
                            }

                            Location locOne = new Location("");
                            locOne.setLatitude(pickUpLocation.latitude);
                            locOne.setLongitude(pickUpLocation.longitude);

                            Location locTwo = new Location("");
                            locTwo.setLatitude(driverLatLng.latitude);
                            locTwo.setLongitude(driverLatLng.longitude);

                            float distance = locOne.distanceTo(locTwo);

                            if(distance < 100){
                                mRequest.setText("Driver Has Arrived");
                            }
                            else{
                                mRequest.setText("Driver Found: " + distance);
                            }

                            if (markers.size() >= 1) {
                                // Remove the previous marker(s)
                                for (Marker marker : markers) {
                                    marker.remove();
                                }
                                // Clear the list
                                markers.clear();
                            }

                            mDriverLatLng = driverLatLng;

                            mDriverMarker = mMap.addMarker(new MarkerOptions()
                                    .position(driverLatLng)
                                    .title("Your Driver is Here")
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.drawable.truck)));

                            markers.add(mDriverMarker);

                            driverDetected = true;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void initializeRunnable() {
        runnable = new Runnable() {
            @Override
            public void run() {
                getUserLocation();
                if (updateLocation) {
                    handler.postDelayed(this, refreshTime);
                }
            }
        };
    }

    private void toggleLocationUpdates() {
        if (updateLocation) {
            handler.removeCallbacks(runnable);
            updateLocation = false;
        } else {
            updateLocation = true;
            handler.postDelayed(runnable, 0);
            isInitialUpdate = true;
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                updateMap(location);
            }
        });
    }

    private void updateMap(Location location) {
        mLastLocation = location;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        LatLng userLocation = new LatLng(lat, lon);

        mCurrentLatLng = userLocation;

        if (mMap != null) {
            if(isPickupMarker){
                pickUpMarker = mMap.addMarker(new MarkerOptions()
                        .position(userLocation).title("PickUp Here"));
            }
            else{
                mMap.addMarker(new MarkerOptions().position(userLocation).title("Current Location"));
            }
            if (isInitialUpdate) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                isInitialUpdate = false;
            } else {
                //Loop Update
            }
        }
    }

    private void requestForPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            toggleLocationUpdates();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        requestForPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (requestBol) {
            requestBol = false;
            geoQuery.removeAllListeners();
            driverLocationRef.removeEventListener(driverLocationRefListener);

            if (driverFoundID != null) {
                DatabaseReference driverRef = FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("Users")
                        .child("Drivers")
                        .child(driverFoundID);

                driverRef.setValue(true);
                driverFoundID = null;

            }

            driverFound = false;
            radius = 1;

            String userID = FirebaseAuth
                    .getInstance()
                    .getCurrentUser()
                    .getUid();

            DatabaseReference ref = FirebaseDatabase
                    .getInstance()
                    .getReference("customerRequest");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.removeLocation(userID);

            if (pickUpMarker != null) {
                pickUpMarker.remove();
            }
            mRequest.setText("Call Driver");
        }
    }
}