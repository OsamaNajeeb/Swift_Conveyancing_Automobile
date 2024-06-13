package com.example.sca.Homepage.Fragments;

import static android.app.ProgressDialog.show;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.sca.Maps.CustomerMapsActivity;
import com.example.sca.R;
import com.example.sca.Settings.CustomerSettingsActivity;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class CustomerProfileFragment extends Fragment {

    private EditText mNameET, mPhoneNoET;
    private Button mConfirm;
    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;
    private String userID;
    private String mName;
    private String mPhoneNo;
    ActivityResultLauncher<Intent> imagePicker;
    Uri selectedImageUri;
    private ImageView mProfieImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);

        imagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            setProfilePic(getContext(), selectedImageUri, mProfieImage);
                        }
                    }
                });

        mNameET = view.findViewById(R.id.etUsername);
        mPhoneNoET  = view.findViewById(R.id.etPhoneNo);
        mConfirm  = view.findViewById(R.id.btnConfirm);
        mProfieImage = view.findViewById(R.id.ivProfile);
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userID);
        getUserInfo();
        mConfirm.setOnClickListener(view1 -> saveUserInfo());

        mProfieImage.setOnClickListener((v) ->{
            ImagePicker
                    .with(this)
                    .cropSquare()
                    .compress(512)
                    .maxResultSize(512, 512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePicker.launch(intent);
                            return null;
                        }
                    });
        });

        return view;
    }

    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if(map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameET.setText(mName);
                    }
                    if(map.get("phone") != null){
                        mPhoneNo = map.get("phone").toString();
                        mPhoneNoET.setText(mPhoneNo);
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
                setProfilePic(getContext(), uri, mProfieImage);
            }
        });
    }
    private void saveUserInfo() {
        mName = mNameET.getText().toString();
        mPhoneNo = mPhoneNoET.getText().toString();

        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhoneNo);
        mCustomerDatabase.updateChildren(userInfo);

        if(selectedImageUri != null){
            StorageReference fileRef = getCurrentProfileImageStorageRef();
            fileRef.putFile(selectedImageUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        userInfo.put("profileImageUrl", imageUrl);
                        mCustomerDatabase.updateChildren(userInfo);
                        Toast.makeText(getContext(),
                                "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(),
                                "Failed to get image URL", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(getContext(),
                            "Failed to upload image: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            mCustomerDatabase.updateChildren(userInfo);
            Toast.makeText(getContext(),
                    "Profile updated without new image", Toast.LENGTH_SHORT).show();
        }
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
}