package com.example.sca.Homepage.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
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

public class CustomerHomeFragment extends Fragment {
    private ImageView mOpenMap, mProfilePhoto;
    private TextView mID, mNameTV, mPhoneNoTV;
    private DatabaseReference mCustomerDatabase;
    private String userID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_home, container, false);

        userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        mID = view.findViewById(R.id.tvID);
        mNameTV = view.findViewById(R.id.tvName);
        mPhoneNoTV = view.findViewById(R.id.tvPhoneNo);

        mOpenMap = view.findViewById(R.id.ivMapSearch);
        mProfilePhoto = view.findViewById(R.id.ivProfile);

        mOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CustomerMapsActivity.class);
                startActivity(intent);
            }
        });

        mCustomerDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userID);

        getUserInfo();

        return view;
    }

    private void getUserInfo() {

        if(userID != null){
            mID.setText("ID: " + truncateString(userID,15));
        }
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            String inName = mNameTV.getText().toString();
            String inPhone = mPhoneNoTV.getText().toString();
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        inName = map.get("name").toString();
                        mNameTV.setText("Name: " + truncateString(inName, 15));
                    }
                    if(map.get("phone") != null){
                        inPhone = map.get("phone").toString();
                        mPhoneNoTV.setText("Phone: " + truncateString(inPhone, 10));
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
    private static void setProfilePic(Context context, Uri selectedImageUri, ImageView imageView){
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