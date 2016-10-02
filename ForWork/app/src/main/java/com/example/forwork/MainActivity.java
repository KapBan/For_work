package com.example.forwork;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity{

    public final static int SIZE=30;

    public String[] titles = new String[SIZE];          //array for titles
    public String[] keys = new String[SIZE];            //array for id of news
    public String[] image_url = new String[SIZE];       //array for image urls to download
    public String data="";
    //that's for parsing
    JSONObject news = null;
    String title = "";
    JSONObject prefs = null;
    JSONObject inner = null;
    JSONObject elements = null;
    JSONObject image =null;
    JSONObject json = null;
    JSONArray collection = null;
    JSONObject documents = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (isInternetThere(this))                                          //if internet is not absent
            try {
                ListView my_list_view = (ListView) findViewById(R.id.list_view);    //custom list view
                CustomAdapter adapter = new CustomAdapter(this,titles,image_url);         //custom adapter
                my_list_view.setAdapter(adapter);
                Parse p = new Parse();                                               //instance of my async task for getting json
                data = p.execute().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            finish();
        }

        try {
            json = new JSONObject(data);                                    //parsing json
            collection = json.getJSONArray("collection");                   //new's ids
            documents = json.getJSONObject("documents");                    //new's info
            for (int i=0; i<keys.length;i++){
                keys[i]=collection.getString(i);
                news = documents.getJSONObject(keys[i]);
                title = news.getString("title");                            //title
                titles[i]=title;                                            //put title into list
                prefs = news.getJSONObject("prefs");
                inner = prefs.getJSONObject("inner");
                elements = inner.getJSONObject("elements");
                image = elements.getJSONObject("image");
                if (image.has("small_url"))                                 //whether there is a picture?
                    image_url[i] = image.getString("small_url");
                else image_url[i] = "";
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {           //it is just for orientation monitoring
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    public class Parse extends AsyncTask<Void,Void,String> {                //my async task #1 that just get a json from url

        @Override
        protected String doInBackground(Void... params) {
            String str= null;
            try {
                str = readJSON();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return str;
        }

        public String readJSON() {                                          //function that get json
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String resultJson = "";                                         //this will be a string with json
            try {
                URL url = new URL("https://meduza.io/api/v3/search?chrono=news&page=0&per_page=30&locale=ru");      //url with 30 news

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();       //stream for getting json
                StringBuffer buffer = new StringBuffer();                       //buffer for json saving

                reader = new BufferedReader(new InputStreamReader(inputStream));        //it just reads

                String line;
                while ((line = reader.readLine()) != null) {                            //line by line..
                    buffer.append(line);                                                //...and add it into buffer
                }

                resultJson = buffer.toString();                                 //result is string got from buffer

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }
    }

    public class CustomAdapter extends ArrayAdapter<String>{                           //that is my custom adapter named CustomAdapter. probably for not misunderstanding
        private Activity context;
        private String[] item_name;
        private String[] imgurl;

        public CustomAdapter(Activity context,String[] item_name, String[] imgurl){        //constructor
            super(context,R.layout.my_listview_item,item_name);
            this.context=context;               //activity
            this.item_name=item_name;           //there are will be the titles
            this.imgurl=imgurl;                 //there are will be the images
        }

        @Override
        public int getCount(){
            return item_name.length;
        }

        @Override
        public String getItem(int position){
            return item_name[position];
        }

        @Override
        public long getItemId(int position){
            return position;
        }


        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.my_listview_item,null,true);

            TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);

            if (imgurl[position] != "") {                                               //if there is a url of an image(because there some news without it)
                rowView.findViewById(R.id.img).setVisibility(View.VISIBLE);             //show the picture slot
                imgurl[position] = "https://meduza.io" + imgurl[position];              //and make the url absolute, not relative
                Picasso.with(context).load(imgurl[position]).into((ImageView) rowView.findViewById(R.id.img));  //download an image if it exists and put it into imageview
            }
            else
                rowView.findViewById(R.id.img).setVisibility(View.GONE);                //if there is no picture, just hide the slot
            txtTitle.setText(item_name[position]);                                      //it is title

            return rowView;
        }
    }

    public boolean isInternetThere(Context context) {                                   //it is for internet connection testing
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}
