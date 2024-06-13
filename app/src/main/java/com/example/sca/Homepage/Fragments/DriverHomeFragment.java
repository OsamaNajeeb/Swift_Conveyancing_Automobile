package com.example.sca.Homepage.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.sca.Maps.CustomerMapsActivity;
import com.example.sca.Maps.DriverMapsActivity;
import com.example.sca.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Map;

public class DriverHomeFragment extends Fragment {
    private ImageView mOpenMap, mProfilePhoto;
    private TextView mID, mNameTV, mPhoneNoTV, mVehicleNo, mVehType;
    private DatabaseReference mCustomerDatabase;
    private String userID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);


        userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        mID = view.findViewById(R.id.tvID);
        mNameTV = view.findViewById(R.id.tvName);
        mPhoneNoTV = view.findViewById(R.id.tvPhoneNo);
        mVehicleNo = view.findViewById(R.id.tvVehicleDetail);
        mVehType = view.findViewById(R.id.tvVehicleType);

        mOpenMap = view.findViewById(R.id.ivMapSearch);
        mProfilePhoto = view.findViewById(R.id.ivProfile);

        mOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DriverMapsActivity.class);
                startActivity(intent);
            }
        });

        mCustomerDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userID);

        getUserInfo();

        return view;
    }

    private void getUserInfo() {

        if (userID != null) {
            mID.setText("ID: " + truncateString(userID, 15));
        }
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            String inName = mNameTV.getText().toString();
            String inPhone = mPhoneNoTV.getText().toString();
            String inRegistrationNo = mVehicleNo.getText().toString();
            String inVehicleType = mVehType.getText().toString();

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("name") != null) {
                        inName = map.get("name").toString();
                        mNameTV.setText("Name: " + truncateString(inName, 15));
                    }
                    if (map.get("phone") != null) {
                        inPhone = map.get("phone").toString();
                        mPhoneNoTV.setText("Phone: " + truncateString(inPhone, 10));
                    }
                    if (map.get("car") != null) {
                        inRegistrationNo = map.get("car").toString();
                        mVehicleNo.setText("Vehicle Registration: " + truncateString(inRegistrationNo, 9));
                    }
                    if (map.get("service") != null) {
                        inVehicleType = map.get("service").toString();
                        mVehType.setText("Vehicle Type: " + inVehicleType);
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
                setProfilePic(getContext(), uri, mProfilePhoto);
            }
        });
    }

    private static void setProfilePic(Context context, Uri selectedImageUri, ImageView imageView) {
        Glide.with(context)
                .load(selectedImageUri)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }

    private StorageReference getCurrentProfileImageStorageRef() {
        if (userID == null || userID.isEmpty()) {
            return null;
        }
        return FirebaseStorage.getInstance().getReference()
                .child("profile_image")
                .child(userID);
    }

    private String truncateString(String str, int maxLength) {
        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...";
        }
        return str;
    }
}