package com.angicia.logins;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    EditText et1, et2;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et1 = findViewById(R.id.e1);
        et2 = findViewById(R.id.e2);
        tv = findViewById(R.id.t1);
    }
    public void login(View v){
        String s1 = et1.getText().toString();
        String s2 = et2.getText().toString();
        if(s1.equals("admin") && (s2.equals("admin123"))) {
            tv.setText("Login Success");
            String n= et1.getText().toString();
            tv.setTextColor(Color.GREEN);
            Intent i = new Intent(this, Second.class);
            i.putExtra("name",n);
            startActivity(i);
        }
        else {
            tv.setText("Login Failed");
            tv.setTextColor(Color.RED);
        }
    }

}