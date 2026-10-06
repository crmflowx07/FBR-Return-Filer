package com.muzamil.propertyreturnfiler;

import android.app.*;
import android.content.*;
import android.os.Build;

public final class ReminderScheduler {
    private ReminderScheduler(){}

    public static void schedule(Context c,long id,String title,String msg,long at,String repeat){
        Intent i=new Intent(c,ReminderReceiver.class);
        i.putExtra("id",id);
        i.putExtra("title",title);
        i.putExtra("message",msg);
        i.putExtra("repeat",repeat);
        PendingIntent pi=PendingIntent.getBroadcast(
            c,(int)(id%Integer.MAX_VALUE),i,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        try{
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms())
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            else if(Build.VERSION.SDK_INT>=23)
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            else
                am.setExact(AlarmManager.RTC_WAKEUP,at,pi);
        }catch(SecurityException e){
            am.set(AlarmManager.RTC_WAKEUP,at,pi);
        }
    }

    public static void cancel(Context c,long id){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        Intent i=new Intent(c,ReminderReceiver.class);
        PendingIntent pi=PendingIntent.getBroadcast(
            c,(int)(id%Integer.MAX_VALUE),i,
            PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE
        );
        if(pi!=null){ am.cancel(pi); pi.cancel(); }
    }
}