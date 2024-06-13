package com.example.sca.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.sca.Maps.DriverMapsActivity;
import com.example.sca.R;
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

public class DriverSettingsActivity extends AppCompatActivity {

    private EditText mNameET, mPhoneNoET, mCarDetails;
    private Button mConfirm, mBack;
    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;
    private String userID;
    private String mName;
    private String mPhoneNo;
    private String mCarStore;
    private String mService;
    ActivityResultLauncher<Intent> imagePicker;
    Uri selectedImageUri;
    private ImageView mProfieImage;
    private RadioGroup mRadioVehicleType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            setProfilePic(getBaseContext(), selectedImageUri, mProfieImage);
                        }
                    }
                });

        mNameET = findViewById(R.id.etUsername);
        mPhoneNoET  = findViewById(R.id.etPhoneNo);
        mCarDetails = findViewById(R.id.etCar);
        mConfirm  = findViewById(R.id.btnConfirm);
        mBack  = findViewById(R.id.btnBack);
        mProfieImage = findViewById(R.id.ivProfile);
        mRadioVehicleType = findViewById(R.id.rgVehicleType);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mDriverDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userID);
        getUserInfo();
        mConfirm.setOnClickListener(view -> saveUserInfo());
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DriverSettingsActivity.this,
                        DriverMapsActivity.class));
                finish();
            }
        });
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
    }
    private void getUserInfo(){
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
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
                    if(map.get("car") != null){
                        mCarStore = map.get("car").toString();
                        mCarDetails.setText(mCarStore);
                    }
                    if(map.get("service") != null){
                        mService = map.get("service").toString();
                        switch (mService){
                            case "PickUp":
                                mRadioVehicleType.check(R.id.rbPickUp);
                                break;
                            case "Box":
                                mRadioVehicleType.check(R.id.rbBox);
                                break;
                            case "Flat Bed":
                                mRadioVehicleType.check(R.id.rbFlatbed);
                                break;
                        }
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
                setProfilePic(getBaseContext(), uri, mProfieImage);
            }
        });
    }
    private void saveUserInfo() {
        mName = mNameET.getText().toString();
        mPhoneNo = mPhoneNoET.getText().toString();
        mCarStore = mCarDetails.getText().toString();

        int selectID = mRadioVehicleType.getCheckedRadioButtonId();

        final RadioButton radioButton = findViewById(selectID);

        if(radioButton.getText() == null){
            return;
        }

        mService = radioButton.getText().toString();

        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhoneNo);
        userInfo.put("car", mCarStore);
        userInfo.put("service", mService);
        mDriverDatabase.updateChildren(userInfo);

        if(selectedImageUri != null){
            StorageReference fileRef = getCurrentProfileImageStorageRef();
            fileRef.putFile(selectedImageUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        userInfo.put("profileImageUrl", imageUrl);
                        mDriverDatabase.updateChildren(userInfo);
                        Toast.makeText(DriverSettingsActivity.this,
                                "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(DriverSettingsActivity.this,
                                "Failed to get image URL", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(DriverSettingsActivity.this,
                            "Failed to upload image: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            mDriverDatabase.updateChildren(userInfo);
            Toast.makeText(DriverSettingsActivity.this,
                    "Profile updated without new image", Toast.LENGTH_SHORT).show();
        }

//        finish();
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