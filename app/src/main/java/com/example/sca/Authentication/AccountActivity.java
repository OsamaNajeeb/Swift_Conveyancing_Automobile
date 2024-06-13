package com.example.sca.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sca.R;
import com.example.sca.Utility.onAppTerminated;


public class AccountActivity extends AppCompatActivity {

    private Button BTNCustomer, BTNDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startService(new Intent(AccountActivity.this, onAppTerminated.class));

        BTNCustomer = findViewById(R.id.btnCustomer);
        BTNDriver = findViewById(R.id.btnDriver);

        BTNCustomer.setOnClickListener(view -> {
            startActivity(new Intent(AccountActivity.this, CustomerLoginActivity.class));
        });

        BTNDriver.setOnClickListener(view -> {
            startActivity(new Intent(AccountActivity.this, DriverLoginActivity.class));
        });
    }

}