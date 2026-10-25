package com.axie.advisor;

import android.app.Service;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.*;
import android.widget.*;

public class BubbleService extends Service {
    private WindowManager manager;
    private View bubble;
    private WindowManager.LayoutParams params;
    @Override public void onCreate() {
        super.onCreate();
        if (!android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return; }
        manager = (WindowManager)getSystemService(WINDOW_SERVICE);
        TextView icon = new TextView(this);
        icon.setText("AI");
        icon.setTextSize(19);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundColor(Color.rgb(38,100,190));
        bubble = icon;
        params = new WindowManager.LayoutParams(65,65,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP|Gravity.START; params.x = 20; params.y = 250;
        manager.addView(bubble,params);
        icon.setOnTouchListener(new View.OnTouchListener() {
            float downX,downY; int startX,startY; boolean moved;
            public boolean onTouch(View v, android.view.MotionEvent e) {
                switch(e.getAction()) {
                    case MotionEvent.ACTION_DOWN: downX=e.getRawX(); downY=e.getRawY(); startX=params.x; startY=params.y; moved=false; return true;
                    case MotionEvent.ACTION_MOVE: params.x=startX+(int)(e.getRawX()-downX); params.y=startY+(int)(e.getRawY()-downY); moved |= Math.abs(e.getRawX()-downX)>10 || Math.abs(e.getRawY()-downY)>10; manager.updateViewLayout(bubble,params); return true;
                    case MotionEvent.ACTION_UP: if(!moved){Intent i=new Intent(BubbleService.this,MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}return true;
                }
                return false;
            }
        });
    }
    @Override public void onDestroy(){if(manager!=null&&bubble!=null) manager.removeView(bubble);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}