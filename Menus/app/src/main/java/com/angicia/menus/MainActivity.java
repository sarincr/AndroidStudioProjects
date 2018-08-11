package com.angicia.menus;

import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
Button b;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        b=findViewById(R.id.b1);
        registerForContextMenu(b);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mmenu,menu);
        return true;
        }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.start:
                Toast.makeText(this,"Started",Toast.LENGTH_SHORT).show();
                String s=item.getTitle().toString();
                this.shoAlert(s);

                break;
            case R.id.run:
                Toast.makeText(this,"run",Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop:
                finish();
                break;
        }

       return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.mmenu, menu);

    }
   public void  shoAlert(String msg)
   {
       AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Your Alert");
       alert.setMessage("Your Message");
       alert.setCancelable(true);
       AlertDialog al= alert.create();
       al.show();



           }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.start:
                Toast.makeText(this,"Started",Toast.LENGTH_SHORT).show();
                 break;
            case R.id.run:
                Toast.makeText(this,"run",Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop:
                finish();
                break;
        }
        return true;
    }
}
