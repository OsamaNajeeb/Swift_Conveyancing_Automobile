package com.example.sca.Homepage.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.sca.R;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class DriverHelpFragment extends Fragment {
    private EditText mTitle, mPost;
    private ImageView mPostPhoto;
    private Button mBTNAddPost;
    String requestID;
    private String userID = "";
    ActivityResultLauncher<Intent> imagePicker;
    Uri selectedImageUri;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_help, container, false);

        userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        mTitle = view.findViewById(R.id.etTitle);
        mPost = view.findViewById(R.id.etPost);
        mPostPhoto = view.findViewById(R.id.ivPhoto);
        mBTNAddPost = view.findViewById(R.id.btnAddPost);

        imagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data != null && data.getData() != null){
                            selectedImageUri = data.getData();
                            setProfilePic(getContext(), selectedImageUri, mPostPhoto);
                        }
                    }
                });

        mPostPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker
                        .with(DriverHelpFragment.this)
                        .cropSquare()
                        .createIntent(new Function1<Intent, Unit>() {
                            @Override
                            public Unit invoke(Intent intent) {
                                imagePicker.launch(intent);
                                return null;
                            }
                        });
            }
        });

        mBTNAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String inTitle = mTitle.getText().toString();
                String inPost = mPost.getText().toString();

                DatabaseReference customerRef = FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("Users")
                        .child("Drivers")
                        .child(userID)
                        .child("post");

                DatabaseReference postRef = FirebaseDatabase
                        .getInstance()
                        .getReference()
                        .child("post");

                requestID = postRef.push().getKey();

                if (requestID != null) {
                    customerRef.child(requestID).setValue(true);

                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", userID);
                    map.put("title ", inTitle);
                    map.put("post: ", inPost);

                    if(selectedImageUri != null){
                        StorageReference fileRef = getCurrentProfileImageStorageRef();
                        fileRef.putFile(selectedImageUri).addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    String imageUrl = uri.toString();
                                    map.put("postImageUrl", imageUrl);
                                    postRef.child(requestID).updateChildren(map);
                                    Toast.makeText(getContext(), "Success",
                                            Toast.LENGTH_LONG).show();
                                    mTitle.setText("");
                                    mPost.setText("");
                                    mPostPhoto.setImageResource(R.drawable.add_photo_alternate_100);
                                });
                            }
                        });
                    }else{
                        mTitle.setText("");
                        mPost.setText("");
                        postRef.child(requestID).updateChildren(map);
                        Toast.makeText(getContext(), "Success", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        return  view;
    }

    private static void setProfilePic(Context context, Uri selectedImageUri, ImageView imageView){
        Glide.with(context)
                .load(selectedImageUri)
                .into(imageView);
    }

    private StorageReference getCurrentProfileImageStorageRef() {
        if (userID == null || userID.isEmpty()) {
            return null;
        }
        return FirebaseStorage.getInstance().getReference()
                .child("post_Image")
                .child(requestID);
    }

}