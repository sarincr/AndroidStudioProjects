package com.angicia.logins;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity {
    EditText et1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et1 = findViewById(R.id.et1);
         }
    public void login(View v){
        int num=9605;
        String s="https://wwww.googe.com";
        Intent i= new Intent(Intent.ACTION_WEB_SEARCH,Uri.parse(s));
        startActivity(i);
       }

}
