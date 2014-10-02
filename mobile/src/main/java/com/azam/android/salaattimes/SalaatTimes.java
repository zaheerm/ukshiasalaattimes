package com.azam.android.salaattimes;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class SalaatTimes extends Activity {

    private String city;
    private Calendar currentDay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salaat_times);
        SharedPreferences preferences = getPreferences(0);

        city = preferences.getString("city", "London");
        currentDay = Calendar.getInstance();
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SalaatTimeFragment(Calendar.getInstance(), city))
                    .commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences preferences = getPreferences(0);
        SharedPreferences.Editor editor = preferences.edit();
        Log.i("salaattimes", "Storing " + city + " as preferred city");
        editor.putString("city", city);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.salaat_times, menu);
        MenuItem item = null;
        if (city.equals("London"))
            item = menu.findItem(R.id.action_london);
        else if (city.equals("Birmingham"))
            item = menu.findItem(R.id.action_birmingham);
        else if (city.equals("Peterborough"))
            item = menu.findItem(R.id.action_peterborough);
        if (item == null) Log.i("SalaatTimes", "Did not find menu item");
        else item.setChecked(true);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        boolean citySelected = true;
        switch (id) {
            case R.id.action_london:
                city = "London";
                break;
            case R.id.action_birmingham:
                city = "Birmingham";
                break;
            case R.id.action_peterborough:
                city = "Peterborough";
                break;
            case R.id.action_choosedate:
                new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // Do something here
                        currentDay.set(year, monthOfYear, dayOfMonth);
                        changeDay(currentDay);
                    }
                }, currentDay.get(Calendar.YEAR), currentDay.get(Calendar.MONTH), currentDay.get(Calendar.DAY_OF_MONTH)).show();
            default:
                citySelected = false;
        }
        if (citySelected) {
            Log.i("SalaatTimesActivity", "City chosen " + city);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.container, new SalaatTimeFragment(currentDay, city));
            ft.commit();
            item.setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void changeDay(Calendar day) {
        currentDay = day;
        Log.i("SalaatTimesActivity", "Date changed");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        ft.replace(R.id.container, new SalaatTimeFragment(currentDay, city));
        ft.commit();

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SalaatTimeFragment extends Fragment {

        private Calendar day;
        private String city;
        public SalaatTimeFragment(Calendar day, String city) {
            this.day = day;
            this.city = city;
        }

        public SalaatTimeFragment() {
            day = Calendar.getInstance();
            this.city = city;
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
        private void setDate(View rootView) {
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
            SalaatTimes activity = (SalaatTimes)getActivity();
            Entry entry = data.getEntry(day, city);
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
            actionBar.setTitle(new SimpleDateFormat("d MMM yyyy").format(day.getTime()) + " - " + city);
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
            setDate(rootView);
            rootView.setOnTouchListener(new OnSwipeTouchListener(c) {
                @Override
                public void onSwipeLeft() {
                    SalaatTimes activity = (SalaatTimes)getActivity();
                    day.add(Calendar.DAY_OF_MONTH, 1);
                    activity.changeDay(day);
                }

                @Override
                public void onSwipeRight() {
                    SalaatTimes activity = (SalaatTimes)getActivity();
                    day.add(Calendar.DAY_OF_MONTH, -1);
                    activity.changeDay(day);
                }

            });
            return rootView;
        }
    }
}
