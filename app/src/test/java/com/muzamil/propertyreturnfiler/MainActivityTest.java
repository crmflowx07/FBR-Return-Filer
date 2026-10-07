package com.muzamil.propertyreturnfiler;

import static org.junit.Assert.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivityTest {

    @Test
    public void directDashboardAndClientsFlowWorks() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        View dashboardRoot;
        try {
            Field rootField=MainActivity.class.getDeclaredField("root");
            rootField.setAccessible(true);
            dashboardRoot=(View)rootField.get(activity);
        } catch(Exception e) {
            throw new AssertionError(e);
        }
        assertNotNull("App must launch directly into dashboard", dashboardRoot);
        assertNotNull("Dashboard must show dynamic greeting", findText(dashboardRoot, "Good "));
        assertNotNull("Dashboard must show Total Clients", findText(dashboardRoot, "Total Clients"));
        assertNotNull("Dashboard must show Quick Actions", findText(dashboardRoot, "Quick Actions"));
        assertNull("Get Started must not exist in V19", findText(dashboardRoot, "Get Started"));

        try {
            Method showClients=MainActivity.class.getDeclaredMethod("showClients",String.class);
            showClients.setAccessible(true);
            showClients.invoke(activity,"");
        } catch(Exception e) {
            throw new AssertionError("Clients navigation must execute",e);
        }
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Field rootField2;
        View clientsRoot;
        try{
            rootField2=MainActivity.class.getDeclaredField("root");
            rootField2.setAccessible(true);
            clientsRoot=(View)rootField2.get(activity);
        }catch(Exception e){throw new AssertionError(e);}
        assertNotNull("Clients screen must show search field", findHint(clientsRoot, "Search by name"));
        assertNotNull("Seeded client Ahmed Raza must render", findText(clientsRoot, "Ahmed Raza"));
    }

    private View findText(View v, String needle) {
        if (v instanceof TextView) {
            CharSequence s = ((TextView)v).getText();
            if (s != null && s.toString().contains(needle)) return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View hit=findText(g.getChildAt(i),needle);
                if(hit!=null)return hit;
            }
        }
        return null;
    }

    private View findExactText(View v, String target) {
        if (v instanceof TextView) {
            CharSequence s=((TextView)v).getText();
            if(s!=null && s.toString().equals(target)) return v;
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View hit=findExactText(g.getChildAt(i),target);
                if(hit!=null)return hit;
            }
        }
        return null;
    }

    private View findHint(View v, String needle) {
        if (v instanceof EditText) {
            CharSequence s=((EditText)v).getHint();
            if(s!=null && s.toString().contains(needle))return v;
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View hit=findHint(g.getChildAt(i),needle);
                if(hit!=null)return hit;
            }
        }
        return null;
    }
}