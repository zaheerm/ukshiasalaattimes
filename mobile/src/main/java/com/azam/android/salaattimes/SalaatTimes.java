package com.azam.android.salaattimes;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.SQLException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class SalaatTimes extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salaat_times);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.salaat_times, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private Calendar day;
        public PlaceholderFragment() {
            day = Calendar.getInstance();
        }

        private void resetColors(View rootView) {
            for(int viewId : new int[]{
                    R.id.imsaak_value,
                    R.id.fajr_value,
                    R.id.sunrise_value,
                    R.id.zohr_value,
                    R.id.sunset_value,
                    R.id.maghrib_value,
                    R.id.tomorrowfajr_value,
                    R.id.imsaak_label,
                    R.id.fajr_label,
                    R.id.sunrise_label,
                    R.id.zohr_label,
                    R.id.sunset_label,
                    R.id.maghrib_label,
                    R.id.tomorrowfajr_label

            }) {
                TextView t = (TextView) rootView.findViewById(viewId);
                t.setTextColor(getResources().getColor(R.color.white));
            }
        }
        private void setDate(Calendar day, View rootView) {
            resetColors(rootView);
            DatabaseHelper myDbHelper = new DatabaseHelper(rootView.getContext());
            try {
                myDbHelper.createDataBase();
            } catch (IOException ioe) {
                throw new Error("Unable to create database");
            }

            try {
                myDbHelper.openDataBase();
            }catch(SQLException sqle){
                throw sqle;
            }
            Data data = new Data(myDbHelper);
            Entry entry = data.getEntry(day);
            for(int viewId : new int[]{
                    R.id.imsaak_value,
                    R.id.fajr_value,
                    R.id.sunrise_value,
                    R.id.zohr_value,
                    R.id.sunset_value,
                    R.id.maghrib_value,
                    R.id.tomorrowfajr_value
            }) {
                TextView t = (TextView)rootView.findViewById(viewId);
                try {
                    t.setText(entry.getSalaat(viewId));
                } catch (Exception e) {
                    // should never get here
                }
            }
            ActionBar actionBar = getActivity().getActionBar();
            actionBar.setTitle(new SimpleDateFormat("d MMM yyyy").format(day.getTime()));
            Calendar now = Calendar.getInstance();
            if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    day.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    day.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                highlightNextSalaat(rootView, entry);
            }
        }

        private void highlightNextSalaat(View rootView, Entry entry) {
            Calendar now = Calendar.getInstance();
            boolean found = false;
            for(int viewId : new int[]{
                    R.id.fajr_value,
                    R.id.zohr_value,
                    R.id.maghrib_value,
            }) {
                try {
                    String salaatTime = entry.getSalaat(viewId);
                    String[] time_split = salaatTime.split(" ");
                    int hour = Integer.valueOf(time_split[0]);
                    int minute = Integer.valueOf(time_split[1]);
                    Calendar salaat = (Calendar)now.clone();
                    salaat.set(Calendar.HOUR, hour);
                    salaat.set(Calendar.MINUTE, minute);
                    if (now.compareTo(salaat) < 0) {
                        TextView t = (TextView)rootView.findViewById(viewId);
                        t.setTextColor(getResources().getColor(R.color.green));
                        found = true;
                        break;
                    }
                } catch (Exception e) {}
            }
            if (!found) {
                TextView t = (TextView)rootView.findViewById(R.id.tomorrowfajr_value);
                t.setTextColor(getResources().getColor(R.color.green));
            }
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_salaat_times, container, false);
            Context c = rootView.getContext();
            setDate(day, rootView);
            rootView.setOnTouchListener(new OnSwipeTouchListener(c) {
                @Override
                public void onSwipeLeft() {
                   day.add(Calendar.DAY_OF_MONTH, 1);
                   setDate(day, rootView);
                }

                @Override
                public void onSwipeRight() {
                    day.add(Calendar.DAY_OF_MONTH, -1);
                    setDate(day, rootView);
                }

            });
            return rootView;
        }
    }
}
