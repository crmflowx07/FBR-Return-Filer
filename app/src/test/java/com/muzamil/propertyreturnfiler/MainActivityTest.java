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
        assertNotNull("Dashboard must show Good Morning", findText(dashboardRoot, "Good Morning"));
        assertNotNull("Dashboard must show Total Clients", findText(dashboardRoot, "Total Clients"));
        assertNotNull("Dashboard must show Quick Actions", findText(dashboardRoot, "Quick Actions"));
        assertNull("Get Started must not exist in V13", findText(dashboardRoot, "Get Started"));

        View clients = findExactText(dashboardRoot, "Clients");
        assertNotNull("Dashboard must expose Clients", clients);
        View clientsTarget = clients.isClickable() ? clients : (View) clients.getParent();
        assertNotNull("Clients clickable parent must exist", clientsTarget);
        assertTrue("Clients target must have click listener", clientsTarget.hasOnClickListeners());
        assertTrue("Clients click listener must execute", clientsTarget.callOnClick());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertNotNull("Clients screen must show search field", findHint(activity.getWindow().getDecorView(), "Search by name"));
        assertNotNull("Seeded client Ahmed Raza must render", findText(activity.getWindow().getDecorView(), "Ahmed Raza"));
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