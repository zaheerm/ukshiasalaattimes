package com.azam.android.salaattimes;

import android.content.Context;
import android.content.SharedPreferences;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;

/**
 * Created by zmerali on 10/9/14.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataTest {
    @Mock private Context context;
    @Mock private DatabaseHelper dbHelper;
    @Mock private SharedPreferences preferences;

    @Test
    public void test_open_close() throws Exception {
        Data data = new Data(dbHelper, 37.3382, -121.8863);
        data.close();
        verify(dbHelper, times(1)).close();
    }
    @Test
    public void test_get_next_salaat() throws Exception {
        Data data = new Data(dbHelper, 37.3382, -121.8863);
        when(context.getSharedPreferences("salaat", 0)).thenReturn(preferences);
        when(preferences.getString(eq("city"), anyString())).thenReturn("uselocation");
        Salaat salaat = data.getNextSalaat(context, Calendar.getInstance());
        data.close();
        assertThat(data!=null);
    }

}
