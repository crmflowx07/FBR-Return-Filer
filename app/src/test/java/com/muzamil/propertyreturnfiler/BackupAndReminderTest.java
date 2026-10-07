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
        int tasks=db.countPendingTasks();
        int audit=db.auditLogs(0).size();
        String json=db.exportJson();
        assertTrue(json.contains("FBR_RETURN_FILER_BACKUP"));
        assertTrue(json.contains("Ahmed Raza"));
        assertTrue(json.contains("tasks"));
        assertTrue(json.contains("audit_logs"));

        db.deleteClient(1);
        assertTrue(db.countClients()<clients);

        db.importJson(json);
        assertEquals(clients,db.countClients());
        assertEquals(filed,db.countFiled());
        assertEquals(pending,db.countPending());
        assertEquals(tasks,db.countPendingTasks());
        assertEquals(audit,db.auditLogs(0).size());
        assertNotNull(db.client(1));
        db.close();
    }

    @Test public void tasksAndAuditTrailWork() {
        DBHelper db=new DBHelper(context);
        int before=db.countPendingTasks();
        long id=db.addTask(1,"QA verify documents","Today","High","Automated test");
        assertEquals(before+1,db.countPendingTasks());
        assertNotNull(db.task(id));
        db.setTaskStatus(id,"Completed");
        assertEquals("Completed",db.task(id).status);
        boolean logged=false;
        for(DBHelper.AuditLog log:db.auditLogs(1)) if(log.action.contains("TASK_COMPLETED")) logged=true;
        assertTrue("Completed task must be audited",logged);
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