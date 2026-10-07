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

    @Test public void duplicateDetectionAndClientFinancialsWork() {
        DBHelper db=new DBHelper(context);
        DBHelper.Client ahmed=db.client(1);
        assertNotNull(ahmed);
        assertTrue(db.clientExists("ntn",ahmed.ntn,0));
        assertTrue(db.duplicateWarning(ahmed.ntn,ahmed.cnic,ahmed.whatsapp,ahmed.phone,0).contains("NTN"));
        assertTrue(db.clientTotalPayments(1)>=db.clientPaidPayments(1));
        assertTrue(db.clientOutstandingPayments(1)>=0);
        assertTrue(db.countClientPendingFilings(1)>=0);
        assertTrue(db.countClientOpenTasks(1)>=0);
        db.close();
    }

    @Test public void taskDeleteAndReminderSnoozeWork() {
        DBHelper db=new DBHelper(context);
        long task=db.addTask(1,"Temporary QA Task","Today","Low","Delete me");
        assertNotNull(db.task(task));
        db.deleteTask(task);
        assertNull(db.task(task));

        long at=System.currentTimeMillis()+60000L;
        long rem=db.addReminder(1,"Snooze QA","Test",at,"Once","Local");
        long later=at+86400000L;
        db.snoozeReminder(rem,later);
        DBHelper.Reminder found=null;
        for(DBHelper.Reminder r:db.reminders(0))if(r.id==rem)found=r;
        assertNotNull(found);
        assertEquals(later,found.at);
        db.close();
    }

    @Test public void documentAndReceivableIntelligenceWork() {
        DBHelper db=new DBHelper(context);
        assertTrue(db.countMissingDocuments()>=0);
        assertTrue(db.countClientMissingDocuments(1)>=0);
        assertFalse(db.allPayments().isEmpty());
        assertFalse(db.allDocuments().isEmpty());
        db.close();
    }

    @Test public void executiveAnalyticsWork() {
        DBHelper db=new DBHelper(context);
        assertTrue(db.countPendingDueWithinDays(7)>=0);
        assertTrue(db.countPendingDueWithinDays(30)>=0);
        assertTrue(db.countClientsWithOutstandingFees()>=0);
        assertTrue(db.receivablesAging("current")>=0);
        assertTrue(db.receivablesAging("1-30")>=0);
        assertTrue(db.receivablesAging("31-60")>=0);
        assertTrue(db.receivablesAging("61+")>=0);
        assertTrue(db.collectionRate()>=0 && db.collectionRate()<=100.0);
        assertTrue(db.countTaxType("Income")>=0);
        assertTrue(db.latestAuditTime(1)>=0);
        db.close();
    }

    @Test public void noticesExpensesAndProfitWork() {
        DBHelper db=new DBHelper(context);
        assertTrue(db.countOpenNotices()>=1);
        assertFalse(db.notices(0).isEmpty());
        assertFalse(db.expenses().isEmpty());
        assertTrue(db.totalExpenses()>0);
        double before=db.totalExpenses();
        long ex=db.addExpense("QA Expense","Testing",1000,"07 Oct 2026","Automated test");
        assertTrue(ex>0);
        assertTrue(db.totalExpenses()>=before+1000);
        db.deleteExpense(ex);
        long notice=db.addNotice(1,"QA Notice","QA-001","20 Oct 2026","Open","Automated test");
        assertTrue(notice>0);
        db.setNoticeStatus(notice,"Resolved");
        boolean resolved=false;
        for(DBHelper.Notice n:db.notices(1))if(n.id==notice&&"Resolved".equals(n.status))resolved=true;
        assertTrue(resolved);
        db.deleteNotice(notice);
        db.close();
    }

    @Test public void workloadAnalyticsAreAvailable() {
        DBHelper db=new DBHelper(context);
        assertTrue(db.countOverdueFilings()>=0);
        assertTrue(db.countHighPriorityTasks()>=0);
        assertTrue(db.countRemindersNext7Days()>=0);
        assertTrue(db.outstandingPayments()>=0);
        assertFalse(db.allFilings().isEmpty());
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