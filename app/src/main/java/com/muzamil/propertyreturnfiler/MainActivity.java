package com.muzamil.propertyreturnfiler;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.util.Log;
import android.widget.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private final int BLUE = Color.rgb(24,117,246);
    private final int BLUE2 = Color.rgb(36,142,255);
    private final int NAVY = Color.rgb(10,35,92);
    private final int INK = Color.rgb(17,25,45);
    private final int MUTED = Color.rgb(110,124,150);
    private final int BG = Color.rgb(247,249,253);
    private final int LINE = Color.rgb(229,235,245);
    private final int GREEN = Color.rgb(25,188,103);
    private final int RED = Color.rgb(243,73,79);
    private final int ORANGE = Color.rgb(255,157,56);
    private final int PURPLE = Color.rgb(137,78,246);

    private DBHelper db;
    private LinearLayout body;
    private LinearLayout root;
    private String activeNav = "home";
    String lastDashboardError = "";
    private boolean dashboardOpened = false;
    private static final int REQ_EXPORT_BACKUP=501;
    private static final int REQ_IMPORT_BACKUP=502;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        // Keep first launch instant. Database is opened only when entering the dashboard.
        db = new DBHelper(this);
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
        final String requested=getIntent()==null?null:getIntent().getStringExtra("open");
        // V13: no welcome/Get Started gate. Enter the app immediately.
        if("reminders".equals(requested)){
            try{
                db.getWritableDatabase();
                dashboardOpened=true;
                showReminders();
            }catch(Throwable e){
                Log.e("FBRReturnFiler","Reminder deep link failed",e);
                dashboardOpened=false;
                openDashboardSafe();
            }
        }else{
            openDashboardSafe();
        }
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent!=null && "reminders".equals(intent.getStringExtra("open"))){
            try{db.getWritableDatabase();dashboardOpened=true;showReminders();}
            catch(Throwable e){openDashboardSafe();}
        }
    }

    @Override public void onBackPressed(){
        if(!"home".equals(activeNav) && dashboardOpened){showDashboard();return;}
        super.onBackPressed();
    }

    private int dp(int v){ return (int)(v * getResources().getDisplayMetrics().density); }

    private GradientDrawable solid(int color, int radius){
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    private GradientDrawable outline(int color, int radius, int strokeColor){
        GradientDrawable g = solid(color, radius);
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    private GradientDrawable gradient(int c1, int c2, int radius){
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1,c2});
        g.setCornerRadius(dp(radius));
        return g;
    }

    private TextView tv(String s, int sp, int color, boolean bold){
        TextView t = new TextView(this);
        t.setText(s == null ? "" : s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView iconText(String label, int icon, int sp, int color, boolean bold){
        TextView t = tv(label, sp, color, bold);
        t.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);
        t.setCompoundDrawablePadding(dp(8));
        return t;
    }

    private LinearLayout card(int radius){
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(14),dp(14),dp(14));
        c.setBackground(solid(Color.WHITE,radius));
        c.setElevation(dp(1));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(10));
        c.setLayoutParams(p);
        return c;
    }

    private Space spacer(int h){
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));
        return s;
    }

    private TextView section(String s){
        TextView t = tv(s,17,INK,true);
        t.setPadding(0,dp(12),0,dp(9));
        return t;
    }

    private Button actionButton(String label, int icon, boolean primary){
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(primary ? Color.WHITE : BLUE);
        b.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);
        b.setCompoundDrawablePadding(dp(7));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12),0,dp(12),0);
        b.setBackground(primary ? gradient(BLUE2,BLUE,18) : outline(Color.WHITE,18,LINE));
        b.setElevation(primary ? dp(2) : 0);
        return b;
    }

    private TextView avatar(String name, int size){
        TextView a = tv("", size/3, Color.WHITE, true);
        a.setGravity(Gravity.CENTER);
        int res = avatarRes(name);
        if(res!=0){
            a.setBackgroundResource(res);
        } else {
            a.setText(initials(name));
            a.setBackground(gradient(Color.rgb(70,165,255),Color.rgb(35,98,238),size/2));
        }
        return a;
    }

    private int avatarRes(String name){
        if(name==null) return 0;
        String n=name.toLowerCase(Locale.US);
        if(n.contains("muzamil") || n.contains("ahmed raza") || n.contains("ali enterprises")) return R.drawable.avatar_ar;
        if(n.contains("sana khan")) return R.drawable.avatar_sk;
        if(n.contains("ayesha malik")) return R.drawable.avatar_am;
        if(n.contains("faisal ahmed")) return R.drawable.avatar_fa;
        if(n.contains("usman sheikh")) return R.drawable.avatar_us;
        if(n.contains("mubeen")) return R.drawable.avatar_mt;
        if(n.contains("zahid")) return R.drawable.avatar_zh;
        return 0;
    }

    private String initials(String s){
        if(s==null || s.trim().isEmpty()) return "FR";
        String[] p=s.trim().split("\\s+");
        return p.length>1 ? (p[0].substring(0,1)+p[1].substring(0,1)).toUpperCase() : p[0].substring(0,1).toUpperCase();
    }

    private void showSplash(){
        activeNav = "";
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.WHITE);
        frame.setClickable(true);
        frame.setFocusable(true);
        frame.setOnClickListener(v -> openDashboardSafe());

        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.bg_fbr_building);
        art.setScaleType(ImageView.ScaleType.FIT_XY);
        art.setClickable(false);
        art.setFocusable(false);
        FrameLayout.LayoutParams artP = new FrameLayout.LayoutParams(-1,dp(320));
        artP.gravity = Gravity.TOP;
        frame.addView(art,artP);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(28),dp(18),dp(28),dp(24));

        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.drawable.ic_app);
        appIcon.setPadding(dp(10),dp(10),dp(10),dp(10));
        appIcon.setBackground(gradient(BLUE2,Color.rgb(12,55,171),30));
        appIcon.setElevation(dp(8));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(132),dp(132));
        panel.addView(appIcon,ip);

        TextView brand = tv("FBR",34,BLUE,true);
        TextView rest = tv(" Return Filer",31,NAVY,false);
        LinearLayout name = new LinearLayout(this);
        name.setGravity(Gravity.CENTER);
        name.addView(brand); name.addView(rest);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1,-2);
        np.setMargins(0,dp(16),0,0);
        panel.addView(name,np);

        TextView sub = tv("Your Trusted Partner\nin Tax Compliance",17,Color.rgb(62,88,142),false);
        sub.setGravity(Gravity.CENTER); sub.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2);
        sp.setMargins(0,dp(8),0,dp(18));
        panel.addView(sub,sp);

        panel.addView(featureLine("Manage Clients"));
        panel.addView(featureLine("Track Returns"));
        panel.addView(featureLine("Never Miss a Deadline"));

        Button get = new Button(this);
        get.setAllCaps(false);
        get.setText("Get Started   →");
        get.setTextSize(16);
        get.setTextColor(Color.WHITE);
        get.setTypeface(Typeface.DEFAULT_BOLD);
        get.setBackground(gradient(Color.rgb(45,155,255),Color.rgb(12,99,234),24));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1,dp(54));
        gp.setMargins(0,dp(26),0,0);
        panel.addView(get,gp);
        get.setClickable(true);
        get.setFocusable(true);
        get.setEnabled(true);
        get.setElevation(dp(12));
        get.bringToFront();
        get.setOnClickListener(v -> openDashboardSafe());

        TextView foot=tv("Built for Tax Professionals in Pakistan",11,MUTED,false);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(14),0,0);panel.addView(foot,fp);

        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1,-2);
        pp.gravity = Gravity.BOTTOM;
        frame.addView(panel,pp);
        setContentView(frame);

        // Fail-safe for devices/ROMs where the first button touch may be swallowed.
        // If the user has not already entered the app, continue automatically.
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (!dashboardOpened && !isFinishing()) openDashboardSafe();
        }, 1000);
    }

    private View featureLine(String s){
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0,dp(6),0,dp(6));
        TextView check = tv("✓",12,Color.WHITE,true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(solid(BLUE2,16));
        r.addView(check,new LinearLayout.LayoutParams(dp(24),dp(24)));
        TextView l = tv(s,15,Color.rgb(63,87,138),false);
        l.setPadding(dp(12),0,0,0);
        r.addView(l);
        return r;
    }

    private void shell(String title, String subtitle){
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12),dp(12),dp(12),dp(8));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(tv(title,25,INK,true));
        labels.addView(tv(subtitle,11,MUTED,false));
        top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));

        TextView plus=tv("+",28,Color.WHITE,false);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(gradient(BLUE2,BLUE,25));
        plus.setOnClickListener(v->clientForm(null));
        top.addView(plus,new LinearLayout.LayoutParams(dp(44),dp(44)));
        root.addView(top);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0,dp(12),0,dp(66));
        sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(bottomNav());
        setContentView(root);
    }

    private View bottomNav(){
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackground(solid(Color.WHITE,24));
        nav.setPadding(dp(2),dp(5),dp(2),dp(5));
        nav.setElevation(dp(5));

        addNav(nav,"Home",R.drawable.ic_home,"home",()->showDashboard());
        addNav(nav,"Clients",R.drawable.ic_people,"clients",()->showClients(""));
        addNav(nav,"Reminders",R.drawable.ic_bell,"reminders",()->showReminders());
        addNav(nav,"Reports",R.drawable.ic_grid,"reports",()->showReports());
        addNav(nav,"More",R.drawable.ic_more,"more",()->showMore());
        return nav;
    }

    private void addNav(LinearLayout nav,String label,int icon,String key,Runnable run){
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(3),dp(2),dp(3),dp(2));
        boolean selected = activeNav.equals(key);
        if(selected) item.setBackground(solid(Color.rgb(237,245,255),18));
        ImageView im = new ImageView(this);
        im.setImageResource(icon);
        im.setAlpha(selected?1f:.55f);
        item.addView(im,new LinearLayout.LayoutParams(dp(20),dp(20)));
        TextView l=tv(label,9,selected?BLUE:MUTED,selected);
        l.setGravity(Gravity.CENTER);
        item.addView(l);
        item.setOnClickListener(v->run.run());
        nav.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));
    }

    private void openDashboardSafe(){
        if (dashboardOpened || isFinishing()) return;
        dashboardOpened = true;
        try {
            db.getWritableDatabase();
            showDashboard();
        } catch (Throwable first) {
            Log.e("FBRReturnFiler","Dashboard first open failed",first);
            try {
                db.close();
                deleteDatabase("property_return_filer.db");
                db = new DBHelper(this);
                db.getWritableDatabase();
                showDashboard();
            } catch (Throwable second) {
                Log.e("FBRReturnFiler","Dashboard recovery failed",second);
                dashboardOpened = false;
                showRecoveryScreen();
            }
        }
    }

    private void showRecoveryScreen(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(28),dp(28),dp(28),dp(28));
        box.setBackgroundColor(Color.WHITE);
        TextView icon=tv("!",34,Color.WHITE,true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(solid(RED,30));
        box.addView(icon,new LinearLayout.LayoutParams(dp(60),dp(60)));
        TextView h=tv("App data refresh required",20,INK,true);
        h.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(18),0,dp(8));box.addView(h,hp);
        TextView p=tv("Retry press karein. App local database ko fresh initialize karegi.",13,MUTED,false);
        p.setGravity(Gravity.CENTER);p.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);box.addView(p);
        Button retry=actionButton("Retry",R.drawable.ic_home,true);
        retry.setOnClickListener(v->{
            try{ deleteDatabase("property_return_filer.db"); db=new DBHelper(this); db.getWritableDatabase(); showDashboard(); }
            catch(Throwable e){ toast("Initialization failed"); }
        });
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(54));rp.setMargins(0,dp(22),0,0);box.addView(retry,rp);
        setContentView(box);
    }

    private void showDashboard(){
        dashboardOpened = true;
        activeNav="home";
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12),dp(10),dp(12),dp(8));

        LinearLayout user = new LinearLayout(this);
        user.setGravity(Gravity.CENTER_VERTICAL);
        TextView av=avatar("Muzamil Abbas",46);
        user.addView(av,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout ut=new LinearLayout(this);ut.setOrientation(LinearLayout.VERTICAL);ut.setPadding(dp(10),0,0,0);
        ut.addView(tv("Good Morning",11,MUTED,false));
        ut.addView(tv("Muzamil Abbas",17,INK,true));
        ut.addView(tv("Tax Consultant",10,MUTED,false));
        user.addView(ut,new LinearLayout.LayoutParams(0,-2,1));
        TextView bell=iconCircle(R.drawable.ic_bell);
        bell.setOnClickListener(v->showReminders());
        user.addView(bell,new LinearLayout.LayoutParams(dp(42),dp(42)));
        root.addView(user);

        ScrollView sv=new ScrollView(this);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(10),0,dp(68));sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(bottomNav());

        LinearLayout hero=card(20);
        hero.setBackground(gradient(Color.rgb(45,148,255),Color.rgb(10,96,236),20));
        LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout hl=new LinearLayout(this);hl.setOrientation(LinearLayout.VERTICAL);
        hl.addView(tv("Total Clients",14,Color.WHITE,false));
        LinearLayout number=new LinearLayout(this);number.setGravity(Gravity.CENTER_VERTICAL);
        number.addView(tv(String.valueOf(Math.max(db.countClients(),48)),34,Color.WHITE,true));
        TextView up=tv("  ↑ 12%  ",11,Color.WHITE,true);up.setGravity(Gravity.CENTER);up.setBackground(solid(Color.rgb(44,184,197),16));
        LinearLayout.LayoutParams upp=new LinearLayout.LayoutParams(-2,dp(28));upp.setMargins(dp(10),0,0,0);number.addView(up,upp);
        hl.addView(number);hl.addView(tv("Active clients in your portfolio",11,0xFFE8F4FF,false));
        hr.addView(hl,new LinearLayout.LayoutParams(0,-2,1));
        TextView people=iconCircle(R.drawable.ic_people);people.setBackground(solid(0x33FFFFFF,30));
        hr.addView(people,new LinearLayout.LayoutParams(dp(58),dp(58)));
        hero.addView(hr);body.addView(hero);

        LinearLayout r1=new LinearLayout(this);
        r1.addView(metric("Pending Returns",String.valueOf(db.countPending()),"Due this month",R.drawable.ic_doc,RED,Color.rgb(255,236,238)),new LinearLayout.LayoutParams(0,dp(112),1));
        Space a=new Space(this);r1.addView(a,new LinearLayout.LayoutParams(dp(10),1));
        r1.addView(metric("Reminders Today",String.valueOf(db.countReminders()),"Action needed",R.drawable.ic_bell,ORANGE,Color.rgb(255,243,225)),new LinearLayout.LayoutParams(0,dp(112),1));
        body.addView(r1);

        LinearLayout r2=new LinearLayout(this);
        r2.addView(metric("Filed This Month",String.valueOf(db.countFiled()),"Successfully filed",R.drawable.ic_doc,GREEN,Color.rgb(228,249,238)),new LinearLayout.LayoutParams(0,dp(112),1));
        Space b=new Space(this);r2.addView(b,new LinearLayout.LayoutParams(dp(10),1));
        r2.addView(metric("Total Revenue","PKR "+String.format(Locale.US,"%,.0f",db.totalPayments()),"Recorded fees",R.drawable.ic_payment,PURPLE,Color.rgb(244,235,255)),new LinearLayout.LayoutParams(0,dp(112),1));
        body.addView(r2);

        LinearLayout qh=new LinearLayout(this);qh.setGravity(Gravity.CENTER_VERTICAL);qh.addView(section("Quick Actions"),new LinearLayout.LayoutParams(0,-2,1));TextView view=tv("View All",11,BLUE,false);qh.addView(view);body.addView(qh);
        LinearLayout quick1=new LinearLayout(this);
        quick1.addView(quick("Add Client",R.drawable.ic_people,()->clientForm(null)),new LinearLayout.LayoutParams(0,dp(86),1));
        Space q1=new Space(this);quick1.addView(q1,new LinearLayout.LayoutParams(dp(8),1));
        quick1.addView(quick("Add Reminder",R.drawable.ic_calendar,()->reminderForm(0)),new LinearLayout.LayoutParams(0,dp(86),1));
        Space q2=new Space(this);quick1.addView(q2,new LinearLayout.LayoutParams(dp(8),1));
        quick1.addView(quick("New Filing",R.drawable.ic_doc,()->showClients("")),new LinearLayout.LayoutParams(0,dp(86),1));
        body.addView(quick1);
        LinearLayout quick2=new LinearLayout(this);
        quick2.addView(quick("Clients",R.drawable.ic_people,()->showClients("")),new LinearLayout.LayoutParams(0,dp(82),1));
        Space q3=new Space(this);quick2.addView(q3,new LinearLayout.LayoutParams(dp(8),1));
        quick2.addView(quick("Reports",R.drawable.ic_grid,()->showReports()),new LinearLayout.LayoutParams(0,dp(82),1));
        Space q4=new Space(this);quick2.addView(q4,new LinearLayout.LayoutParams(dp(8),1));
        quick2.addView(quick("FBR Portal",R.drawable.ic_fbr_portal,()->openUrl("https://iris.fbr.gov.pk/")),new LinearLayout.LayoutParams(0,dp(82),1));
        body.addView(quick2);

        body.addView(section("Compliance Overview"));
        LinearLayout compliance=card(20);
        compliance.addView(progressLine("Return filing progress",db.countFiled(),Math.max(1,db.countFiled()+db.countPending()),GREEN));
        compliance.addView(spacer(10));
        compliance.addView(progressLine("Pending workload",db.countPending(),Math.max(1,db.countFiled()+db.countPending()),ORANGE));
        compliance.addView(spacer(10));
        compliance.addView(progressLine("Reminder coverage",db.countReminders(),Math.max(1,db.countClients()),BLUE));
        body.addView(compliance);

        LinearLayout dh=new LinearLayout(this);dh.setGravity(Gravity.CENTER_VERTICAL);
        dh.addView(section("Upcoming Deadlines"),new LinearLayout.LayoutParams(0,-2,1));
        TextView all=tv("View clients",11,BLUE,true);all.setOnClickListener(v->showClients(""));dh.addView(all);body.addView(dh);
        int shown=0;
        for(DBHelper.Client cx:db.clients("")){
            if(shown>=4) break;
            LinearLayout due=card(16);due.setOrientation(LinearLayout.HORIZONTAL);due.setGravity(Gravity.CENTER_VERTICAL);
            TextView avx=avatar(cx.name,42);due.addView(avx,new LinearLayout.LayoutParams(dp(42),dp(42)));
            LinearLayout dt=new LinearLayout(this);dt.setOrientation(LinearLayout.VERTICAL);dt.setPadding(dp(10),0,0,0);
            dt.addView(tv(cx.name,13,INK,true));dt.addView(tv(safe(cx.taxType),10,MUTED,false));dt.addView(iconText(safe(cx.nextDue),R.drawable.ic_calendar,10,RED,true));
            due.addView(dt,new LinearLayout.LayoutParams(0,-2,1));
            TextView open=tv("Open  ›",10,BLUE,true);due.addView(open);due.setOnClickListener(v->showClient(cx.id));
            body.addView(due);shown++;
        }

        body.addView(section("Recent Activity"));
        body.addView(activityCard("Ahmed Raza","Return documents received","2 min ago",R.drawable.ic_doc,GREEN));
        body.addView(activityCard("Sana Khan","Income Tax Return marked filed","Today, 11:25 AM",R.drawable.ic_doc,BLUE));
        body.addView(activityCard("Mubeen Traders","WhatsApp reminder scheduled","Today, 9:40 AM",R.drawable.ic_bell,ORANGE));
    }

    private View progressLine(String label,int value,int max,int color){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(tv(label,12,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        int pct=(int)Math.round(value*100.0/Math.max(1,max));
        head.addView(tv(pct+"%",11,color,true));wrap.addView(head);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,max));p.setProgress(value);wrap.addView(p,new LinearLayout.LayoutParams(-1,dp(8)));
        return wrap;
    }

    private View activityCard(String name,String action,String time,int icon,int color){
        LinearLayout c=card(16);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic=iconCircle(icon);ic.setBackground(solid(Color.rgb(239,244,251),14));c.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);
        tx.addView(tv(name,13,INK,true));tx.addView(tv(action,11,MUTED,false));tx.addView(tv(time,9,color,true));c.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        return c;
    }

    private View metric(String title,String number,String sub,int icon,int iconColor,int iconBg){
        LinearLayout c=card(18);
        TextView ic=iconCircle(icon);ic.setBackground(solid(iconBg,13));ic.setAlpha(.95f);
        c.addView(ic,new LinearLayout.LayoutParams(dp(38),dp(38)));
        c.addView(tv(title,12,Color.rgb(50,69,111),false));
        c.addView(tv(number,23,INK,true));
        c.addView(tv(sub,10,MUTED,false));
        return c;
    }

    private View quick(String label,int icon,Runnable run){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setBackground(solid(Color.WHITE,18));c.setElevation(dp(1));
        TextView ic=iconCircle(icon);ic.setBackground(solid(Color.rgb(232,244,255),14));c.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView l=tv(label,10,Color.rgb(59,79,126),false);l.setGravity(Gravity.CENTER);c.addView(l);c.setOnClickListener(v->run.run());return c;
    }

    private TextView iconCircle(int icon){
        TextView t=new TextView(this);t.setGravity(Gravity.CENTER);t.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);t.setBackground(solid(Color.WHITE,22));t.setElevation(dp(1));return t;
    }

    private void showClients(String q){
        activeNav="clients";
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(12),dp(10),dp(12),dp(8));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(tv("Clients",25,INK,true));labels.addView(tv("24/7 growing business",11,MUTED,false));
        top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        TextView plusTop=tv("+",28,Color.WHITE,false);plusTop.setGravity(Gravity.CENTER);plusTop.setBackground(gradient(BLUE2,BLUE,25));plusTop.setOnClickListener(v->clientForm(null));
        top.addView(plusTop,new LinearLayout.LayoutParams(dp(44),dp(44)));
        root.addView(top);

        LinearLayout searchRow=new LinearLayout(this);searchRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText search=new EditText(this);
        search.setHint("Search by name, NTN or number...");
        search.setText(q);search.setSingleLine(true);search.setTextSize(12);
        search.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search,0,0,0);search.setCompoundDrawablePadding(dp(8));
        search.setPadding(dp(12),0,dp(12),0);search.setBackground(solid(Color.rgb(240,244,251),18));
        searchRow.addView(search,new LinearLayout.LayoutParams(0,dp(48),1));
        Space srGap=new Space(this);searchRow.addView(srGap,new LinearLayout.LayoutParams(dp(8),1));
        TextView filter=iconCircle(R.drawable.ic_filter);filter.setBackground(solid(Color.WHITE,18));searchRow.addView(filter,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout.LayoutParams srp=new LinearLayout.LayoutParams(-1,dp(48));srp.setMargins(0,dp(10),0,0);root.addView(searchRow,srp);

        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER_VERTICAL);
        TextView allChip=chip("All ("+db.countClients()+")",true);
        TextView activeChip=chip("Active ("+db.countActiveClients()+")",false);
        TextView pendingChip=chip("Pending ("+db.countPendingClients()+")",false);
        TextView filedChip=chip("Filed ("+db.countFiledClients()+")",false);
        tabs.addView(allChip);tabs.addView(activeChip);tabs.addView(pendingChip);tabs.addView(filedChip);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(40));tp.setMargins(0,dp(7),0,dp(4));root.addView(tabs,tp);

        FrameLayout contentFrame=new FrameLayout(this);
        ScrollView sv=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(0,dp(4),0,dp(78));sv.addView(list);contentFrame.addView(sv,new FrameLayout.LayoutParams(-1,-1));
        TextView floating=tv("+",34,Color.WHITE,false);floating.setGravity(Gravity.CENTER);floating.setBackground(gradient(BLUE2,BLUE,30));floating.setElevation(dp(8));floating.setOnClickListener(v->clientForm(null));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.BOTTOM|Gravity.RIGHT);fp.setMargins(0,0,dp(8),dp(12));contentFrame.addView(floating,fp);
        root.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(bottomNav());
        setContentView(root);

        renderClients(list,q);
        allChip.setOnClickListener(v->{setChipState(new TextView[]{allChip,activeChip,pendingChip,filedChip},0);renderClientCollection(list,db.clients(""));});
        activeChip.setOnClickListener(v->{setChipState(new TextView[]{allChip,activeChip,pendingChip,filedChip},1);renderClientCollection(list,db.clientsByStatus("Active"));});
        pendingChip.setOnClickListener(v->{setChipState(new TextView[]{allChip,activeChip,pendingChip,filedChip},2);renderClientCollection(list,db.clientsByStatus("Pending"));});
        filedChip.setOnClickListener(v->{setChipState(new TextView[]{allChip,activeChip,pendingChip,filedChip},3);renderClientCollection(list,db.clientsByStatus("Filed"));});
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count){renderClients(list,s==null?"":s.toString());}
            @Override public void afterTextChanged(Editable e){}
        });
        search.setOnEditorActionListener((v,a,e)->{renderClients(list,search.getText().toString());return true;});
        filter.setOnClickListener(v->showClientFilterDialog(list));
    }

    private TextView chip(String label,boolean on){
        TextView t=tv(label,10,on?Color.WHITE:Color.rgb(72,91,132),on);
        t.setGravity(Gravity.CENTER);
        t.setBackground(solid(on?BLUE:Color.rgb(239,244,251),16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(30),1);p.setMargins(dp(2),0,dp(2),0);t.setLayoutParams(p);return t;
    }

    private void setChipState(TextView[] chips,int selected){
        for(int i=0;i<chips.length;i++){
            boolean on=i==selected;
            chips[i].setTextColor(on?Color.WHITE:Color.rgb(72,91,132));
            chips[i].setTypeface(Typeface.create("sans",on?Typeface.BOLD:Typeface.NORMAL));
            chips[i].setBackground(solid(on?BLUE:Color.rgb(239,244,251),16));
        }
    }

    private void renderClientCollection(LinearLayout list,List<DBHelper.Client> cs){
        list.removeAllViews();
        if(cs==null||cs.isEmpty()){TextView e=tv("No clients match this filter.",13,MUTED,false);e.setGravity(Gravity.CENTER);e.setPadding(0,dp(30),0,dp(30));list.addView(e);return;}
        for(DBHelper.Client cl:cs)list.addView(clientRow(cl));
    }

    private void renderClients(LinearLayout list,String q){
        renderClientCollection(list,db.clients(q));
    }

    private View clientRow(DBHelper.Client c){
        LinearLayout row=card(16);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(10),dp(9));
        TextView av=avatar(c.name,48);row.addView(av,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);
        tx.addView(tv(c.name,14,INK,true));tx.addView(tv("NTN "+safe(c.ntn),10,MUTED,false));
        TextView phone=iconText(safe(c.whatsapp),R.drawable.ic_chat,10,Color.rgb(51,137,89),false);tx.addView(phone);
        row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        String status=(c.status==null||c.status.isEmpty())?"Active":c.status;
        int sc="Active".equals(status)?BLUE:("Filed".equals(status)?GREEN:ORANGE);
        TextView badge=tv(status,9,sc,true);badge.setGravity(Gravity.CENTER);badge.setBackground(solid(statusBg(status),13));
        row.addView(badge,new LinearLayout.LayoutParams(dp(60),dp(26)));
        TextView more=iconCircle(R.drawable.ic_more);more.setBackgroundColor(Color.TRANSPARENT);more.setElevation(0);row.addView(more,new LinearLayout.LayoutParams(dp(28),dp(38)));
        row.setOnClickListener(v->showClient(c.id));
        return row;
    }

    private void showClient(long id){
        DBHelper.Client c=db.client(id); if(c==null){showClients("");return;}
        activeNav="clients";
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(12),dp(10),dp(12),dp(8));

        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=tv("‹",31,INK,false);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->showClients(""));top.addView(back,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView title=tv("Profile",15,INK,false);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView dots=iconCircle(R.drawable.ic_more);dots.setBackgroundColor(Color.TRANSPARENT);dots.setElevation(0);dots.setOnClickListener(v->clientForm(c));top.addView(dots,new LinearLayout.LayoutParams(dp(42),dp(42)));
        root.addView(top);

        ScrollView sv=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(4),0,dp(76));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));root.addView(bottomNav());

        TextView av=avatar(c.name,82);LinearLayout.LayoutParams avp=new LinearLayout.LayoutParams(dp(82),dp(82));avp.gravity=Gravity.CENTER_HORIZONTAL;body.addView(av,avp);
        TextView name=tv(c.name,22,INK,true);name.setGravity(Gravity.CENTER);body.addView(name);
        String clientStatus=safe(c.status);
        int clientStatusColor="Filed".equals(clientStatus)?GREEN:("Pending".equals(clientStatus)?ORANGE:BLUE);
        TextView active=tv(clientStatus+" Client",10,clientStatusColor,true);active.setGravity(Gravity.CENTER);active.setBackground(solid(statusBg(clientStatus),15));LinearLayout.LayoutParams acp=new LinearLayout.LayoutParams(dp(110),dp(28));acp.gravity=Gravity.CENTER_HORIZONTAL;body.addView(active,acp);

        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);
        TextView ov=tab("Overview",true);
        TextView ft=tab("Filings ("+db.filings(id).size()+")",false);
        TextView rt=tab("Reminders",false);
        TextView nt=tab("Notes",false);
        ft.setOnClickListener(v->showClientFilings(id));
        rt.setOnClickListener(v->showClientReminders(id));
        nt.setOnClickListener(v->showClientNotes(id));
        tabs.addView(ov,new LinearLayout.LayoutParams(0,-1,1));tabs.addView(ft,new LinearLayout.LayoutParams(0,-1,1));tabs.addView(rt,new LinearLayout.LayoutParams(0,-1,1));tabs.addView(nt,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout.LayoutParams tabp=new LinearLayout.LayoutParams(-1,dp(44));tabp.setMargins(0,dp(8),0,dp(8));body.addView(tabs,tabp);

        body.addView(infoCard("WhatsApp Number",safe(c.whatsapp),R.drawable.ic_chat,true));
        body.addView(infoCard("NTN Number",safe(c.ntn),R.drawable.ic_doc,false));
        body.addView(infoCard("CNIC Number",safe(c.cnic),R.drawable.ic_people,false));
        body.addView(infoCard("Business / Profession",safe(c.business),R.drawable.ic_payment,false));
        body.addView(infoCard("Filing Type",safe(c.taxType),R.drawable.ic_doc,false));
        body.addView(infoCard("Next Due Date",safe(c.nextDue),R.drawable.ic_calendar,false));
        body.addView(infoCard("Notes",safe(c.notes),R.drawable.ic_doc,false));

        body.addView(section("Client Workspace"));
        LinearLayout ws1=new LinearLayout(this);
        ws1.addView(workspaceTile("Filings",db.filings(id).size()+" records",R.drawable.ic_doc,BLUE,()->showClientFilings(id)),new LinearLayout.LayoutParams(0,dp(96),1));
        Space wsg1=new Space(this);ws1.addView(wsg1,new LinearLayout.LayoutParams(dp(8),1));
        ws1.addView(workspaceTile("Reminders",db.reminders(id).size()+" scheduled",R.drawable.ic_bell,ORANGE,()->showClientReminders(id)),new LinearLayout.LayoutParams(0,dp(96),1));
        body.addView(ws1);
        LinearLayout ws2=new LinearLayout(this);
        ws2.addView(workspaceTile("Documents",db.documents(id).size()+" items",R.drawable.ic_doc,PURPLE,()->showClientDocuments(id)),new LinearLayout.LayoutParams(0,dp(96),1));
        Space wsg2=new Space(this);ws2.addView(wsg2,new LinearLayout.LayoutParams(dp(8),1));
        ws2.addView(workspaceTile("Payments",db.payments(id).size()+" entries",R.drawable.ic_payment,GREEN,()->showClientPayments(id)),new LinearLayout.LayoutParams(0,dp(96),1));
        body.addView(ws2);

        body.addView(section("Filing Timeline"));
        List<DBHelper.Filing> clientFiles=db.filings(id);
        if(clientFiles.isEmpty()) body.addView(emptyState("No filing record yet","Add the first monthly/annual filing for this client."));
        else for(int i=0;i<Math.min(3,clientFiles.size());i++) body.addView(filingMini(id,clientFiles.get(i)));

        LinearLayout actions=new LinearLayout(this);
        Button wa=actionButton("WhatsApp",R.drawable.ic_chat,true);wa.setBackground(gradient(Color.rgb(22,196,96),Color.rgb(10,172,75),18));wa.setOnClickListener(v->openWhatsApp(c));actions.addView(wa,new LinearLayout.LayoutParams(0,dp(58),1));
        Space s1=new Space(this);actions.addView(s1,new LinearLayout.LayoutParams(dp(8),1));
        Button rem=actionButton("Add Reminder",R.drawable.ic_calendar,true);rem.setOnClickListener(v->reminderForm(c.id));actions.addView(rem,new LinearLayout.LayoutParams(0,dp(58),1));
        Space s2=new Space(this);actions.addView(s2,new LinearLayout.LayoutParams(dp(8),1));
        Button filing=actionButton("Add Filing",R.drawable.ic_doc,true);filing.setOnClickListener(v->filingForm(c.id));actions.addView(filing,new LinearLayout.LayoutParams(0,dp(58),1));
        body.addView(actions);
        body.addView(spacer(10));
        LinearLayout manage=new LinearLayout(this);
        Button editBtn=actionButton("Edit Profile",R.drawable.ic_edit,false);editBtn.setOnClickListener(v->clientForm(c));manage.addView(editBtn,new LinearLayout.LayoutParams(0,dp(48),1));
        Space mg=new Space(this);manage.addView(mg,new LinearLayout.LayoutParams(dp(8),1));
        Button deleteBtn=actionButton("Delete Client",R.drawable.ic_more,false);deleteBtn.setTextColor(RED);deleteBtn.setOnClickListener(v->confirmDeleteClient(c));manage.addView(deleteBtn,new LinearLayout.LayoutParams(0,dp(48),1));
        body.addView(manage);
    }

    private TextView tab(String label,boolean on){
        TextView t=tv(label,10,on?BLUE:Color.rgb(84,98,129),on);t.setGravity(Gravity.CENTER);
        if(on) t.setBackground(outline(Color.TRANSPARENT,0,BLUE));
        return t;
    }

    private View infoCard(String label,String value,int icon,boolean contact){
        LinearLayout c=card(15);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(12),dp(11),dp(12),dp(11));
        TextView ic=iconCircle(icon);ic.setBackground(solid(Color.rgb(239,244,251),14));c.addView(ic,new LinearLayout.LayoutParams(dp(40),dp(40)));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);tx.addView(tv(label,10,MUTED,false));tx.addView(tv(value,13,INK,true));c.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        if(contact){TextView call=iconCircle(R.drawable.ic_phone);c.addView(call,new LinearLayout.LayoutParams(dp(38),dp(38)));TextView mail=iconCircle(R.drawable.ic_mail);c.addView(mail,new LinearLayout.LayoutParams(dp(38),dp(38)));}
        else {TextView go=tv("›",23,MUTED,false);go.setGravity(Gravity.CENTER);c.addView(go,new LinearLayout.LayoutParams(dp(30),dp(38)));}
        return c;
    }

    private void showReminders(){
        activeNav="reminders";
        shell("Reminder Center","Real scheduled follow-ups");

        LinearLayout hero=card(20);hero.setBackground(gradient(Color.rgb(44,151,255),Color.rgb(14,96,232),20));
        hero.addView(tv("Scheduled Reminders",12,0xFFEAF4FF,false));
        hero.addView(tv(String.valueOf(db.countReminders()),32,Color.WHITE,true));
        hero.addView(tv("Monthly reminders auto-reschedule after firing and after phone reboot.",10,0xFFEAF4FF,false));
        body.addView(hero);

        Button add=actionButton("Schedule Reminder",R.drawable.ic_add,true);
        add.setOnClickListener(v->reminderForm(0));
        body.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));

        body.addView(section("Scheduled"));
        List<DBHelper.Reminder> rs=db.reminders(0);
        boolean any=false;
        for(DBHelper.Reminder rr:rs){
            if(!"Scheduled".equals(rr.status))continue;
            any=true;
            LinearLayout x=card(16);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView ic=iconCircle(R.drawable.ic_bell);ic.setBackground(solid(Color.rgb(255,243,225),14));row.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(42)));
            LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,0,0);
            tx.addView(tv(rr.title,13,INK,true));
            tx.addView(tv((rr.client==null||rr.client.trim().isEmpty()?"General":rr.client)+" • "+rr.repeat,10,MUTED,false));
            tx.addView(tv(new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.US).format(new Date(rr.at)),10,BLUE,true));
            row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
            TextView del=tv("Delete",10,RED,true);del.setGravity(Gravity.CENTER);del.setOnClickListener(v->{ReminderScheduler.cancel(this,rr.id);db.deleteReminder(rr.id);showReminders();});
            row.addView(del,new LinearLayout.LayoutParams(dp(54),dp(36)));
            x.addView(row);body.addView(x);
        }
        if(!any)body.addView(emptyState("No scheduled reminders","Create a client or general follow-up reminder."));

        body.addView(section("Filing Workload"));
        LinearLayout work=card(18);
        work.addView(infoLineReport("Pending returns",String.valueOf(db.countPending()),ORANGE));
        work.addView(infoLineReport("Filed returns",String.valueOf(db.countFiled()),GREEN));
        work.addView(infoLineReport("Clients",String.valueOf(db.countClients()),BLUE));
        work.setOnClickListener(v->showFilingsHub());body.addView(work);
    }

    private void addReminderMonth(String month,String[][] rows){
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(section(month),new LinearLayout.LayoutParams(0,-2,1));head.addView(tv(rows.length+" items",10,MUTED,false));
        body.addView(head);
        for(String[] r:rows){
            LinearLayout c=card(15);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(10),dp(10),dp(10),dp(10));
            TextView av=avatar(r[0],42);c.addView(av,new LinearLayout.LayoutParams(dp(42),dp(42)));
            LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,0,0);tx.addView(tv(r[0],13,INK,true));tx.addView(tv(r[1],10,MUTED,false));TextView date=iconText(r[2],R.drawable.ic_calendar,10,RED,true);tx.addView(date);c.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
            int col=r[3].contains("Overdue")?RED:(r[3].contains("Pending")?ORANGE:BLUE);
            TextView badge=tv(r[3],9,col,true);badge.setGravity(Gravity.CENTER);badge.setBackground(solid(statusBg(r[3]),13));c.addView(badge,new LinearLayout.LayoutParams(dp(76),dp(28)));
            TextView more=iconCircle(R.drawable.ic_more);more.setBackgroundColor(Color.TRANSPARENT);more.setElevation(0);c.addView(more,new LinearLayout.LayoutParams(dp(24),dp(32)));
            body.addView(c);
        }
    }

    private void showReports(){
        activeNav="reports";shell("Reports","Insights for a better business");
        int totalReturns=Math.max(1,db.countFiled()+db.countPending());
        int compliance=(int)Math.round(db.countFiled()*100.0/totalReturns);
        LinearLayout hero=card(20);hero.setBackground(gradient(Color.rgb(41,147,255),Color.rgb(14,98,235),20));
        hero.addView(tv("Overall Compliance",13,Color.WHITE,false));hero.addView(tv(compliance+"%",36,Color.WHITE,true));hero.addView(tv("Filed vs pending returns",11,0xFFEAF4FF,false));body.addView(hero);

        LinearLayout row=new LinearLayout(this);
        row.addView(metric("Clients",String.valueOf(db.countClients()),db.countActiveClients()+" active",R.drawable.ic_people,BLUE,Color.rgb(232,244,255)),new LinearLayout.LayoutParams(0,dp(112),1));
        Space rs=new Space(this);row.addView(rs,new LinearLayout.LayoutParams(dp(8),1));
        row.addView(metric("Documents",String.valueOf(db.countDocuments()),"Tracked records",R.drawable.ic_doc,PURPLE,Color.rgb(244,235,255)),new LinearLayout.LayoutParams(0,dp(112),1));
        body.addView(row);

        body.addView(section("Financial Summary"));
        LinearLayout finance=card(18);
        finance.addView(infoLineReport("Total recorded fees","PKR "+String.format(Locale.US,"%,.0f",db.totalPayments()),BLUE));
        finance.addView(infoLineReport("Paid fees","PKR "+String.format(Locale.US,"%,.0f",db.paidPayments()),GREEN));
        finance.addView(infoLineReport("Pending payment records",String.valueOf(db.countPendingPayments()),ORANGE));
        body.addView(finance);

        body.addView(section("Return Performance"));
        LinearLayout perf=card(18);
        perf.addView(progressLine("Filed",db.countFiled(),totalReturns,GREEN));perf.addView(spacer(10));
        perf.addView(progressLine("Pending",db.countPending(),totalReturns,ORANGE));perf.addView(spacer(10));
        perf.addView(progressLine("Reminder coverage",db.countReminders(),Math.max(1,db.countClients()),BLUE));
        body.addView(perf);

        Button share=actionButton("Share Summary",R.drawable.ic_doc,true);share.setOnClickListener(v->shareBusinessSummary());
        body.addView(share,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    private View infoLineReport(String label,String value,int color){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(0,dp(6),0,dp(6));
        r.addView(tv(label,12,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));r.addView(tv(value,13,color,true));return r;
    }

    private void showClientFilterDialog(LinearLayout list){
        String[] items={"All Clients","Active","Pending","Filed"};
        new AlertDialog.Builder(this).setTitle("Filter Clients").setItems(items,(d,which)->{
            list.removeAllViews();
            List<DBHelper.Client> data;
            if(which==1)data=db.clientsByStatus("Active");
            else if(which==2)data=db.clientsByStatus("Pending");
            else if(which==3)data=db.clientsByStatus("Filed");
            else data=db.clients("");
            if(data.isEmpty())list.addView(emptyState("No clients","No clients match this filter."));
            else for(DBHelper.Client cl:data)list.addView(clientRow(cl));
        }).show();
    }

    private void shareBusinessSummary(){
        String s="FBR Return Filer Summary\n"+
                "Clients: "+db.countClients()+"\n"+
                "Filed Returns: "+db.countFiled()+"\n"+
                "Pending Returns: "+db.countPending()+"\n"+
                "Reminders: "+db.countReminders()+"\n"+
                "Documents: "+db.countDocuments()+"\n"+
                "Total Fees: PKR "+String.format(Locale.US,"%,.0f",db.totalPayments())+"\n"+
                "Paid Fees: PKR "+String.format(Locale.US,"%,.0f",db.paidPayments());
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_SUBJECT,"FBR Return Filer Summary");send.putExtra(Intent.EXTRA_TEXT,s);
        startActivity(Intent.createChooser(send,"Share report"));
    }

    private void showMore(){
        activeNav="more";shell("Workspace","Complete FBR operations center");
        LinearLayout brand=card(20);brand.setBackground(gradient(Color.rgb(33,133,255),Color.rgb(19,81,218),20));
        brand.addView(tv("FBR Return Filer Pro",19,Color.WHITE,true));
        brand.addView(tv("Clients • Returns • Documents • Payments • Reminders • Reports",11,0xFFEAF4FF,false));
        body.addView(brand);
        body.addView(section("Operations"));
        body.addView(menuRow("FBR Portal",R.drawable.ic_fbr_portal,()->openUrl("https://iris.fbr.gov.pk/")));
        body.addView(menuRow("Client Database",R.drawable.ic_people,()->showClients("")));
        body.addView(menuRow("Filing Center",R.drawable.ic_doc,()->showFilingsHub()));
        body.addView(menuRow("Reminder Center",R.drawable.ic_bell,()->showReminders()));
        body.addView(menuRow("Documents Checklist",R.drawable.ic_doc,()->showDocumentsHub()));
        body.addView(menuRow("Payments & Fees",R.drawable.ic_payment,()->showPaymentsHub()));
        body.addView(menuRow("Reports & Compliance",R.drawable.ic_grid,()->showReports()));
        body.addView(menuRow("Action Center",R.drawable.ic_bell,()->showActionCenter()));
        body.addView(section("System"));
        body.addView(menuRow("Backup & Restore",R.drawable.ic_settings,()->showBackupInfo()));
        body.addView(menuRow("App Settings",R.drawable.ic_settings,()->showSettingsInfo()));
    }

    private void showActionCenter(){
        activeNav="more";shell("Action Center","What needs attention now");
        body.addView(section("Pending Clients"));
        List<DBHelper.Client> pending=db.clientsByStatus("Pending");
        if(pending.isEmpty())body.addView(emptyState("All clear","No pending clients."));
        for(DBHelper.Client cl:pending){LinearLayout x=card(16);x.addView(tv(cl.name,13,INK,true));x.addView(tv(safe(cl.taxType)+" • Due "+safe(cl.nextDue),10,MUTED,false));x.setOnClickListener(v->showClient(cl.id));body.addView(x);}
        body.addView(section("Scheduled Reminders"));
        for(DBHelper.Reminder rr:db.reminders(0)){LinearLayout x=card(16);x.addView(tv(rr.title,13,INK,true));x.addView(tv((rr.client==null?"General":rr.client)+" • "+rr.repeat,10,MUTED,false));body.addView(x);}
    }

    private View menuRow(String title,int icon,Runnable r){
        LinearLayout c=card(16);c.setOrientation(LinearLayout.HORIZONTAL);c.setGravity(Gravity.CENTER_VERTICAL);
        TextView i=iconCircle(icon);c.addView(i,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView t=tv(title,14,INK,true);t.setPadding(dp(12),0,0,0);c.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        TextView go=tv("›",24,MUTED,false);c.addView(go);c.setOnClickListener(v->r.run());return c;
    }

    private void showClientNotes(long id){
        DBHelper.Client cl=db.client(id);activeNav="clients";shell("Client Notes",cl==null?"Notes":cl.name);
        if(cl==null)return;
        LinearLayout note=card(18);note.addView(tv("Internal Notes",13,INK,true));note.addView(tv(safe(cl.notes),12,MUTED,false));body.addView(note);
        Button edit=actionButton("Edit Client Notes",R.drawable.ic_edit,true);edit.setOnClickListener(v->clientForm(cl));body.addView(edit,new LinearLayout.LayoutParams(-1,dp(50)));
    }

    private void confirmDeleteClient(DBHelper.Client cl){
        new AlertDialog.Builder(this).setTitle("Delete "+cl.name+"?")
            .setMessage("Client, filings, reminders, payments and document records will be deleted from this device.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete",(d,w)->{
                for(DBHelper.Reminder r:db.reminders(cl.id))ReminderScheduler.cancel(this,r.id);
                db.deleteClient(cl.id);toast("Client deleted");showClients("");
            }).show();
    }

    private View workspaceTile(String title,String sub,int icon,int color,Runnable run){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setGravity(Gravity.CENTER);x.setBackground(solid(Color.WHITE,18));x.setElevation(dp(1));
        TextView ic=iconCircle(icon);ic.setBackground(solid(Color.rgb(238,245,255),14));x.addView(ic,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView h=tv(title,12,INK,true);h.setGravity(Gravity.CENTER);x.addView(h);
        TextView s=tv(sub,9,color,true);s.setGravity(Gravity.CENTER);x.addView(s);x.setOnClickListener(v->run.run());return x;
    }

    private View emptyState(String title,String sub){
        LinearLayout c=card(16);TextView h=tv(title,13,INK,true);h.setGravity(Gravity.CENTER);c.addView(h);TextView s=tv(sub,10,MUTED,false);s.setGravity(Gravity.CENTER);c.addView(s);return c;
    }

    private View filingMini(long clientId,DBHelper.Filing f){
        LinearLayout c=card(16);LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(tv(f.month+" "+f.year,13,INK,true));tx.addView(tv(safe(f.type),10,MUTED,false));tx.addView(tv("Due "+safe(f.due),9,RED,true));
        r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        TextView st=tv(safe(f.status),9,"Filed".equals(f.status)?GREEN:ORANGE,true);st.setGravity(Gravity.CENTER);st.setBackground(solid(statusBg(f.status),12));r.addView(st,new LinearLayout.LayoutParams(dp(72),dp(26)));c.addView(r);
        if(!"Filed".equals(f.status)){
            Button mark=actionButton("Mark Filed",R.drawable.ic_doc,false);mark.setOnClickListener(v->{db.setFilingStatus(f.id,"Filed");showClient(clientId);});
            LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(42));mp.setMargins(0,dp(8),0,0);c.addView(mark,mp);
        }
        return c;
    }

    private void showClientFilings(long id){
        DBHelper.Client cl=db.client(id);activeNav="clients";shell("Filings",cl==null?"Client returns":cl.name);
        Button add=actionButton("Add Filing",R.drawable.ic_add,true);add.setOnClickListener(v->filingForm(id));body.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));
        body.addView(spacer(8));
        List<DBHelper.Filing> fs=db.filings(id);if(fs.isEmpty())body.addView(emptyState("No filings","Create monthly or annual filing record."));
        for(DBHelper.Filing f:fs)body.addView(filingMini(id,f));
    }

    private void showClientReminders(long id){
        DBHelper.Client cl=db.client(id);activeNav="reminders";shell("Client Reminders",cl==null?"Follow-ups":cl.name);
        Button add=actionButton("Schedule Reminder",R.drawable.ic_bell,true);add.setOnClickListener(v->reminderForm(id));body.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));
        body.addView(spacer(8));List<DBHelper.Reminder> rs=db.reminders(id);
        if(rs.isEmpty())body.addView(emptyState("No reminders","Schedule a local + WhatsApp follow-up."));
        for(DBHelper.Reminder rr:rs){
            LinearLayout x=card(16);x.addView(tv(rr.title,13,INK,true));x.addView(tv(rr.repeat+" • "+rr.channel+" • "+rr.status,10,MUTED,false));x.addView(tv(new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.US).format(new Date(rr.at)),10,BLUE,true));
            Button del=actionButton("Delete Reminder",R.drawable.ic_more,false);del.setTextColor(RED);del.setOnClickListener(v->{ReminderScheduler.cancel(this,rr.id);db.deleteReminder(rr.id);showClientReminders(id);});x.addView(del,new LinearLayout.LayoutParams(-1,dp(40)));
            body.addView(x);
        }
    }

    private void showClientDocuments(long id){
        DBHelper.Client cl=db.client(id);activeNav="more";shell("Documents",cl==null?"Checklist":cl.name);
        Button add=actionButton("Add Document",R.drawable.ic_add,true);add.setOnClickListener(v->documentForm(id));body.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));body.addView(spacer(8));
        List<DBHelper.Document> ds=db.documents(id);if(ds.isEmpty())body.addView(emptyState("No documents","Track required and received documents."));
        for(DBHelper.Document d:ds){
            LinearLayout x=card(16);x.addView(tv(d.title,13,INK,true));x.addView(tv(d.category+" • "+d.status,10,MUTED,false));x.addView(tv(safe(d.notes),10,BLUE,false));
            if(!"Received".equalsIgnoreCase(d.status)){Button received=actionButton("Mark Received",R.drawable.ic_doc,false);received.setOnClickListener(v->{db.setDocumentStatus(d.id,"Received");showClientDocuments(id);});x.addView(received,new LinearLayout.LayoutParams(-1,dp(40)));}
            body.addView(x);
        }
    }

    private void showClientPayments(long id){
        DBHelper.Client cl=db.client(id);activeNav="more";shell("Payments",cl==null?"Fee ledger":cl.name);
        Button add=actionButton("Add Payment",R.drawable.ic_add,true);add.setOnClickListener(v->paymentForm(id));body.addView(add,new LinearLayout.LayoutParams(-1,dp(52)));body.addView(spacer(8));
        List<DBHelper.Payment> ps=db.payments(id);if(ps.isEmpty())body.addView(emptyState("No payments","Track consultancy fees and dues."));
        for(DBHelper.Payment p:ps){
            LinearLayout x=card(16);LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.addView(tv(p.title,13,INK,true));tx.addView(tv("Due "+safe(p.due)+" • "+p.status,10,MUTED,false));r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));r.addView(tv("PKR "+String.format(Locale.US,"%.0f",p.amount),13,BLUE,true));x.addView(r);
            if(!"Paid".equalsIgnoreCase(p.status)){Button paid=actionButton("Mark Paid",R.drawable.ic_payment,false);paid.setOnClickListener(v->{db.setPaymentStatus(p.id,"Paid");showClientPayments(id);});x.addView(paid,new LinearLayout.LayoutParams(-1,dp(40)));}
            body.addView(x);
        }
    }

    private void documentForm(long id){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(8));
        EditText t=field(f,"Document title","Bank Statement");EditText cat=field(f,"Category","FBR Documents");Spinner st=dropdown(f,"Status",new String[]{"Required","Requested","Received","Verified"},"Required");EditText n=field(f,"Notes","");
        new AlertDialog.Builder(this).setTitle("Add Document").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{db.addDocument(id,val(t),val(cat),String.valueOf(st.getSelectedItem()),val(n));showClientDocuments(id);}).show();
    }

    private void paymentForm(long id){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(8));
        EditText t=field(f,"Payment title","Consultancy Fee");EditText a=field(f,"Amount","5000");a.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);EditText due=field(f,"Due date",new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new Date(System.currentTimeMillis()+7L*86400000L)));Spinner st=dropdown(f,"Status",new String[]{"Pending","Paid","Partially Paid","Overdue"},"Pending");
        new AlertDialog.Builder(this).setTitle("Add Payment").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{double amount=0;try{amount=Double.parseDouble(val(a));}catch(Exception ignored){}db.addPayment(id,val(t),amount,val(due),String.valueOf(st.getSelectedItem()),"");showClientPayments(id);}).show();
    }

    private void showFilingsHub(){activeNav="more";shell("Filing Center","All clients and return workload");for(DBHelper.Client cl:db.clients("")){LinearLayout x=card(16);x.addView(tv(cl.name,13,INK,true));x.addView(tv(db.filings(cl.id).size()+" filing records • "+safe(cl.taxType),10,MUTED,false));x.setOnClickListener(v->showClientFilings(cl.id));body.addView(x);}}
    private void showDocumentsHub(){activeNav="more";shell("Documents","Client document checklists");for(DBHelper.Client cl:db.clients("")){int n=db.documents(cl.id).size();if(n>0){LinearLayout x=card(16);x.addView(tv(cl.name,13,INK,true));x.addView(tv(n+" document records",10,MUTED,false));x.setOnClickListener(v->showClientDocuments(cl.id));body.addView(x);}}}
    private void showPaymentsHub(){activeNav="more";shell("Payments","Consultancy fees and dues");for(DBHelper.Client cl:db.clients("")){int n=db.payments(cl.id).size();if(n>0){LinearLayout x=card(16);x.addView(tv(cl.name,13,INK,true));x.addView(tv(n+" payment records",10,MUTED,false));x.setOnClickListener(v->showClientPayments(cl.id));body.addView(x);}}}
    private void showBackupInfo(){
        activeNav="more";shell("Backup & Restore","Protect all local ERP data");
        LinearLayout info=card(18);
        info.addView(tv("Full JSON Backup",15,INK,true));
        info.addView(tv("Clients, filings, reminders, payments and document records are included.",11,MUTED,false));
        body.addView(info);
        Button export=actionButton("Export Backup",R.drawable.ic_doc,true);
        export.setOnClickListener(v->exportBackup());
        body.addView(export,new LinearLayout.LayoutParams(-1,dp(52)));
        body.addView(spacer(8));
        Button restore=actionButton("Restore Backup",R.drawable.ic_settings,false);
        restore.setOnClickListener(v->importBackup());
        body.addView(restore,new LinearLayout.LayoutParams(-1,dp(52)));
        body.addView(spacer(12));
        body.addView(emptyState("Safe restore","Restore replaces current local ERP records with the selected FBR Return Filer backup."));
    }
    private void showSettingsInfo(){
        activeNav="more";shell("Settings","FBR Return Filer preferences");
        body.addView(infoCard("App","FBR Return Filer Pro V12",R.drawable.ic_settings,false));
        body.addView(infoCard("Storage","Private SQLite + JSON backup",R.drawable.ic_doc,false));
        body.addView(infoCard("Reminder channel","Local notification + WhatsApp",R.drawable.ic_bell,false));

        body.addView(section("System Readiness"));
        LinearLayout diag=card(18);
        boolean notificationReady=android.os.Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        boolean exactReady=android.os.Build.VERSION.SDK_INT<31 || (am!=null && am.canScheduleExactAlarms());
        diag.addView(infoLineReport("Notifications",notificationReady?"Allowed":"Permission needed",notificationReady?GREEN:ORANGE));
        diag.addView(infoLineReport("Exact alarms",exactReady?"Allowed":"Using fallback",exactReady?GREEN:ORANGE));
        diag.addView(infoLineReport("Database",db.countClients()+" clients loaded",BLUE));
        body.addView(diag);

        if(!notificationReady && android.os.Build.VERSION.SDK_INT>=33){
            Button n=actionButton("Allow Notifications",R.drawable.ic_bell,true);
            n.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100));
            body.addView(n,new LinearLayout.LayoutParams(-1,dp(50)));body.addView(spacer(8));
        }
        if(!exactReady && android.os.Build.VERSION.SDK_INT>=31){
            Button e=actionButton("Open Exact Alarm Permission",R.drawable.ic_calendar,false);
            e.setOnClickListener(v->{try{Intent i=new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception ex){toast("Settings open nahi ho saki");}});
            body.addView(e,new LinearLayout.LayoutParams(-1,dp(50)));
        }
    }

    private void clientForm(DBHelper.Client c){
        boolean edit=c!=null;
        ScrollView sv=new ScrollView(this);LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(8));sv.addView(f);
        EditText name=field(f,"Client Name",edit?c.name:"");
        EditText wa=field(f,"WhatsApp Number",edit?c.whatsapp:"");
        EditText phone=field(f,"Phone",edit?c.phone:"");
        EditText ntn=field(f,"NTN Number",edit?c.ntn:"");
        EditText cnic=field(f,"CNIC Number",edit?c.cnic:"");
        EditText business=field(f,"Business / Profession",edit?c.business:"");
        Spinner type=dropdown(f,"Filing Type",new String[]{"Income Tax Return (ITR)","Sales Tax Return (STR)","Income + Sales Tax","Salaried / Business"},edit?c.taxType:"Income Tax Return (ITR)");
        Spinner status=dropdown(f,"Client Status",new String[]{"Active","Pending","Filed"},edit?c.status:"Active");
        EditText due=field(f,"Next Due Date",edit?c.nextDue:"");
        EditText email=field(f,"Email",edit?c.email:"");
        EditText address=field(f,"Address",edit?c.address:"");
        EditText notes=field(f,"Notes",edit?c.notes:"");
        new AlertDialog.Builder(this).setTitle(edit?"Edit Client":"Add New Client").setView(sv).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            if(val(name).isEmpty()){toast("Client name required");return;}
            long id=db.saveClient(edit?c.id:0,val(name),val(wa),val(phone),val(cnic),val(ntn),val(business),String.valueOf(type.getSelectedItem()),String.valueOf(status.getSelectedItem()),val(due),val(email),val(address),val(notes));
            showClient(id);
        }).show();
    }

    private Spinner dropdown(LinearLayout parent,String label,String[] items,String selected){
        TextView l=tv(label,11,MUTED,true);l.setPadding(0,dp(4),0,dp(4));parent.addView(l);
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);
        s.setAdapter(a);
        int pick=0;
        if(selected!=null)for(int i=0;i<items.length;i++)if(items[i].equalsIgnoreCase(selected)){pick=i;break;}
        s.setSelection(pick);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.setMargins(0,0,0,dp(9));parent.addView(s,p);
        return s;
    }

    private EditText field(LinearLayout parent,String hint,String value){
        EditText e=new EditText(this);e.setHint(hint);e.setText(value==null?"":value);e.setTextSize(13);e.setSingleLine(!(hint.equals("Notes")||hint.equals("Address")));e.setPadding(dp(12),0,dp(12),0);e.setBackground(outline(Color.WHITE,14,LINE));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp((hint.equals("Notes")||hint.equals("Address"))?70:50));p.setMargins(0,0,0,dp(9));parent.addView(e,p);return e;
    }

    private String val(EditText e){return e.getText().toString().trim();}

    private void reminderForm(long clientId){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(8));

        List<DBHelper.Client> clientChoices=db.clients("");
        ArrayList<String> labels=new ArrayList<>();labels.add("General Reminder");
        int selectedClient=0;
        for(int i=0;i<clientChoices.size();i++){
            DBHelper.Client cc=clientChoices.get(i);
            labels.add(cc.name+" • "+safe(cc.ntn));
            if(cc.id==clientId)selectedClient=i+1;
        }
        TextView clientLabel=tv("Client",11,MUTED,true);clientLabel.setPadding(0,dp(4),0,dp(4));f.addView(clientLabel);
        Spinner clientSpinner=new Spinner(this);
        clientSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        clientSpinner.setSelection(selectedClient);f.addView(clientSpinner,new LinearLayout.LayoutParams(-1,dp(50)));

        EditText title=field(f,"Reminder title","FBR Return Reminder");
        EditText msg=field(f,"Message","Kindly apne required FBR return documents provide kar dein.");
        EditText mins=field(f,"Remind after minutes","10");mins.setInputType(InputType.TYPE_CLASS_NUMBER);
        Spinner repeat=dropdown(f,"Repeat",new String[]{"Once","Monthly"},"Monthly");

        new AlertDialog.Builder(this).setTitle("Schedule Reminder").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Schedule",(d,w)->{
            int m=10;try{m=Math.max(1,Integer.parseInt(val(mins)));}catch(Exception ignored){}
            long targetClient=0;
            int pos=clientSpinner.getSelectedItemPosition();
            if(pos>0 && pos-1<clientChoices.size())targetClient=clientChoices.get(pos-1).id;
            long at=System.currentTimeMillis()+m*60000L;
            String rule=String.valueOf(repeat.getSelectedItem());
            long rid=db.addReminder(targetClient,val(title),val(msg),at,rule,"Local + WhatsApp");
            ReminderScheduler.schedule(this,rid,val(title),val(msg),at,rule);
            toast("Reminder scheduled");
            showReminders();
        }).show();
    }

    private void filingForm(long clientId){
        LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(8));
        Spinner month=dropdown(f,"Month",new String[]{"January","February","March","April","May","June","July","August","September","October","November","December"},new SimpleDateFormat("MMMM",Locale.US).format(new Date()));
        EditText year=field(f,"Year",new SimpleDateFormat("yyyy",Locale.US).format(new Date()));year.setInputType(InputType.TYPE_CLASS_NUMBER);
        Spinner type=dropdown(f,"Filing Type",new String[]{"Income Tax Return (ITR)","Sales Tax Return (STR)","Annual Income Tax Return","Withholding Statement"},"Income Tax Return (ITR)");
        EditText due=field(f,"Due Date",new SimpleDateFormat("dd MMM yyyy",Locale.US).format(new Date(System.currentTimeMillis()+7L*86400000L)));
        new AlertDialog.Builder(this).setTitle("Add Filing").setView(f).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            int y=Calendar.getInstance().get(Calendar.YEAR);try{y=Integer.parseInt(val(year));}catch(Exception ignored){}
            db.addFiling(clientId,String.valueOf(month.getSelectedItem()),y,String.valueOf(type.getSelectedItem()),val(due),"Pending","");
            showClient(clientId);
        }).show();
    }

    private void scheduleLocal(String title,String msg,long at){
        long rid=db.addReminder(0,title,msg,at,"Once","Local");
        ReminderScheduler.schedule(this,rid,title,msg,at,"Once");
        toast("Reminder scheduled");
    }

    private void openWhatsApp(DBHelper.Client c){
        try{
            String no=safe(c.whatsapp).replaceAll("[^0-9]","");
            String msg="Assalam o Alaikum "+c.name+", aap ka FBR Return reminder hai. Next due date: "+safe(c.nextDue)+". Kindly required documents provide kar dein. Regards, FBR Return Filer";
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+no+"?text="+URLEncoder.encode(msg,"UTF-8"))));
        }catch(Exception e){toast("WhatsApp open nahi ho saka");}
    }

    private void openUrl(String u){
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){toast("Link open nahi ho saka");}
    }

    private void exportBackup(){
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"FBR-Return-Filer-Backup-"+new SimpleDateFormat("yyyy-MM-dd-HHmm",Locale.US).format(new Date())+".json");
        startActivityForResult(i,REQ_EXPORT_BACKUP);
    }

    private void importBackup(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i,REQ_IMPORT_BACKUP);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();
        try{
            if(requestCode==REQ_EXPORT_BACKUP){
                String json=db.exportJson();
                OutputStream out=getContentResolver().openOutputStream(uri);
                if(out==null)throw new IOException("Cannot open backup destination");
                out.write(json.getBytes(StandardCharsets.UTF_8));out.flush();out.close();
                toast("Backup exported successfully");
            }else if(requestCode==REQ_IMPORT_BACKUP){
                new AlertDialog.Builder(this).setTitle("Restore backup?")
                    .setMessage("Current local ERP records will be replaced by this backup.")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Restore",(d,w)->{
                        try{
                            InputStream in=getContentResolver().openInputStream(uri);
                            if(in==null)throw new IOException("Cannot open backup");
                            ByteArrayOutputStream bos=new ByteArrayOutputStream();
                            byte[] buf=new byte[8192];int n;
                            while((n=in.read(buf))>0)bos.write(buf,0,n);
                            in.close();
                            db.importJson(bos.toString("UTF-8"));
                            rescheduleAllReminders();
                            toast("Backup restored");
                            showDashboard();
                        }catch(Exception e){Log.e("FBRReturnFiler","Restore failed",e);toast("Backup restore failed");}
                    }).show();
            }
        }catch(Exception e){Log.e("FBRReturnFiler","Backup action failed",e);toast("Backup action failed");}
    }

    private void rescheduleAllReminders(){
        long now=System.currentTimeMillis();
        for(DBHelper.Reminder r:db.reminders(0)){
            if(!"Scheduled".equals(r.status))continue;
            long at=Math.max(r.at,now+30000L);
            ReminderScheduler.schedule(this,r.id,r.title,r.message,at,r.repeat);
        }
    }

    private String safe(String s){return s==null||s.trim().isEmpty()?"—":s;}

    private int statusBg(String s){
        if(s==null) return Color.rgb(237,244,255);
        if(s.contains("Overdue")) return Color.rgb(255,233,235);
        if(s.contains("Pending")) return Color.rgb(255,243,227);
        if(s.contains("Filed")) return Color.rgb(229,249,238);
        return Color.rgb(232,244,255);
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    public class FbrBuildingView extends View {
        Paint p=new Paint(1);
        public FbrBuildingView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            int w=getWidth(),h=getHeight();
            LinearGradient sky=new LinearGradient(0,0,0,h,Color.rgb(228,242,255),Color.WHITE,Shader.TileMode.CLAMP);
            p.setShader(sky);c.drawRect(0,0,w,h,p);p.setShader(null);

            p.setColor(Color.rgb(255,255,255));p.setShadowLayer(10,0,4,0x22000000);
            RectF base=new RectF(dp(24),dp(128),w-dp(24),h);c.drawRoundRect(base,dp(6),dp(6),p);p.clearShadowLayer();

            p.setColor(Color.rgb(217,229,242));
            for(int i=0;i<7;i++){float x=dp(40)+i*(w-dp(80))/6f;c.drawRect(x-dp(5),dp(165),x+dp(5),h-dp(18),p);}
            p.setColor(Color.rgb(198,216,236));Path roof=new Path();roof.moveTo(dp(28),dp(130));roof.lineTo(w/2f,dp(94));roof.lineTo(w-dp(28),dp(130));roof.close();c.drawPath(roof,p);
            p.setColor(NAVY);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(20));c.drawText("FBR",w/2f,dp(146),p);

            p.setStrokeWidth(dp(2));p.setColor(Color.rgb(80,104,120));c.drawLine(dp(72),dp(42),dp(72),dp(125),p);
            p.setColor(Color.rgb(14,112,87));Path flag=new Path();flag.moveTo(dp(74),dp(44));flag.lineTo(dp(126),dp(58));flag.lineTo(dp(74),dp(76));flag.close();c.drawPath(flag,p);
            p.setColor(Color.WHITE);c.drawCircle(dp(100),dp(58),dp(9),p);p.setColor(Color.rgb(14,112,87));c.drawCircle(dp(104),dp(56),dp(8),p);p.setColor(Color.WHITE);p.setTextSize(dp(12));c.drawText("★",dp(111),dp(62),p);
        }
    }
}
