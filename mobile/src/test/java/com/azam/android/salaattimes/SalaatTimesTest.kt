package com.azam.android.salaattimes

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.location.LocationManager

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.*

/**
 * Created by zmerali on 10/9/14.
 */
@RunWith(MockitoJUnitRunner::class)
class SalaatTimesTest {
    @Mock
    private val context: Context? = null
    @Mock
    private val dbHelper: DatabaseHelper? = null
    @Mock
    private val preferences: SharedPreferences? = null
    @Mock
    private val locationManager: LocationManager? = null
    @Mock
    private val db: SQLiteDatabase? = null
    @Mock
    private val cursor: Cursor? = null

    @Test
    @Throws(Exception::class)
    fun test_open_close() {
        val data = SalaatTimes(dbHelper, context)
        data.close()
        verify<DatabaseHelper>(dbHelper, times(1)).close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_gps_no_location() {
        val data = SalaatTimes(dbHelper, context)
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("uselocation")
        `when`(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        val salaat = data.getNextSalaat(context, Calendar.getInstance())
        assertThat(salaat).isNull()
        data.close()
    }

    @Test(expected=SecurityException::class)
    @Throws(Exception::class)
    fun test_get_next_salaat_gps_security_exception() {
        val data = SalaatTimes(dbHelper, context)
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("uselocation")
        `when`(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        `when`(locationManager!!.getProviders(true)).thenReturn(mutableListOf<String>("dummyprovider").toList())
        val location = Location("")
        location.latitude = 0.0
        location.longitude = 0.0
        `when`(locationManager.getLastKnownLocation(anyString())).thenThrow(SecurityException())
        val salaat = data.getNextSalaat(context, Calendar.getInstance())
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_gps_with_location() {
        val data = SalaatTimes(dbHelper, context)
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("uselocation")
        `when`(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        `when`(locationManager!!.getProviders(true)).thenReturn(mutableListOf<String>("dummyprovider").toList())
        val location = Location("")
        location.latitude = 0.0
        location.longitude = 0.0
        `when`(locationManager.getLastKnownLocation(anyString())).thenReturn(location);
        val salaat = data.getNextSalaat(context, Calendar.getInstance())
        assertThat(salaat).isNotNull()
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_entry_with_city() {
        val data = SalaatTimes(dbHelper, context)
        `when`(dbHelper!!.getReadableDatabase()).thenReturn(db)
        `when`(db!!.rawQuery(anyString(), any())).thenReturn(cursor)
        `when`(cursor!!.getString(0)).thenReturn("05:00")
        `when`(cursor!!.getString(1)).thenReturn("05:10")
        `when`(cursor!!.getString(2)).thenReturn("06:00")
        `when`(cursor!!.getString(3)).thenReturn("12:00")
        `when`(cursor!!.getString(4)).thenReturn("16:00")
        `when`(cursor!!.getString(5)).thenReturn("16:20")
        `when`(cursor!!.getString(6)).thenReturn("04:59")
        val entry = data.getEntry(Calendar.getInstance(), "London")
        assertThat(entry).isNotNull()
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_time_fajr() {
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("London")
        val data = SalaatTimes(dbHelper, context)
        `when`(dbHelper!!.getReadableDatabase()).thenReturn(db)
        `when`(db!!.rawQuery(anyString(), any())).thenReturn(cursor)
        `when`(cursor!!.getString(0)).thenReturn("05:00")
        `when`(cursor!!.getString(1)).thenReturn("05:10")
        `when`(cursor!!.getString(2)).thenReturn("06:00")
        `when`(cursor!!.getString(3)).thenReturn("12:00")
        `when`(cursor!!.getString(4)).thenReturn("16:00")
        `when`(cursor!!.getString(5)).thenReturn("16:20")
        `when`(cursor!!.getString(6)).thenReturn("04:59")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("Europe/London")
        cal.set(Calendar.HOUR_OF_DAY, 2);
        val salaat = data.getNextSalaat(context, cal)
        assertThat(salaat.salaatName).isSameAs("Fajr")
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_time_zohr() {
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("London")
        val data = SalaatTimes(dbHelper, context)
        `when`(dbHelper!!.getReadableDatabase()).thenReturn(db)
        `when`(db!!.rawQuery(anyString(), any())).thenReturn(cursor)
        `when`(cursor!!.getString(0)).thenReturn("05:00")
        `when`(cursor!!.getString(1)).thenReturn("05:10")
        `when`(cursor!!.getString(2)).thenReturn("06:00")
        `when`(cursor!!.getString(3)).thenReturn("12:00")
        `when`(cursor!!.getString(4)).thenReturn("16:00")
        `when`(cursor!!.getString(5)).thenReturn("16:20")
        `when`(cursor!!.getString(6)).thenReturn("04:59")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("Europe/London")
        cal.set(Calendar.HOUR_OF_DAY, 9);
        val salaat = data.getNextSalaat(context, cal)
        assertThat(salaat.salaatName).isSameAs("Zohr")
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_time_maghrib() {
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("London")
        val data = SalaatTimes(dbHelper, context)
        `when`(dbHelper!!.getReadableDatabase()).thenReturn(db)
        `when`(db!!.rawQuery(anyString(), any())).thenReturn(cursor)
        `when`(cursor!!.getString(0)).thenReturn("05:00")
        `when`(cursor!!.getString(1)).thenReturn("05:10")
        `when`(cursor!!.getString(2)).thenReturn("06:00")
        `when`(cursor!!.getString(3)).thenReturn("12:00")
        `when`(cursor!!.getString(4)).thenReturn("16:00")
        `when`(cursor!!.getString(5)).thenReturn("16:20")
        `when`(cursor!!.getString(6)).thenReturn("04:59")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("Europe/London")
        cal.set(Calendar.HOUR_OF_DAY, 14);
        val salaat = data.getNextSalaat(context, cal)
        assertThat(salaat.salaatName).isSameAs("Maghrib")
        data.close()
    }

    @Test
    @Throws(Exception::class)
    fun test_get_next_salaat_time_tomorrowfajr() {
        `when`(context!!.getSharedPreferences("salaat", 0)).thenReturn(preferences)
        `when`(preferences!!.getString(eq("city"), anyString())).thenReturn("London")
        val data = SalaatTimes(dbHelper, context)
        `when`(dbHelper!!.getReadableDatabase()).thenReturn(db)
        `when`(db!!.rawQuery(anyString(), any())).thenReturn(cursor)
        `when`(cursor!!.getString(0)).thenReturn("05:00")
        `when`(cursor!!.getString(1)).thenReturn("05:10")
        `when`(cursor!!.getString(2)).thenReturn("06:00")
        `when`(cursor!!.getString(3)).thenReturn("12:00")
        `when`(cursor!!.getString(4)).thenReturn("16:00")
        `when`(cursor!!.getString(5)).thenReturn("16:20")
        `when`(cursor!!.getString(6)).thenReturn("04:59")
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("Europe/London")
        cal.set(Calendar.HOUR_OF_DAY, 17);
        val salaat = data.getNextSalaat(context, cal)
        assertThat(salaat.salaatName).isSameAs("Fajr")
        data.close()
    }
}
