package com.muzamil.propertyreturnfiler;

import static org.junit.Assert.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class MainActivityFlowTest {
    private TextView findText(View v, String needle){
        if(v instanceof TextView){
            CharSequence t=((TextView)v).getText();
            if(t!=null && t.toString().contains(needle)) return (TextView)v;
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                TextView found=findText(g.getChildAt(i),needle);
                if(found!=null) return found;
            }
        }
        return null;
    }

    @Test public void appLaunchesDirectlyToDashboard() throws Exception {
        MainActivity activity=Robolectric.buildActivity(MainActivity.class).setup().get();
        Field rf=MainActivity.class.getDeclaredField("root");
        rf.setAccessible(true);
        View dashboard=(View)rf.get(activity);
        assertNotNull("V13 must create dashboard root on launch",dashboard);
        assertNull("V13 must not show Get Started",findText(dashboard,"Get Started"));
        assertNotNull("V13 must launch dashboard immediately",findText(dashboard,"Good Morning"));
        assertNotNull("Dashboard must show Total Clients",findText(dashboard,"Total Clients"));
    }

    @Test public void dashboardRendersWithoutCrash() throws Exception {
        MainActivity activity=Robolectric.buildActivity(MainActivity.class).setup().get();
        Method m=MainActivity.class.getDeclaredMethod("showDashboard");
        m.setAccessible(true);
        m.invoke(activity);
        Field rf=MainActivity.class.getDeclaredField("root");
        rf.setAccessible(true);
        View dashboard=(View)rf.get(activity);
        assertNotNull("Dashboard root must be created",dashboard);
        assertNotNull("Dashboard must show Total Clients",findText(dashboard,"Total Clients"));
        assertNotNull("Dashboard must show Quick Actions",findText(dashboard,"Quick Actions"));
        assertNotNull("Dashboard must show Good Morning",findText(dashboard,"Good Morning"));
    }

    @Test public void databaseSeedsCoreErpData(){
        MainActivity activity=Robolectric.buildActivity(MainActivity.class).setup().get();
        DBHelper db=new DBHelper(activity);
        assertEquals(48,db.countClients());
        assertEquals(12,db.countPending());
        assertEquals(28,db.countFiled());
        assertEquals(5,db.countReminders());
        db.close();
    }
}
