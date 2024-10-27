package com.azam.android.salaattimes;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import androidx.fragment.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class SalaatTimesActivity extends FragmentActivity {

    private static final int MY_PERMISSION_REQUEST_LOCATION = 100;
    private static final String LOG_TAG = "salaat_times_activity";
    private Menu optionsMenu;
    private Calendar currentDay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salaat_times);
        NotificationPublisher.initializeChannels(this);
        makeSureExactAlarmPermission();
        updateAllWidgets(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationPublisher.cancel(this);
        currentDay = Calendar.getInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, SalaatTimeFragment.newInstance(Calendar.getInstance(), getCity()))
                .commit();
        if (hasExactAlarmPermission()) scheduleNotification();
        else makeSureExactAlarmPermission();

    }

    private boolean hasExactAlarmPermission() {
        boolean hasExactAlarmPermission = true;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            hasExactAlarmPermission = alarmManager.canScheduleExactAlarms();
        }
        return hasExactAlarmPermission;
    }
    protected void makeSureExactAlarmPermission() {
        boolean hasExactAlarmPermission = hasExactAlarmPermission();

        if (!hasExactAlarmPermission) {
            showAlarmPermissionDialog();
        }
    }
    public void showAlarmPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("To ensure Salaat notifications are triggered at the exact time, this app needs permission to schedule exact alarms. Please grant this permission in the next screen.");

        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Intent to navigate the user to the system settings for exact alarm permission
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));  // Include package name
                    startActivity(intent);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private String getCity() {
        SharedPreferences preferences = getSharedPreferences("salaat", 0);
        if (preferences!=null)
            return preferences.getString("city", "London");
        else return "London";

    }

    private void scheduleNotification() {
        SalaatTimes salaatTimes = SalaatTimes.build(this);
        salaatTimes.scheduleNextSalaatNotification(this);
        salaatTimes.close();
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
        boolean allSalaatNotify = preferences.getBoolean("nextsalaatnotify", true);
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
        if (item == null) Log.w(LOG_TAG, "Did not find menu item");
        else item.setChecked(true);

        item = menu.findItem(R.id.action_all_salaat);
        if (preferences.contains("fajrnotify")) {
            // this means nextsalaatnotify is no longer used so use whether all salaats have it there
            boolean all_salaat_checked = true;
            for (String salaat: new String[] {"fajr", "zohr", "maghrib"}) {
                boolean salaat_notify = preferences.getBoolean(salaat + "notify", true);
                MenuItem individual_salaat_item = menu.findItem(getIdentifierByName("action_"+salaat));
                individual_salaat_item.setChecked(salaat_notify);
                all_salaat_checked = all_salaat_checked && salaat_notify;
            }
            item.setChecked(all_salaat_checked);
        } else {
            for (String salaat: new String[] {"fajr", "zohr", "maghrib"}) {
                MenuItem individual_salaat_item = menu.findItem(getIdentifierByName("action_"+salaat));
                individual_salaat_item.setChecked(allSalaatNotify);
                preferences.edit().putBoolean(salaat + "notify", allSalaatNotify).apply();
            }
            item.setChecked(allSalaatNotify);
        }

        return true;
    }

    private void adjustAllSalaatNotifications() {
        SharedPreferences preferences = getSharedPreferences("salaat", 0);
        boolean all_salaat_checked = true;
        for (String salaat: new String[] {"fajr", "zohr", "maghrib"}) {
            MenuItem individual_salaat_item = optionsMenu.findItem(getIdentifierByName("action_"+salaat));
            boolean salaat_notify = individual_salaat_item.isChecked();
            all_salaat_checked = all_salaat_checked && salaat_notify;
        }
        MenuItem item = optionsMenu.findItem(R.id.action_all_salaat);
        item.setChecked(all_salaat_checked);
    }

    private int getIdentifierByName(String identifier) {
        try {
            Class res = R.id.class;
            Field field = res.getField(identifier);
            int id = field.getInt(null);
            return id;
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Failure to get identifier.", e);
        }
        return -1;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        String city = "London";
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
                citySelected = false;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.location_dialog_message)
                        .setTitle(R.string.location_dialog_title);

                // Add the buttons
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        if (ContextCompat.checkSelfPermission(SalaatTimesActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(SalaatTimesActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_LOCATION);
                        }
                        cityChosen("uselocation");
                        item.setChecked(true);
                        Log.i(LOG_TAG, "ok button clicked from gps location dialog");
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });


                // Create the AlertDialog
                AlertDialog dialog = builder.create();
                dialog.show();

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
            case R.id.action_all_salaat:
                item.setChecked(!item.isChecked());
                boolean allSalaatNotify = item.isChecked();
                for (int i : new int[]{R.id.action_fajr, R.id.action_zohr, R.id.action_maghrib}) {
                    MenuItem mi = optionsMenu.findItem(i);
                    mi.setChecked(allSalaatNotify);
                }
                preferences.edit().putBoolean("nextsalaatnotify", allSalaatNotify).apply();
                preferences.edit().putBoolean("fajrnotify", allSalaatNotify).apply();
                preferences.edit().putBoolean("zohrnotify", allSalaatNotify).apply();
                preferences.edit().putBoolean("maghribnotify", allSalaatNotify).apply();

                scheduleNotification();
                citySelected = false;
                break;
            case R.id.action_fajr:
                item.setChecked(!item.isChecked());
                citySelected = false;
                preferences.edit().putBoolean("fajrnotify", item.isChecked()).apply();
                adjustAllSalaatNotifications();
                break;
            case R.id.action_zohr:
                item.setChecked(!item.isChecked());
                citySelected = false;
                preferences.edit().putBoolean("zohrnotify", item.isChecked()).apply();
                adjustAllSalaatNotifications();
                break;
            case R.id.action_maghrib:
                item.setChecked(!item.isChecked());
                citySelected = false;
                preferences.edit().putBoolean("maghribnotify", item.isChecked()).apply();
                adjustAllSalaatNotifications();
                break;
            case R.id.privacypolicy:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zaheer.merali.org/ukshiasalaattimes/privacy_policy_2022.html"));
                startActivity(browserIntent);
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
        Log.i(LOG_TAG, "City chosen " + city);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.replace(R.id.container, SalaatTimeFragment.newInstance(currentDay, city));
        ft.commit();
        scheduleNotification();
        updateAllWidgets(this);
    }

    public static void updateAllWidgets(Context context) {
        SalaatTimes salaatTimes = SalaatTimes.build(context);
        Salaat salaat = null;
        try {
            salaat = salaatTimes.getNextSalaat(context, Calendar.getInstance());
        } catch (SecurityException e) {}
        salaatTimes.close();
        if (salaat != null) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, SingleSalaatAppWidget.class);
            // Get all widget IDs for this widget class
            int[] appWidgetIds = manager.getAppWidgetIds(thisWidget);
            for (int widgetId : appWidgetIds) {
                Log.i(LOG_TAG, "Getting app widget to update: " + widgetId);
                SingleSalaatAppWidget.updateAppWidget(context, manager, widgetId, salaat);
            }
        }
    }
    public void changeDay(Calendar day) {
        currentDay = day;
        Log.i(LOG_TAG, "Date changed");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
            Activity activity = getActivity();
            if (activity!= null) {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
            return false;
        }

        private void setDate(final View rootView, Context context) {
            SalaatTimes salaatTimes = SalaatTimes.build(context);
            Calendar day = getCalendar();
            String city = getCity();
            try {
                Entry entry = salaatTimes.getEntry(day, city);
                Salaat nextSalaat = salaatTimes.getNextSalaat(context, Calendar.getInstance());
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
                if (isAdded() && context != null) {

                    resetColors(rootView);
                    TextView t = (TextView) rootView.findViewById(R.id.provider);
                    int provider = R.string.provider_London;
                    if (city.equals("Birmingham")) provider = R.string.provider_Birmingham;
                    if (city.equals("Peterborough")) provider = R.string.provider_Peterborough;
                    if (city.equals("Leicester")) provider = R.string.provider_Leicester;
                    if (city.equals("uselocation")) provider = R.string.provider_uselocation;

                    t.setText(provider);

                    ActionBar actionBar = getActivity().getActionBar();
                    if (city.equals("uselocation")) {
                        city = "GPS Location";
                        if (entry == null) {
                            // Define a listener that responds to location updates
                            LocationListener locationListener = new LocationListener() {
                                public void onLocationChanged(Location location) {
                                    setDate(rootView, context);
                                    LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                                    lm.removeUpdates(this);
                                }

                                public void onStatusChanged(String provider, int status, Bundle extras) {
                                }

                                public void onProviderEnabled(String provider) {
                                }

                                public void onProviderDisabled(String provider) {
                                }
                            };

                            // Register the listener with the Location Manager to receive location updates
                            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                        }
                        new GeocoderTask().execute(salaatTimes);
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
                }
            } catch (SecurityException e) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_LOCATION);
                }
            } finally {
                salaatTimes.close();
            }
        }

        private class GeocoderTask extends AsyncTask<SalaatTimes, String, String> {

            @Override
            protected String doInBackground(SalaatTimes... params) {
                String city;
                if (isNetworkAvailable()) {
                    SalaatTimes salaatTimes = params[0];
                    Geocoder geoCoder = new Geocoder(getActivity());
                    Location location = salaatTimes.getLastKnownLocation();
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
            setDate(rootView, c.getApplicationContext());
            rootView.setOnTouchListener(new OnSwipeTouchListener(c) {
                @Override
                public void onSwipeLeft() {
                    SalaatTimesActivity activity = (SalaatTimesActivity)getActivity();
                    Calendar day = getCalendar();
                    day.add(Calendar.DAY_OF_MONTH, 1);
                    activity.changeDay(day);
                }

                @Override
                public void onSwipeRight() {
                    SalaatTimesActivity activity = (SalaatTimesActivity)getActivity();
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
