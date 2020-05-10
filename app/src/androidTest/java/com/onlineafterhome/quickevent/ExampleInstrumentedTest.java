package com.onlineafterhome.quickevent;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.onlineafterhome.quickevent.bean.Add;
import com.onlineafterhome.quickevent.bean.AddResult;
import com.onlineafterhome.quickevnet.QuickEvent;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.onlineafterhome.quickevent", appContext.getPackageName());
    }

    @Test
    public void ipcCalculation() throws InterruptedException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // 1.init
        QuickEvent.getDefault().init();
        QuickEvent.getDefault().register(this);

        // 2.Start remote service
        appContext.startService(new Intent(appContext, RemoteService.class));

        // 3.wait
        Thread.sleep(1000);

        // 3. test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            Add add = new Add(i, i);
            AddResult result = QuickEvent.getDefault().request(add, AddResult.class);
            assertEquals(2 * i, result.getSum().intValue());
        }
        Log.v("QuickEvent", "TestRunning:" + (System.currentTimeMillis() - startTime));

        // 4. release
        QuickEvent.getDefault().unregister(this);

    }
}
