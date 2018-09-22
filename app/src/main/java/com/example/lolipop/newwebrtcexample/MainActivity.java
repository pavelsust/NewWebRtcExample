package com.example.lolipop.newwebrtcexample;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {






    public static final String USER_ID = "e695390a70cedfaa";
    public static final String USER_NAME = "one_plus";

    public static String onePlus = "e695390a70cedfaa";
    public static String samsung = "4f0c65cfb4c11132";


    // one plus e695390a70cedfaa
    // samsung 4f0c65cfb4c11132


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String android_id = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        Log.d("ANDROID_ID" , ""+android_id);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this , CallActivity.class);
                intent.putExtra("to" , samsung);
                intent.putExtra("from" , onePlus);
                startActivity(intent);
            }
        });
    }

}
