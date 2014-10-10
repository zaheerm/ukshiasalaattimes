package com.azam.android.salaattimes.test;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContext;

import com.azam.android.salaattimes.Data;
import com.azam.android.salaattimes.Salaat;

import java.util.Calendar;

/**
 * Created by zmerali on 10/9/14.
 */
public class DataTest extends InstrumentationTestCase {
    public void test_open_close() throws Exception {
        Context context = new MockContext();
        Data data = Data.getData(context);
        data.close();
    }
    public void test_get_next_salaat() throws Exception {
        Context context = new MockContext();
        Data data = Data.getData(context);
        Salaat salaat = data.getNextSalaat(context, Calendar.getInstance());
        data.close();
        assertNotNull(salaat);
    }

}
