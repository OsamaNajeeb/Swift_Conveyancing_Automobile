package com.example.sca.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sca.Homepage.CustomerHomePageActivity;
import com.example.sca.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerLoginActivity extends AppCompatActivity {
    private EditText ETUser, ETPass;
    private Button BTNLogin, BTNBack;
    private TextView TVRegister;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ETUser = findViewById(R.id.etUsername);
        ETPass = findViewById(R.id.etPassword);
        BTNLogin = findViewById(R.id.btnLogin);
        BTNBack = findViewById(R.id.btnBack);
        TVRegister = findViewById(R.id.tvRegister);
        mAuth = FirebaseAuth.getInstance();

        BTNLogin.setOnClickListener(view -> {
            String username = ETUser.getText().toString();
            String password = ETPass.getText().toString();

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

            mAuth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){

                            String user_ID = mAuth.getCurrentUser().getUid();
                            String userAccounts = "Users";
                            String accountType = "Customers";

                            DatabaseReference userAccRef =
                                    FirebaseDatabase.getInstance().getReference().child(userAccounts);
                            userAccRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(accountType) &&
                                            dataSnapshot.child(accountType).hasChild(user_ID)) {

                                        Toast.makeText(CustomerLoginActivity.this, "Login successful",
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(CustomerLoginActivity.this,
                                                CustomerHomePageActivity.class));
                                        finish();
                                    }
                                    else{
                                        Toast.makeText(CustomerLoginActivity.this,
                                                "User account not found", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(CustomerLoginActivity.this, "Database error",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                        else {
                            Toast.makeText(CustomerLoginActivity.this, "Login failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        BTNBack.setOnClickListener(view -> {
            startActivity(new Intent(CustomerLoginActivity.this,
                    AccountActivity.class));
            finish();
        });

        TVRegister.setOnClickListener(view -> {
            startActivity(new Intent(CustomerLoginActivity.this,
                    CustomerRegActivity.class));
            finish();
        });

    }
}