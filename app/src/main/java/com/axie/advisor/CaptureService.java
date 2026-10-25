package com.axie.advisor;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import java.io.*;

public class CaptureService extends Service {
    public static final String ACTION_SCAN="com.axie.advisor.SCAN";
    public static final String ACTION_STOP="com.axie.advisor.STOP";
    public static final String ACTION_MODE="com.axie.advisor.MODE";
    public static final String ACTION_RESULT="com.axie.advisor.RESULT";
    private Handler handler=new Handler(Looper.getMainLooper());
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private boolean auto=false;
    private int width,height,density;
    private long lastCapture=0;
    private int count=0;
    @Override public void onCreate(){super.onCreate();NotificationChannel channel=new NotificationChannel("capture","Screen capture",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(channel);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        if(ACTION_STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(ACTION_MODE.equals(action)){auto=intent.getBooleanExtra("auto",false);return START_STICKY;}
        if(ACTION_SCAN.equals(action)){capture();return START_STICKY;}
        if(projection==null){
            Notification note=new Notification.Builder(this,"capture").setContentTitle("Axie Advisor capture active").setContentText("Screen capture is active; stop it in Axie Advisor").setSmallIcon(android.R.drawable.ic_menu_camera).build();
            startForeground(1,note,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(intent.getIntExtra("code",Activity.RESULT_CANCELED),(Intent)intent.getParcelableExtra("data"));
            if(projection==null){stopSelf();return START_NOT_STICKY;}
            DisplayMetrics metrics=getResources().getDisplayMetrics();width=metrics.widthPixels;height=metrics.heightPixels;density=metrics.densityDpi;
            reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},handler);
            display=projection.createVirtualDisplay("AxieAdvisorCapture",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
            auto=intent.getBooleanExtra("auto",false);
            handler.postDelayed(tick,1500);
        }
        return START_STICKY;
    }
    private final Runnable tick=new Runnable(){@Override public void run(){if(auto)capture();handler.postDelayed(this,3000);}};
    private void capture(){
        if(reader==null||System.currentTimeMillis()-lastCapture<700)return;
        Image image=reader.acquireLatestImage();
        if(image==null){handler.postDelayed(()->capture(),300);return;}
        try{
            Image.Plane plane=image.getPlanes()[0];int stride=plane.getRowStride();int pixel=plane.getPixelStride();
            Bitmap padded=Bitmap.createBitmap(width+(stride-pixel*width)/pixel,height,Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(plane.getBuffer());
            Bitmap cropped=Bitmap.createBitmap(padded,0,0,width,height);
            try(FileOutputStream out=openFileOutput("latest_capture.png",MODE_PRIVATE)){cropped.compress(Bitmap.CompressFormat.PNG,85,out);}
            padded.recycle();cropped.recycle();lastCapture=System.currentTimeMillis();count++;
            Intent notice=new Intent(ACTION_RESULT);notice.setPackage(getPackageName());notice.putExtra("count",count);sendBroadcast(notice);
        }catch(Exception e){android.util.Log.e("AxieAdvisor","Capture failed",e);}
        finally{image.close();}
    }
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(display!=null)display.release();if(reader!=null)reader.close();if(projection!=null)projection.stop();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}