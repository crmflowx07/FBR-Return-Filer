package com.muzamil.propertyreturnfiler;

import static org.junit.Assert.*;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class BackupAndReminderTest {
    private Context context;

    @Before public void setup(){
        context=RuntimeEnvironment.getApplication();
        context.deleteDatabase("property_return_filer.db");
    }

    @Test public void backupRoundTripPreservesCoreData() throws Exception {
        DBHelper db=new DBHelper(context);
        int clients=db.countClients();
        int filed=db.countFiled();
        int pending=db.countPending();
        String json=db.exportJson();
        assertTrue(json.contains("FBR_RETURN_FILER_BACKUP"));
        assertTrue(json.contains("Ahmed Raza"));

        db.deleteClient(1);
        assertTrue(db.countClients()<clients);

        db.importJson(json);
        assertEquals(clients,db.countClients());
        assertEquals(filed,db.countFiled());
        assertEquals(pending,db.countPending());
        assertNotNull(db.client(1));
        db.close();
    }

    @Test public void reminderCanCompleteAndMove() {
        DBHelper db=new DBHelper(context);
        long now=System.currentTimeMillis()+60000L;
        long id=db.addReminder(1,"QA Reminder","Test",now,"Once","Local");
        db.completeReminder(id);
        DBHelper.Reminder found=null;
        for(DBHelper.Reminder r:db.reminders(0))if(r.id==id)found=r;
        assertNotNull(found);
        assertEquals("Completed",found.status);

        long next=now+3600000L;
        db.moveReminder(id,next);
        found=null;
        for(DBHelper.Reminder r:db.reminders(0))if(r.id==id)found=r;
        assertNotNull(found);
        assertEquals("Scheduled",found.status);
        assertEquals(next,found.at);
        db.close();
    }
}