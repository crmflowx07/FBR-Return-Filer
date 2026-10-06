package com.muzamil.propertyreturnfiler;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.util.*;

public class ReminderReceiver extends BroadcastReceiver{
    public static final String CHANNEL="fbr_reminders";

    @Override public void onReceive(Context c,Intent i){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"FBR & Client Reminders",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("FBR returns, client documents and payment reminders");
            nm.createNotificationChannel(ch);
        }

        long id=i==null?0:i.getLongExtra("id",0);
        String title=i==null?null:i.getStringExtra("title");
        String msg=i==null?null:i.getStringExtra("message");
        String repeat=i==null?null:i.getStringExtra("repeat");
        if(title==null||title.trim().isEmpty())title="FBR Return Filer Reminder";
        if(msg==null||msg.trim().isEmpty())msg="Client follow-up due hai.";

        Intent open=new Intent(c,MainActivity.class);
        open.putExtra("open","reminders");
        PendingIntent pi=PendingIntent.getActivity(c,(int)(id%Integer.MAX_VALUE),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notification)
         .setContentTitle(title)
         .setContentText(msg)
         .setStyle(new Notification.BigTextStyle().bigText(msg))
         .setContentIntent(pi)
         .setAutoCancel(true);
        nm.notify((int)(id>0?id:System.currentTimeMillis()%999999),b.build());

        if(id>0){
            DBHelper db=new DBHelper(c);
            if("Monthly".equalsIgnoreCase(repeat)){
                Calendar cal=Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.add(Calendar.MONTH,1);
                long next=cal.getTimeInMillis();
                db.moveReminder(id,next);
                ReminderScheduler.schedule(c,id,title,msg,next,"Monthly");
            } else {
                db.completeReminder(id);
            }
            db.close();
        }
    }
}