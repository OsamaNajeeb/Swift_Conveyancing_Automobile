package com.example.sca.Homepage;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.sca.Extra.ContactUsActivity;
import com.example.sca.History.DriverHistoryActivity;
import com.example.sca.Homepage.Fragments.CustomerHelpFragment;
import com.example.sca.Homepage.Fragments.CustomerHomeFragment;
import com.example.sca.Homepage.Fragments.CustomerProfileFragment;
import com.example.sca.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

public class CustomerHomePageActivity extends AppCompatActivity {

    private BottomNavigationView mBottomNV;

    private CustomerHomeFragment homeFragment= new CustomerHomeFragment();
    private CustomerHelpFragment helpFragment = new CustomerHelpFragment();
    private CustomerProfileFragment profileFragment = new CustomerProfileFragment();
    private ImageView mOpenSideMenu;
    private DrawerLayout mDrawer;
    private NavigationView mNavView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mBottomNV = findViewById(R.id.bnvNav);
        mDrawer = findViewById(R.id.dlMenu);
        mOpenSideMenu = findViewById(R.id.ivSideMenu);
        mNavView = findViewById(R.id.nvMenu);

        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int itemID = menuItem.getItemId();

                if(itemID == R.id.navHistory){
                    Intent eye = new Intent(CustomerHomePageActivity.this,
                            DriverHistoryActivity.class);
                    eye.putExtra("customerOrDriver","Customers");
                    startActivity(eye);
                    return true;
                }
                if(itemID == R.id.navContact){
                    startActivity(new Intent(CustomerHomePageActivity.this,
                            ContactUsActivity.class));
                    return true;
                }
                if(itemID == R.id.navLogout){
                    finish();
                    return true;
                }

                mDrawer.close();
                return false;
            }
        });

        mOpenSideMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawer.open();
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flContainer, homeFragment)
                .commit();

        mBottomNV.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.iCustHelp){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.flContainer, helpFragment)
                            .commit();
                    return true;
                }
                else if(menuItem.getItemId() == R.id.iCustProSet){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.flContainer, profileFragment)
                            .commit();
                    return true;
                }
                else{
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.flContainer, homeFragment)
                            .commit();
                    return true;
                }
            }
        });

    }
}