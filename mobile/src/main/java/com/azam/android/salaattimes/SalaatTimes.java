package com.azam.android.salaattimes;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class SalaatTimes extends Activity {

    private static final int MY_PERMISSION_REQUEST_LOCATION = 100;
    private Menu optionsMenu;
    private Calendar currentDay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salaat_times);
        NotificationPublisher.initializeChannels(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationPublisher.cancel(this);
        currentDay = Calendar.getInstance();
        getFragmentManager().beginTransaction()
                .replace(R.id.container, SalaatTimeFragment.newInstance(Calendar.getInstance(), getCity()))
                .commit();
        scheduleNotification();

    }

    private String getCity() {
        SharedPreferences preferences = getSharedPreferences("salaat", 0);

        return preferences.getString("city", "London");
    }

    private void scheduleNotification() {
        Data data = Data.getData(this);
        data.scheduleNextSalaatNotification(this);
        data.close();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.salaat_times, menu);
        SharedPreferences preferences = getSharedPreferences("salaat", 0);
        boolean nextSalaatNotify = preferences.getBoolean("nextsalaatnotify", true);
        boolean adhan = preferences.getBoolean("nextsalaatadhan", true);
        MenuItem item = null;
        String city = getCity();
        if (city.equals("London"))
            item = menu.findItem(R.id.action_london);
        else if (city.equals("Birmingham"))
            item = menu.findItem(R.id.action_birmingham);
        else if (city.equals("Peterborough"))
            item = menu.findItem(R.id.action_peterborough);
        else if (city.equals("Leicester"))
            item = menu.findItem(R.id.action_leicester);
        else if (city.equals("uselocation"))
            item = menu.findItem(R.id.action_uselocation);
        if (item == null) Log.w("SalaatTimes", "Did not find menu item");
        else item.setChecked(true);

        item = menu.findItem(R.id.action_notifications);
        item.setChecked(nextSalaatNotify);
        item = menu.findItem(R.id.action_adhan);
        item.setChecked(adhan);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    cityChosen("uselocation");
                    optionsMenu.findItem(R.id.action_uselocation).setChecked(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    cityChosen("London");
                    optionsMenu.findItem(R.id.action_london).setChecked(true);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        boolean citySelected = true;
        String city = null;
        SharedPreferences preferences = getSharedPreferences("salaat", 0);
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
            case R.id.action_leicester:
                city = "Leicester";
                break;
            case R.id.action_uselocation:
                city = "uselocation";
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_LOCATION);
                }
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
                citySelected = false;
                break;
            case R.id.action_notifications:
                item.setChecked(!item.isChecked());
                boolean nextSalaatNotify = item.isChecked();
                preferences.edit().putBoolean("nextsalaatnotify", nextSalaatNotify).commit();
                scheduleNotification();
                citySelected = false;
                break;
            case R.id.action_adhan:
                item.setChecked(!item.isChecked());
                boolean adhan = item.isChecked();
                preferences.edit().putBoolean("nextsalaatadhan", adhan).commit();
                citySelected = false;
                break;

            default:
                citySelected = false;
        }
        if (citySelected) {
            cityChosen(city);
            item.setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cityChosen(String city) {
        SharedPreferences preferences = getSharedPreferences("salaat", 0);
        preferences.edit().putString("city", city).commit();
        Log.i("SalaatTimesActivity", "City chosen " + city);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.container, SalaatTimeFragment.newInstance(currentDay, city));
        ft.commit();
        scheduleNotification();
        Data data = Data.getData(this);
        Salaat salaat = null;
        try {
            salaat = data.getNextSalaat(this, Calendar.getInstance());
        } catch (SecurityException e) {}
        data.close();
        if (salaat != null) {
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.single_salaat_app_widget);
            views.setTextViewText(R.id.nextsalaat_label, salaat.getSalaatName());
            views.setTextViewText(R.id.nextsalaat_value, salaat.getSalaatTimeAsString());
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            ComponentName thisWidget = new ComponentName(this, SingleSalaatAppWidget.class);
            manager.updateAppWidget(thisWidget, views);
        }
    }

    public void changeDay(Calendar day) {
        currentDay = day;
        Log.i("salaattimes", "Date changed");
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.container, SalaatTimeFragment.newInstance(day, getCity()));
        ft.commit();

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SalaatTimeFragment extends Fragment {

        public static SalaatTimeFragment newInstance(Calendar day, String city) {
            SalaatTimeFragment fragment = new SalaatTimeFragment();
            Bundle args = new Bundle();
            args.putInt("year", day.get(Calendar.YEAR));
            args.putInt("month", day.get(Calendar.MONTH));
            args.putInt("day", day.get(Calendar.DAY_OF_MONTH));
            args.putString("city", city);
            fragment.setArguments(args);
            return fragment;
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

        private Calendar getCalendar() {
            Bundle args = getArguments();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, args.getInt("year"));
            cal.set(Calendar.MONTH, args.getInt("month"));
            cal.set(Calendar.DAY_OF_MONTH, args.getInt("day"));
            return cal;
        }

        private String getCity() {
            return getArguments().getString("city");
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        private void setDate(final View rootView) {
            resetColors(rootView);
            Data data = Data.getData(getActivity());
            Calendar day = getCalendar();
            String city = getCity();
            try {
                Entry entry = data.getEntry(day, city);
                Salaat nextSalaat = data.getNextSalaat(getActivity(), Calendar.getInstance());
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
                if (city.equals("uselocation")) {
                    city = "GPS Location";
                    if (entry == null) {
                        // Define a listener that responds to location updates
                        LocationListener locationListener = new LocationListener() {
                            public void onLocationChanged(Location location) {
                                setDate(rootView);
                                LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
                                lm.removeUpdates(this);
                            }

                            public void onStatusChanged(String provider, int status, Bundle extras) {}

                            public void onProviderEnabled(String provider) {}

                            public void onProviderDisabled(String provider) {}
                        };

// Register the listener with the Location Manager to receive location updates
                        LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    }
                    new GeocoderTask().execute(data);
                }
                actionBar.setTitle(new SimpleDateFormat("d MMM yyyy").format(day.getTime()) + " - " + city);
                Calendar now = Calendar.getInstance();
                if (nextSalaat != null) {
                    if (day.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            day.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            day.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)) {
                        highlightNextSalaat(rootView, nextSalaat);
                    }
                }
            } catch (SecurityException e) {
                Activity context = getActivity();
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_LOCATION);
                }
            } finally {
                data.close();
            }

        }

        private class GeocoderTask extends AsyncTask<Data, String, String> {

            @Override
            protected String doInBackground(Data... params) {
                String city;
                if (isNetworkAvailable()) {
                    Data data = params[0];
                    Geocoder geoCoder = new Geocoder(getActivity());
                    Location location = data.getLastKnownLocation();
                    if (location != null) {
                        try {
                            List<Address> list = geoCoder.getFromLocation(location
                                    .getLatitude(), location.getLongitude(), 1);
                            if (list != null & list.size() > 0) {
                                Address address = list.get(0);
                                city = "GPS: " + address.getLocality();
                            } else {
                                city = "GPS Location";
                            }
                        } catch (IOException e) {
                            city = "GPS Location";
                        }
                    } else {
                        city = "GPS Location";
                    }
                } else {
                    city = "GPS Location";
                }
                return city;
            }

            @Override
            protected void onPostExecute(String city) {
                super.onPostExecute(city);
                Activity activity = getActivity();
                if (activity!=null) {
                    ActionBar actionBar = getActivity().getActionBar();
                    Calendar day = getCalendar();
                    actionBar.setTitle(new SimpleDateFormat("d MMM yyyy").format(day.getTime()) + " - " + city);
                }
            }
        }

        private void highlightNextSalaat(View rootView, Salaat nextSalaat) {
            if (nextSalaat != null) {
                TextView t = (TextView) rootView.findViewById(nextSalaat.getLabel());
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
                    Calendar day = getCalendar();
                    day.add(Calendar.DAY_OF_MONTH, 1);
                    activity.changeDay(day);
                }

                @Override
                public void onSwipeRight() {
                    SalaatTimes activity = (SalaatTimes)getActivity();
                    Calendar day = getCalendar();
                    day.add(Calendar.DAY_OF_MONTH, -1);
                    activity.changeDay(day);
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        rootView.findViewById(R.id.showLeft).setVisibility(View.VISIBLE);
                        rootView.findViewById(R.id.showRight).setVisibility(View.VISIBLE);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        rootView.findViewById(R.id.showLeft).setVisibility(View.INVISIBLE);
                        rootView.findViewById(R.id.showRight).setVisibility(View.INVISIBLE);
                    }
                    return super.onTouch(v, event);
                }

            });
            return rootView;
        }
    }
}
