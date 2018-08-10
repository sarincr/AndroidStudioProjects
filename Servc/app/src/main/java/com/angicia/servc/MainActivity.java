package com.angicia.servc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public  void ClickServ(View v)
    {
        switch (v.getId())
        {
            case R.id.Start:
                Intent i= new Intent(this, myServ.class);
                startService(i);
                break;
            case R.id.Stop:
                Intent i1= new Intent(this, myServ.class);
                stopService(i1);
                break;
        }
    }
}
