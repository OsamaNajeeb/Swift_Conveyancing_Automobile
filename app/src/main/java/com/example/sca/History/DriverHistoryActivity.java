package com.example.sca.History;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sca.History.RecyclerViewMatierials.HistoryAdapter;
import com.example.sca.History.RecyclerViewMatierials.HistoryObject;
import com.example.sca.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;


public class DriverHistoryActivity extends AppCompatActivity {

    private RecyclerView mHistoryRV;
    private RecyclerView.Adapter mHistoryAdapter;
    private RecyclerView.LayoutManager mHistoryLayoutManager;
    private String customerOrDriver, userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mHistoryRV = findViewById(R.id.rvHistory);
        mHistoryRV.setNestedScrollingEnabled(false);
        mHistoryRV.setHasFixedSize(true);
        mHistoryLayoutManager = new LinearLayoutManager(DriverHistoryActivity.this);
        mHistoryRV.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(getDataSetHistory(),
                DriverHistoryActivity.this);
        mHistoryRV.setAdapter(mHistoryAdapter);

        customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
        userID = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        getUserHistoryIDs();

    }

    private void getUserHistoryIDs() {
        DatabaseReference userHistoryDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Users")
                .child(customerOrDriver)
                .child(userID)
                .child("history");

        userHistoryDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot history : snapshot.getChildren()){
                        fetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void fetchRideInformation(String deliveryKey) {
        DatabaseReference historyDatabase = FirebaseDatabase
                .getInstance()
                .getReference()
                .child("history")
                .child(deliveryKey);

        historyDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String deliveryID = snapshot.getKey();
                    Long timeStamp = 0L;
                    for(DataSnapshot child : snapshot.getChildren()){
                        if(child.getKey().equals("timestamp")){
                            timeStamp = Long.valueOf(child.getValue().toString());
                        }
                    }
                    HistoryObject obj = new HistoryObject(deliveryID, getDate(timeStamp));
                    resultHistory.add(obj);

                    Collections.sort(resultHistory, new Comparator<HistoryObject>() {
                        @Override
                        public int compare(HistoryObject o1, HistoryObject o2) {
                            return o1.getTime().compareTo(o2.getTime());
                        }
                    });

                    Collections.reverse(resultHistory);

                    mHistoryAdapter.notifyDataSetChanged();
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


    private  ArrayList resultHistory = new ArrayList<HistoryObject>();
    private ArrayList<HistoryObject> getDataSetHistory() {
        return resultHistory;
    }
}