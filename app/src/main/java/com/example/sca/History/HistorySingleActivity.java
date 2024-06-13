package com.example.sca.History;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.example.sca.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback,
        RouteListener {
    private String deliveryID, currentUserID, customerID, driverID, userDriverOrCustomer, dist;
    private TextView mDeliveryLoc, mDeliveryDist, mDeliveryDate, mUsername, mPhoneNo;
    private ImageView mUserPFP;
    private GoogleMap mMap;
    private DatabaseReference historyDeliveryInfoDB;
    private LatLng destinationLatLng, pickUpLatLng;
    private RatingBar mRatingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_single);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mDeliveryLoc = findViewById(R.id.tvDeliveryLocation);
        mDeliveryDist = findViewById(R.id.tvDeliveryDistance);
        mDeliveryDate = findViewById(R.id.tvDeliveryDate);
        mUsername = findViewById(R.id.tvUserName);
        mPhoneNo = findViewById(R.id.tvPhoneNo);

        mRatingBar = findViewById(R.id.rbRating);

        mUserPFP = findViewById(R.id.ivUserPFP);

        deliveryID = getIntent().getExtras().getString("deliveryID");

        currentUserID = FirebaseAuth
                            .getInstance()
                            .getCurrentUser()
                            .getUid();

        historyDeliveryInfoDB = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("history")
                .child(deliveryID);

        getDeliveryInfo();

        SupportMapFragment mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mMapFragment.getMapAsync(this);

    }

    private void getDeliveryInfo() {
        historyDeliveryInfoDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot child: snapshot.getChildren()){
                        if("customer".equals(child.getKey())){
                            customerID = child.getValue(String.class);
                            if(customerID != null && !customerID.equals(currentUserID)){
                                userDriverOrCustomer = "Drivers";
                                getUserInfo("Customers", customerID);
                            }
                        }
                        if("driver".equals(child.getKey())){
                            driverID = child.getValue(String.class);
                            if(driverID != null && !driverID.equals(currentUserID)){
                                userDriverOrCustomer = "Customers";
                                getUserInfo("Drivers", driverID);
                                displayCustomerRelatedObject();
                            }
                        }
                        if("timestamp".equals(child.getKey())){
                            Long timestamp = child.getValue(Long.class);
                            if (timestamp != null) {
                                mDeliveryDate.setText(getDate(timestamp));
                            }
                        }
                        if("rating".equals(child.getKey())){
                            mRatingBar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if("distance".equals(child.getKey())){
                            dist = child.getValue().toString();
                            mDeliveryDist.setText(dist.substring(0, Math.min(dist.length(),
                                    5)) + "/KM");
                        }
                        if("destination".equals(child.getKey())){
                            String destination = child.getValue(String.class);
                            if (destination != null) {
                                mDeliveryLoc.setText(destination);
                            }
                        }
                        if("location".equals(child.getKey())){
                            DataSnapshot fromSnapshot = child.child("from");
                            DataSnapshot toSnapshot = child.child("to");

                            if (fromSnapshot.exists() && toSnapshot.exists()) {
                                Double fromLat = fromSnapshot.child("lat")
                                        .getValue(Double.class);
                                Double fromLng = fromSnapshot.child("lng")
                                        .getValue(Double.class);
                                Double toLat = toSnapshot.child("lat")
                                        .getValue(Double.class);
                                Double toLng = toSnapshot.child("lng")
                                        .getValue(Double.class);

                                if (fromLat != null && fromLng != null) {
                                    pickUpLatLng = new LatLng(fromLat, fromLng);
                                }

                                if (toLat != null && toLng != null) {
                                    destinationLatLng = new LatLng(toLat, toLng);
                                }

                                if (destinationLatLng != null && !destinationLatLng
                                        .equals(new LatLng(0, 0))) {
                                    getRoute();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("getDeliveryInfo",
                        "Failed to read delivery info", error.toException());
            }
        });
    }

    private void displayCustomerRelatedObject() {
        mRatingBar.setVisibility(View.VISIBLE);
        mRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                // Store the rating as a float value
                historyDeliveryInfoDB.child("rating").setValue(rating).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("RatingUpdate", "Rating updated successfully.");
                    } else {
                        Log.e("RatingUpdate", "Failed to update rating.", task.getException());
                    }
                });

                DatabaseReference mDriverRatingDB = FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("Users")
                        .child("Drivers")
                        .child(driverID)
                        .child("rating");

                mDriverRatingDB.child(deliveryID).setValue(rating).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("DriverRatingUpdate", "Driver rating updated successfully.");
                    } else {
                        Log.e("DriverRatingUpdate", "Failed to update driver rating.", task.getException());
                    }
                });
            }
        });
    }


    private void getUserInfo(String userType, String userID) {
        DatabaseReference mUserTypeDB = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child(userType)
                .child(userID);

        mUserTypeDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mUsername.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        mPhoneNo.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString())
                                .into(mUserPFP);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String getDate(Long timeStamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timeStamp*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm",cal).toString();

        return date;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void getRoute() {
        RouteDrawing routeDrawing = new RouteDrawing.Builder()
                .context(HistorySingleActivity.this)
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(pickUpLatLng, destinationLatLng)
                .build();
        routeDrawing.execute();
    }

    @Override
    public void onRouteFailure(ErrorHandling errorHandling) {

    }

    @Override
    public void onRouteStart() {

    }

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> routeInfoModelArrayList,
                               int routeIndexing) {
        PolylineOptions polylineOptions = new PolylineOptions();
        ArrayList<Polyline> polylines = new ArrayList<>();

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickUpLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources()
                .getDisplayMetrics()
                .widthPixels;

        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);

        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions()
                .position(pickUpLatLng)
                .title("Pickup Location")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.truck)));

        mMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title("Drop Location"));

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

    }
}