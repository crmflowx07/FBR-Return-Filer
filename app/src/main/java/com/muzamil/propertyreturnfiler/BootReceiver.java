package com.muzamil.propertyreturnfiler;

import android.content.*;
import java.util.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){
        if(intent==null)return;
        String a=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(a) && !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(a))return;
        DBHelper db=new DBHelper(context);
        long now=System.currentTimeMillis();
        for(DBHelper.Reminder r:db.reminders(0)){
            if(!"Scheduled".equals(r.status))continue;
            long at=r.at;
            if(at<now) at=now+60000L;
            ReminderScheduler.schedule(context,r.id,r.title,r.message,at,r.repeat);
        }
        db.close();
    }
}