package com.angicia.myapplication;



import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    EditText et= findViewById(R.id.n1);
    TextView tv= findViewById(R.id.t1);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et = findViewById(R.id.n1);
        tv = findViewById(R.id.t1);
    }
    public void show(View v){
        String s = et.getText().toString();
        tv.setText("Hello "+s);
    }
}