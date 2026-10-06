package com.muzamil.propertyreturnfiler;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import org.json.*;

public class DBHelper extends SQLiteOpenHelper {
    public static final int VERSION = 5;
    public DBHelper(Context c){ super(c,"property_return_filer.db",null,VERSION); }

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,whatsapp TEXT,phone TEXT,cnic TEXT,ntn TEXT,business TEXT,taxType TEXT,status TEXT,nextDue TEXT,email TEXT,address TEXT,notes TEXT,createdAt INTEGER)");
        db.execSQL("CREATE TABLE filings(id INTEGER PRIMARY KEY AUTOINCREMENT,clientId INTEGER,month TEXT,year INTEGER,type TEXT,dueDate TEXT,status TEXT,filedDate TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,clientId INTEGER,title TEXT,message TEXT,scheduledAt INTEGER,repeatRule TEXT,status TEXT,channel TEXT)");
        db.execSQL("CREATE TABLE payments(id INTEGER PRIMARY KEY AUTOINCREMENT,clientId INTEGER,title TEXT,amount REAL,dueDate TEXT,status TEXT,notes TEXT)");
        db.execSQL("CREATE TABLE documents(id INTEGER PRIMARY KEY AUTOINCREMENT,clientId INTEGER,title TEXT,category TEXT,status TEXT,notes TEXT)");
        seedExactDemo(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        // Safe reset for early test builds whose schemas were different.
        // DROP IF EXISTS avoids crashes when upgrading from v1/v2/v3 builds
        // that did not yet contain all ERP tables.
        db.execSQL("DROP TABLE IF EXISTS documents");
        db.execSQL("DROP TABLE IF EXISTS payments");
        db.execSQL("DROP TABLE IF EXISTS reminders");
        db.execSQL("DROP TABLE IF EXISTS filings");
        db.execSQL("DROP TABLE IF EXISTS clients");
        onCreate(db);
    }

    private long client(SQLiteDatabase db,String n,String w,String ntn,String business,String type,String status,String due,String cnic){
        ContentValues v=new ContentValues();
        v.put("name",n);v.put("whatsapp",w);v.put("phone",w);v.put("ntn",ntn);v.put("business",business);
        v.put("taxType",type);v.put("status",status);v.put("nextDue",due);v.put("cnic",cnic);
        v.put("email",n.toLowerCase(Locale.US).replace(" ",".")+"@example.com");
        v.put("address","Punjab, Pakistan");v.put("notes","Regular client. Keep monthly filing and reminder record updated.");
        v.put("createdAt",System.currentTimeMillis());
        return db.insert("clients",null,v);
    }
    private void filing(SQLiteDatabase db,long cid,String month,int year,String type,String due,String status){
        ContentValues v=new ContentValues();v.put("clientId",cid);v.put("month",month);v.put("year",year);v.put("type",type);v.put("dueDate",due);v.put("status",status);
        if("Filed".equals(status))v.put("filedDate",due);
        db.insert("filings",null,v);
    }
    private void reminder(SQLiteDatabase db,long cid,String title,String msg,long at,String repeat){
        ContentValues v=new ContentValues();v.put("clientId",cid);v.put("title",title);v.put("message",msg);v.put("scheduledAt",at);v.put("repeatRule",repeat);v.put("status","Scheduled");v.put("channel","Local + WhatsApp");db.insert("reminders",null,v);
    }

    private void seedExactDemo(SQLiteDatabase db){
        String[][] core={
          {"Ahmed Raza","+923001234567","1234567-8","Online Electronics Store","Salaried / Business","Active","15 Aug 2026","35202-1234567-1"},
          {"Sana Khan","+923219876543","3456789-0","Sana Boutique","Income Tax Return (ITR)","Filed","31 Jul 2026","35202-3456789-2"},
          {"Faisal Ahmed","+923334567890","7896543-2","FA Consultants","Income Tax Return (ITR)","Pending","31 Jul 2026","35202-7896543-3"},
          {"Ayesha Malik","+923005550123","1122233-4","Ayesha Traders","Sales Tax Return (STR)","Active","31 Aug 2026","35202-1122233-4"},
          {"Usman Sheikh","+923212223344","9876543-2","Usman Enterprises","Sales Tax Return (STR)","Filed","15 Aug 2026","35202-9876543-5"},
          {"Mubeen Traders","+923347778899","4567891-0","Mubeen Traders","Income Tax Return (ITR)","Pending","15 Sep 2026","35202-4567891-6"},
          {"Zahid Hussain","+923001239876","3344556-6","Zahid & Co.","Income Tax Return (ITR)","Active","15 Sep 2026","35202-3344556-7"},
          {"Ali Enterprises","+923111112222","1234567-8","Ali Enterprises","Sales Tax Return (STR)","Pending","15 Jul 2026","35202-2233445-8"}
        };
        long[] ids=new long[48];
        for(int i=0;i<core.length;i++)ids[i]=client(db,core[i][0],core[i][1],core[i][2],core[i][3],core[i][4],core[i][5],core[i][6],core[i][7]);
        for(int i=8;i<48;i++){
            int num=i+1;
            String name=(i%3==0?"Hassan ":i%3==1?"Bilal ":"Kamran ")+num;
            String phone="+923"+String.format(Locale.US,"%09d",100000000+i*7919);
            String ntn=String.format(Locale.US,"%07d-%d",2200000+i*113,(i%9)+1);
            String type=i%2==0?"Income Tax Return (ITR)":"Sales Tax Return (STR)";
            String status=i<20?"Pending":(i<48?"Filed":"Active");
            String due=i%3==0?"31 Jul 2026":i%3==1?"31 Aug 2026":"15 Sep 2026";
            ids[i]=client(db,name,phone,ntn,name+" Business",type,status,due,"35202-"+String.format(Locale.US,"%07d",5000000+i)+"-"+((i%9)+1));
        }

        // exactly 12 pending filings
        for(int i=0;i<12;i++){
            String month=i<4?"July":i<8?"August":"September";
            String due=i<4?"31 Jul 2026":i<8?"31 Aug 2026":"15 Sep 2026";
            filing(db,ids[i],month,2026,i%2==0?"Income Tax Return (ITR)":"Sales Tax Return (STR)",due,"Pending");
        }
        // exactly 28 filed records
        for(int i=12;i<40;i++){
            String month=i%2==0?"July":"August";
            String due=i%2==0?"20 Jul 2026":"20 Aug 2026";
            filing(db,ids[i],month,2026,i%2==0?"Income Tax Return (ITR)":"Sales Tax Return (STR)",due,"Filed");
        }

        long now=System.currentTimeMillis();
        reminder(db,ids[0],"Ahmed Raza Return","Kindly apne return documents provide kar dein.",now+86400000L,"Monthly");
        reminder(db,ids[2],"Faisal Ahmed Follow-up","Income tax return due soon.",now+2*86400000L,"Once");
        reminder(db,ids[3],"Ayesha Malik STR","Sales tax return reminder.",now+3*86400000L,"Monthly");
        reminder(db,ids[5],"Mubeen Traders ITR","Return documents required.",now+4*86400000L,"Monthly");
        reminder(db,ids[7],"Ali Enterprises STR","Sales tax return is overdue.",now+5*86400000L,"Once");

        ContentValues p=new ContentValues();p.put("clientId",ids[0]);p.put("title","Monthly Consultancy");p.put("amount",25000);p.put("dueDate","10 Aug 2026");p.put("status","Paid");p.put("notes","Received");db.insert("payments",null,p);
        ContentValues d=new ContentValues();d.put("clientId",ids[0]);d.put("title","Bank Statement");d.put("category","FBR Documents");d.put("status","Received");d.put("notes","July statement");db.insert("documents",null,d);
    }

    public long saveClient(long id,String name,String whatsapp,String phone,String cnic,String ntn,String business,String taxType,String status,String nextDue,String email,String address,String notes){
        ContentValues v=new ContentValues();v.put("name",name);v.put("whatsapp",whatsapp);v.put("phone",phone);v.put("cnic",cnic);v.put("ntn",ntn);v.put("business",business);v.put("taxType",taxType);v.put("status",status);v.put("nextDue",nextDue);v.put("email",email);v.put("address",address);v.put("notes",notes);
        if(id>0){getWritableDatabase().update("clients",v,"id=?",new String[]{String.valueOf(id)});return id;}
        v.put("createdAt",System.currentTimeMillis());return getWritableDatabase().insert("clients",null,v);
    }
    public void deleteClient(long id){SQLiteDatabase d=getWritableDatabase();String[] a={""+id};d.delete("filings","clientId=?",a);d.delete("reminders","clientId=?",a);d.delete("payments","clientId=?",a);d.delete("documents","clientId=?",a);d.delete("clients","id=?",a);}

    public Client client(long id){Cursor c=getReadableDatabase().rawQuery("SELECT id,name,whatsapp,phone,cnic,ntn,business,taxType,status,nextDue,email,address,notes FROM clients WHERE id=?",new String[]{""+id});Client x=null;if(c.moveToFirst())x=fromClient(c);c.close();return x;}
    private Client fromClient(Cursor c){return new Client(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getString(9),c.getString(10),c.getString(11),c.getString(12));}
    public List<Client> clients(String q){ArrayList<Client> out=new ArrayList<>();String like="%"+(q==null?"":q)+"%";Cursor c=getReadableDatabase().rawQuery("SELECT id,name,whatsapp,phone,cnic,ntn,business,taxType,status,nextDue,email,address,notes FROM clients WHERE name LIKE ? OR business LIKE ? OR whatsapp LIKE ? OR ntn LIKE ? ORDER BY id ASC",new String[]{like,like,like,like});while(c.moveToNext())out.add(fromClient(c));c.close();return out;}
    private int scalar(String sql){Cursor c=getReadableDatabase().rawQuery(sql,null);c.moveToFirst();int v=c.getInt(0);c.close();return v;}
    public int countClients(){return scalar("SELECT COUNT(*) FROM clients");}
    public int countPending(){return scalar("SELECT COUNT(*) FROM filings WHERE status='Pending'");}
    public int countFiled(){return scalar("SELECT COUNT(*) FROM filings WHERE status='Filed' OR status='Completed'");}
    public int countReminders(){return scalar("SELECT COUNT(*) FROM reminders WHERE status='Scheduled'");}
    public int countActiveClients(){return scalar("SELECT COUNT(*) FROM clients WHERE status='Active'");}
    public int countFiledClients(){return scalar("SELECT COUNT(*) FROM clients WHERE status='Filed'");}
    public int countPendingClients(){return scalar("SELECT COUNT(*) FROM clients WHERE status='Pending'");}
    public int countDocuments(){return scalar("SELECT COUNT(*) FROM documents");}
    public int countPayments(){return scalar("SELECT COUNT(*) FROM payments");}
    public int countPendingPayments(){return scalar("SELECT COUNT(*) FROM payments WHERE status!='Paid'");}
    public double totalPayments(){
        Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM payments",null);
        c.moveToFirst();double v=c.getDouble(0);c.close();return v;
    }
    public double paidPayments(){
        Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='Paid'",null);
        c.moveToFirst();double v=c.getDouble(0);c.close();return v;
    }
    public List<Client> clientsByStatus(String status){
        ArrayList<Client> out=new ArrayList<>();
        Cursor c=getReadableDatabase().rawQuery("SELECT id,name,whatsapp,phone,cnic,ntn,business,taxType,status,nextDue,email,address,notes FROM clients WHERE status=? ORDER BY id ASC",new String[]{status});
        while(c.moveToNext())out.add(fromClient(c));c.close();return out;
    }

    public long addFiling(long clientId,String month,int year,String type,String due,String status,String notes){ContentValues v=new ContentValues();v.put("clientId",clientId);v.put("month",month);v.put("year",year);v.put("type",type);v.put("dueDate",due);v.put("status",status);v.put("notes",notes);return getWritableDatabase().insert("filings",null,v);}
    public void setFilingStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",status);if("Filed".equals(status)||"Completed".equals(status))v.put("filedDate",new java.text.SimpleDateFormat("dd MMM yyyy",Locale.US).format(new Date()));getWritableDatabase().update("filings",v,"id=?",new String[]{""+id});}
    public List<Filing> filings(long clientId){ArrayList<Filing> l=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,month,year,type,dueDate,status,filedDate,notes FROM filings WHERE clientId=? ORDER BY year DESC,id DESC",new String[]{""+clientId});while(c.moveToNext())l.add(new Filing(c.getLong(0),c.getString(1),c.getInt(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7)));c.close();return l;}

    public long addReminder(long clientId,String title,String message,long at,String repeat,String channel){ContentValues v=new ContentValues();v.put("clientId",clientId);v.put("title",title);v.put("message",message);v.put("scheduledAt",at);v.put("repeatRule",repeat);v.put("status","Scheduled");v.put("channel",channel);return getWritableDatabase().insert("reminders",null,v);}
    public List<Reminder> reminders(long clientId){ArrayList<Reminder> l=new ArrayList<>();String where=clientId>0?" WHERE r.clientId="+clientId:"";Cursor c=getReadableDatabase().rawQuery("SELECT r.id,r.clientId,c.name,r.title,r.message,r.scheduledAt,r.repeatRule,r.status,r.channel FROM reminders r LEFT JOIN clients c ON c.id=r.clientId"+where+" ORDER BY r.scheduledAt ASC",null);while(c.moveToNext())l.add(new Reminder(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5),c.getString(6),c.getString(7),c.getString(8)));c.close();return l;}
    public void deleteReminder(long id){getWritableDatabase().delete("reminders","id=?",new String[]{""+id});}
    public void completeReminder(long id){ContentValues v=new ContentValues();v.put("status","Completed");getWritableDatabase().update("reminders",v,"id=?",new String[]{""+id});}
    public void moveReminder(long id,long at){ContentValues v=new ContentValues();v.put("scheduledAt",at);v.put("status","Scheduled");getWritableDatabase().update("reminders",v,"id=?",new String[]{""+id});}

    public long addPayment(long clientId,String title,double amount,String due,String status,String notes){ContentValues v=new ContentValues();v.put("clientId",clientId);v.put("title",title);v.put("amount",amount);v.put("dueDate",due);v.put("status",status);v.put("notes",notes);return getWritableDatabase().insert("payments",null,v);}
    public void setPaymentStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",status);getWritableDatabase().update("payments",v,"id=?",new String[]{""+id});}
    public List<Payment> payments(long clientId){ArrayList<Payment> l=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,amount,dueDate,status,notes FROM payments WHERE clientId=? ORDER BY id DESC",new String[]{""+clientId});while(c.moveToNext())l.add(new Payment(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getString(4),c.getString(5)));c.close();return l;}
    public long addDocument(long clientId,String title,String category,String status,String notes){ContentValues v=new ContentValues();v.put("clientId",clientId);v.put("title",title);v.put("category",category);v.put("status",status);v.put("notes",notes);return getWritableDatabase().insert("documents",null,v);}
    public void setDocumentStatus(long id,String status){ContentValues v=new ContentValues();v.put("status",status);getWritableDatabase().update("documents",v,"id=?",new String[]{""+id});}
    public List<Document> documents(long clientId){ArrayList<Document> l=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,category,status,notes FROM documents WHERE clientId=? ORDER BY id DESC",new String[]{""+clientId});while(c.moveToNext())l.add(new Document(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)));c.close();return l;}

    public String exportJson() throws JSONException {
        JSONObject root=new JSONObject();
        root.put("format","FBR_RETURN_FILER_BACKUP");
        root.put("version",1);
        root.put("exportedAt",System.currentTimeMillis());
        String[] tables={"clients","filings","reminders","payments","documents"};
        SQLiteDatabase db=getReadableDatabase();
        for(String table:tables){
            JSONArray arr=new JSONArray();
            Cursor cur=db.rawQuery("SELECT * FROM "+table,null);
            String[] cols=cur.getColumnNames();
            while(cur.moveToNext()){
                JSONObject row=new JSONObject();
                for(int i=0;i<cols.length;i++){
                    int type=cur.getType(i);
                    if(type==Cursor.FIELD_TYPE_NULL) row.put(cols[i],JSONObject.NULL);
                    else if(type==Cursor.FIELD_TYPE_INTEGER) row.put(cols[i],cur.getLong(i));
                    else if(type==Cursor.FIELD_TYPE_FLOAT) row.put(cols[i],cur.getDouble(i));
                    else row.put(cols[i],cur.getString(i));
                }
                arr.put(row);
            }
            cur.close();
            root.put(table,arr);
        }
        return root.toString(2);
    }

    public void importJson(String json) throws JSONException {
        JSONObject root=new JSONObject(json);
        if(!"FBR_RETURN_FILER_BACKUP".equals(root.optString("format"))) throw new JSONException("Invalid backup format");
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try{
            db.delete("documents",null,null);
            db.delete("payments",null,null);
            db.delete("reminders",null,null);
            db.delete("filings",null,null);
            db.delete("clients",null,null);
            importTable(db,root.optJSONArray("clients"),"clients");
            importTable(db,root.optJSONArray("filings"),"filings");
            importTable(db,root.optJSONArray("reminders"),"reminders");
            importTable(db,root.optJSONArray("payments"),"payments");
            importTable(db,root.optJSONArray("documents"),"documents");
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private void importTable(SQLiteDatabase db,JSONArray arr,String table) throws JSONException {
        if(arr==null)return;
        for(int i=0;i<arr.length();i++){
            JSONObject row=arr.getJSONObject(i);
            ContentValues v=new ContentValues();
            Iterator<String> it=row.keys();
            while(it.hasNext()){
                String k=it.next();
                Object val=row.opt(k);
                if(val==null || val==JSONObject.NULL) v.putNull(k);
                else if(val instanceof Integer || val instanceof Long) v.put(k,((Number)val).longValue());
                else if(val instanceof Float || val instanceof Double) v.put(k,((Number)val).doubleValue());
                else v.put(k,String.valueOf(val));
            }
            db.insertOrThrow(table,null,v);
        }
    }

    public static class Client{public long id;public String name,whatsapp,phone,cnic,ntn,business,taxType,status,nextDue,email,address,notes;Client(long id,String name,String whatsapp,String phone,String cnic,String ntn,String business,String taxType,String status,String nextDue,String email,String address,String notes){this.id=id;this.name=name;this.whatsapp=whatsapp;this.phone=phone;this.cnic=cnic;this.ntn=ntn;this.business=business;this.taxType=taxType;this.status=status;this.nextDue=nextDue;this.email=email;this.address=address;this.notes=notes;}}
    public static class Filing{public long id;public String month,type,due,status,filed,notes;public int year;Filing(long i,String m,int y,String t,String d,String s,String f,String n){id=i;month=m;year=y;type=t;due=d;status=s;filed=f;notes=n;}}
    public static class Reminder{public long id,clientId,at;public String client,title,message,repeat,status,channel;Reminder(long i,long c,String cn,String t,String m,long a,String r,String s,String ch){id=i;clientId=c;client=cn;title=t;message=m;at=a;repeat=r;status=s;channel=ch;}}
    public static class Payment{public long id;public String title,due,status,notes;public double amount;Payment(long i,String t,double a,String d,String s,String n){id=i;title=t;amount=a;due=d;status=s;notes=n;}}
    public static class Document{public long id;public String title,category,status,notes;Document(long i,String t,String c,String s,String n){id=i;title=t;category=c;status=s;notes=n;}}
}