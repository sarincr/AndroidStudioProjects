package com.angicia.asynchtasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class asytask extends AsyncTask <Void,Integer,String>
{
    Context ctx;
    ProgressDialog pb;
    asytask(Context c)
    {
        ctx=c;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pb = new ProgressDialog(ctx);
        pb.setCancelable(true);
        pb.setTitle("My Progress is");
        pb.setMessage("Dwnloadihg");
        pb.setMax(100);
        pb.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pb.show();
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            for (int i=0;i<100;i++)
            {
                publishProgress(i);
                Thread.sleep(100);

            }
            return "Success";
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return "Fail";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        pb.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Toast.makeText(ctx,s, Toast.LENGTH_SHORT);
        pb.dismiss();
    }
}
