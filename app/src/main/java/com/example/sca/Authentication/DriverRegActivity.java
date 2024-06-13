package com.example.sca.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sca.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class DriverRegActivity extends AppCompatActivity {

    private EditText ETUser, ETPass;
    private Button BTNReg, BTNBack;
    private TextView TVLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_reg);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        ETUser = findViewById(R.id.etUsername);
        ETPass = findViewById(R.id.etPassword);
        BTNReg = findViewById(R.id.btnReg);
        BTNBack = findViewById(R.id.btnBack);
        TVLogin = findViewById(R.id.tvLogin);

        TVLogin.setOnClickListener(view -> {
            startActivity(new Intent(DriverRegActivity.this,
                    DriverLoginActivity.class));
            finish();
        });

        BTNBack.setOnClickListener(view -> {
            startActivity(new Intent(DriverRegActivity.this,
                    DriverLoginActivity.class));
            finish();
        });

        BTNReg.setOnClickListener(view -> {
            String username = ETUser.getText().toString().trim();
            String password = ETPass.getText().toString().trim();
            String userAccounts = "Users";
            String accountType = "Drivers";

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(getApplicationContext(), "Enter email address!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "Enter password!",
                        Toast.LENGTH_SHORT).show();
                return;
            }


            mAuth.createUserWithEmailAndPassword(username, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", username);
                            userData.put("password", password);

                            String userID = mAuth.getCurrentUser().getUid();
                            DatabaseReference storeCurrentUser =
                                    FirebaseDatabase.getInstance()
                                            .getReference()
                                            .child(userAccounts)
                                            .child(accountType).child(userID)
                                            .child("username");
                            storeCurrentUser.setValue(username);

                            Toast.makeText(
                                    DriverRegActivity.this,
                                    "Registration is Successful",
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(
                                    DriverRegActivity.this,
                                    DriverLoginActivity.class
                            ));
                            finish();

                        } else {
                            Toast.makeText(DriverRegActivity.this,
                                    "Registration Failed",
                                    Toast.LENGTH_LONG).show();

                        }
                    });
        });
    }
}