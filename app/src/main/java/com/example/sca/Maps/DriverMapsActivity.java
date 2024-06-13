package com.example.sca.Maps;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.example.sca.History.DriverHistoryActivity;
import com.example.sca.R;
import com.example.sca.Settings.DriverSettingsActivity;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback, RouteListener {
    public static final int LOCATION_REQUEST_CODE = 100;
    private  int status = 0;
    private int destinationMarkerStatus = 0;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap mMap;
    private Handler handler = new Handler();
    private Runnable runnable;
    private long refreshTime = 5000;
    private boolean updateLocation = false;
    private boolean isInitialUpdate = true;
    private Button mLogout, mDelieveryStatus, mRefresh, mCurrentBtn, mDestinationBtn, mCancel;
    private String customerID = "", destination;
    private boolean isLoggingOut = false, routeSwitch = false,isRouteSwitchDestiination = false;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomerPFP;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination;
    Location mCurrentLoc;
    LatLng mCurrentLatLng, mPickUpLatlng, destinationLatLng;
    private float deliveryDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);

        mCustomerInfo = findViewById(R.id.llCustomerInfo);
        mCustomerPFP = findViewById(R.id.ivCustomerPFP);
        mCustomerName = findViewById(R.id.tvCustomerName);
        mCustomerPhone = findViewById(R.id.tvCustomeNo);
        mCustomerDestination = findViewById(R.id.tvCustomerDestination);

        //mDelieveryStatus = mRideStatus
        mDelieveryStatus = findViewById(R.id.btnDeliveryStatus);
        mLogout = findViewById(R.id.btnLogout);
        mRefresh = findViewById(R.id.btnRefresh);
        mCurrentBtn = findViewById(R.id.btnCurrentMark);
        mDestinationBtn = findViewById(R.id.btnDestinationMark);
        mCancel = findViewById(R.id.btnCancel);

        mCurrentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRide();
            }
        });

        mDestinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (destinationMarkerStatus){
                    case 1:
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(mPickUpLatlng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                        break;
                    case 2:
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationLatLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                        break;
                    default:
                        Toast.makeText(DriverMapsActivity.this,
                                "No Pickup or Destination LatLng", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });

        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartActivity();
            }
        });


        mDelieveryStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){
                    case 1:
                        status = 2;
                        destinationMarkerStatus = 2;
                        routeSwitch = false;
                        isRouteSwitchDestiination = true;
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {

                        }

                        Toast.makeText(DriverMapsActivity.this,
                                "Deliver the goods to the destination",
                                Toast.LENGTH_LONG).show();
                        break;
                    case 2:
                        destinationMarkerStatus = 0;
                        Toast.makeText(DriverMapsActivity.this,
                                "Delivery Completed",
                                Toast.LENGTH_LONG).show();
                        isRouteSwitchDestiination = false;
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        mLogout.setOnClickListener(view -> {
            isLoggingOut = true;
            disconnectDriver();
            //FirebaseAuth.getInstance().signOut();
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getUserLocation();
        initializeRunnable();
        getAssignedCustomer();
    }

    private void recordRide() {
        String userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        DatabaseReference driverRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userID)
                .child("history");

        DatabaseReference customerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(customerID)
                .child("history");

        DatabaseReference historyRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("history");

        String requestID = historyRef.push().getKey();

        if (requestID != null) {
            driverRef.child(requestID).setValue(true);
            customerRef.child(requestID).setValue(true);

            HashMap<String, Object> map = new HashMap<>();
            map.put("driver", userID);
            map.put("customer", customerID);
            map.put("rating", 0);
            map.put("timestamp", getCurrentTimeStamp());
            map.put("destination", destination);
            map.put("location/from/lat", mPickUpLatlng.latitude);
            map.put("location/from/lng", mPickUpLatlng.longitude);
            map.put("location/to/lat", destinationLatLng.latitude);
            map.put("location/to/lng", destinationLatLng.longitude);
            map.put("distance", deliveryDistance);

            historyRef.child(requestID).updateChildren(map).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("RecordRide", "Ride recorded successfully.");
                } else {
                    Log.e("RecordRide", "Failed to record ride: ", task.getException());
                }
            });
        } else {
            Log.e("RecordRide", "Failed to generate requestID.");
        }

    }

    private Long getCurrentTimeStamp() {
        Long timeStamp = System.currentTimeMillis()/1000;
        return timeStamp;
    }

    private void endRide() {

        mDelieveryStatus.setText("Initiate Delivery");

        String userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

            DatabaseReference driverRef = FirebaseDatabase
                    .getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(userID)
                    .child("customerRequest");

            driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("customerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerID);

        customerID = "";
        deliveryDistance = 0;

        if(pickUpMarker != null){
            pickUpMarker.remove();;
        }
        if(assignedCustomerPickupLocationRefListener != null){
            assignedCustomerPickupLocationRef
                    .removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCancel.setVisibility(View.GONE);
        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerPFP.setImageResource(R.mipmap.ic_default_user_foreground);
        mCustomerDestination.setText("Destination: --");
        routeSwitch = false;

    }

    private void getAssignedCustomer() {

        String driverID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        DatabaseReference assignedCustomerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverID)
                .child("customerRequest")
                .child("customerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    status = 1;
                    destinationMarkerStatus = 1;
                    customerID = snapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerInfo();
                    getAssignedCustomerDestination();
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
    private void getAssignedCustomerDestination() {

        String driverID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        DatabaseReference assignedCustomerRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(driverID)
                .child("customerRequest");

        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if(map.get("destination")!= null){
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination: " + destination);
                    }
                    else{
                        mCustomerDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLong = 0.0;

                    if(map.get("destinationLat") != null){
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if(map.get("destinationLong") != null){
                        destinationLong = Double.valueOf(map.get("destinationLong").toString());
                        destinationLatLng = new LatLng(destinationLat,destinationLong);
                        Log.i("XOXO", "Lat " + destinationLatLng.latitude + "Lng" + destinationLatLng.longitude);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getAssignedCustomerInfo(){
        mCustomerInfo.setVisibility(View.VISIBLE);
        mCancel.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(customerID);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mCustomerName.setText("Name: " + map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        mCustomerPhone.setText("Phone No: " + map.get("phone").toString());
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
                setProfilePic(getBaseContext(), uri, mCustomerPFP);
            }
        });
    }
    private StorageReference getCurrentProfileImageStorageRef() {
        if (customerID == null || customerID.isEmpty()) {
            return null;
        }
        return FirebaseStorage.getInstance().getReference()
                .child("profile_image")
                .child(customerID);
    }
    private static void setProfilePic(Context context, Uri selectedImageUri, ImageView imageView){
        Glide.with(context)
                .load(selectedImageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }

    Marker pickUpMarker, destinationMarker;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("customerRequest")
                .child(customerID)
                .child("l");

        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && !customerID.equals("")){
                            List<Object> map = (List<Object>) snapshot.getValue();
                            double locationLat = 0;
                            double locationLong = 0;
                            if(map.get(0) != null){
                                locationLat = Double.parseDouble(map.get(0).toString());
                            }
                            if(map.get(1) != null){
                                locationLong = Double.parseDouble(map.get(1).toString());
                            }
                            LatLng pickupLatLng = new LatLng(locationLat,locationLong);

                            pickUpMarker = mMap.addMarker(new MarkerOptions()
                                    .position(pickupLatLng)
                                    .title("PickUp Location"));

                            mPickUpLatlng = pickupLatLng;

                            routeSwitch = true;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void getRoute(LatLng mCurrentLatLng, LatLng pickupLatLng) {
        RouteDrawing routeDrawing = new RouteDrawing.Builder()
                .context(DriverMapsActivity.this)
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this).alternativeRoutes(true)
                .waypoints(mCurrentLatLng, pickupLatLng)
                .build();
        routeDrawing.execute();
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

        if(!customerID.equals("")){
            deliveryDistance += mCurrentLoc.distanceTo(location)/1000;
        }

        mCurrentLoc = location;

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        LatLng userLocation = new LatLng(lat, lon);

        mCurrentLatLng = userLocation;

        if(routeSwitch){
            getRoute(mCurrentLatLng,mPickUpLatlng);
            pickUpMarker = mMap.addMarker(new MarkerOptions()
                    .position(mPickUpLatlng)
                    .title("PickUp Location"));
        }
        if(isRouteSwitchDestiination){
            getRoute(mCurrentLatLng,destinationLatLng);
            destinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title("Destination Location"));
            Location locOne = new Location("");
            locOne.setLatitude(mCurrentLatLng.latitude);
            locOne.setLongitude(mCurrentLatLng.longitude);

            Location locTwo = new Location("");
            locTwo.setLatitude(destinationLatLng.latitude);
            locTwo.setLongitude(destinationLatLng.longitude);

            float distance = locOne.distanceTo(locTwo);

            if(distance < 10){
                mDelieveryStatus.setText("Finish");
            }
            else{
                mDelieveryStatus.setText("Destination Distance: " + distance);
            }
        }

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Current Location"));
            if (isInitialUpdate) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
                isInitialUpdate = false;
            } else {
                String userID = FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid();
                DatabaseReference refAvaliable = FirebaseDatabase
                        .getInstance()
                        .getReference("driversAvailable");

                DatabaseReference refWorking = FirebaseDatabase
                        .getInstance()
                        .getReference("driversWorking");

                GeoFire geoFireAvailable = new GeoFire(refAvaliable);
                GeoFire geoFireWorking = new GeoFire(refWorking);

                switch (customerID){
                    case "":
                        geoFireWorking.removeLocation(userID);
                        geoFireAvailable.setLocation(userID, new GeoLocation(location
                                .getLatitude(),location.getLongitude()));
                        break;

                    default:
                        geoFireAvailable.removeLocation(userID);
                        geoFireWorking.setLocation(userID, new GeoLocation(location
                                .getLatitude(),location.getLongitude()));
                        break;
                }
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

    private void disconnectDriver(){
        handler.removeCallbacks(runnable);

        String userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userID);
    }


    @Override
    public void onRouteFailure(ErrorHandling errorHandling) {
        Toast.makeText(this, "Route Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRouteStart() {

    }

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> routeInfoModelArrayList,
                               int routeIndexing) {
        PolylineOptions polylineOptions = new PolylineOptions();
        ArrayList<Polyline> polylines = new ArrayList<>();
        for (int i = 0; i < routeInfoModelArrayList.size(); i++) {
            if (i == routeIndexing) {
                Log.e("TAG", "onRoutingSuccess: routeIndexing" + routeIndexing);
                polylineOptions.color(R.color.black);
                polylineOptions.width(12);
                polylineOptions.addAll(routeInfoModelArrayList.get(routeIndexing).getPoints());
                polylineOptions.startCap(new RoundCap());
                polylineOptions.endCap(new RoundCap());
                Polyline polyline = mMap.addPolyline(polylineOptions);
                polylines.add(polyline);
            }
        }

    }

    @Override
    public void onRouteCancelled() {
        Toast.makeText(this, "Route Cancer", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectDriver();
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

}