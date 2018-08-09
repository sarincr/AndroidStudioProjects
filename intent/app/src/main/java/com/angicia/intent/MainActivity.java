package com.angicia.intent;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;



public class MainActivity extends AppCompatActivity {
    EditText et1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void login(View v) {
        et1=findViewById(R.id.et1);
        String s1 = et1.getText().toString();
        if (s1.equals("admin")) {
            Intent i = new Intent(this, Second.class);
            startActivity(i);
        }
    }
}
