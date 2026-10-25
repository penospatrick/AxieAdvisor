package com.axie.advisor;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private LinearLayout root;
    private EditText energy, costs, scores;
    private TextView result, scanStatus;
    private static final int CAPTURE_REQUEST=110;
    private android.media.projection.MediaProjectionManager projectionManager;
    private boolean autoScan=false;
    private final android.content.BroadcastReceiver scanReceiver=new android.content.BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){scanStatus.setText("Captured "+intent.getIntExtra("count",0)+" screen image(s). Card recognition is not yet implemented.");}};
    @Override public void onCreate(Bundle state){super.onCreate(state);
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,32,28,24);scroll.addView(root);setContentView(scroll);
        TextView title=new TextView(this);title.setText("Axie Classic Battle Advisor");title.setTextSize(25);title.setTextColor(Color.rgb(35,80,155));root.addView(title);
        TextView subtitle=new TextView(this);subtitle.setText("v0.2 · Screen capture available. Automatic card recognition is not yet implemented.");root.addView(subtitle);
        projectionManager=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        TextView label=new TextView(this);label.setText("SCREEN CAPTURE · v0.2");label.setTextSize(21);root.addView(label);
        scanStatus=new TextView(this);scanStatus.setText("Capture inactive. Images stay in app-private storage.");root.addView(scanStatus);
        button("Start automatic scanning (every 3 seconds)",()->{autoScan=true;requestCapture();});
        button("Start manual scanning",()->{autoScan=false;requestCapture();});
        button("Analyze now (capture screen)",()->{Intent i=new Intent(this,CaptureService.class);i.setAction(CaptureService.ACTION_SCAN);startService(i);});
        button("Stop screen capture",()->{stopService(new Intent(this,CaptureService.class));scanStatus.setText("Capture stopped.");});
        button("Enable floating bubble",()->{
            if(!Settings.canDrawOverlays(this)){Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}
            else{startService(new Intent(this,BubbleService.class));Toast.makeText(this,"Bubble enabled",Toast.LENGTH_SHORT).show();}
        });
        button("Close floating bubble",()->stopService(new Intent(this,BubbleService.class)));
        energy=field("Available energy (e.g. 3)","3");
        costs=field("Card energy costs, comma separated (e.g. 1,1,2)","1,1,2");
        scores=field("Estimated card values, comma separated (e.g. 80,60,110)","80,60,110");
        button("Recommend cards",()->calculate());
        result=new TextView(this);result.setTextSize(19);result.setPadding(0,20,0,0);root.addView(result);
        calculate();
    }
    private void requestCapture(){startActivityForResult(projectionManager.createScreenCaptureIntent(),CAPTURE_REQUEST);}
    @Override protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);
        if(request==CAPTURE_REQUEST&&code==RESULT_OK&&data!=null){
            Intent service=new Intent(this,CaptureService.class);service.putExtra("code",code);service.putExtra("data",data);service.putExtra("auto",autoScan);
            startForegroundService(service);scanStatus.setText(autoScan?"Automatic capture started.":"Manual capture started. Tap Analyze now.");
        }
    }
    @Override protected void onStart(){super.onStart();android.content.IntentFilter filter=new android.content.IntentFilter(CaptureService.ACTION_RESULT);if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(scanReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(scanReceiver,filter);}
    @Override protected void onStop(){unregisterReceiver(scanReceiver);super.onStop();}

    private EditText field(String hint,String value){TextView label=new TextView(this);label.setText(hint);root.addView(label);EditText input=new EditText(this);input.setSingleLine(true);input.setText(value);root.addView(input);return input;}
    private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->action.run());}
    private void calculate(){try{
        int available=Integer.parseInt(energy.getText().toString().trim());
        String[] c=costs.getText().toString().split(","),s=scores.getText().toString().split(",");
        int n=c.length;if(n!=s.length||n<1||n>16)throw new IllegalArgumentException("Provide 1–16 cards and matching values.");
        int[] cost=new int[n],val=new int[n];for(int i=0;i<n;i++){cost[i]=Integer.parseInt(c[i].trim());val[i]=Integer.parseInt(s[i].trim());if(cost[i]<0)throw new IllegalArgumentException("Cost must be nonnegative.");}
        int best=-1,mask=0,spent=0;
        for(int m=0;m<(1<<n);m++){int totalCost=0,totalValue=0;for(int i=0;i<n;i++)if((m&(1<<i))!=0){totalCost+=cost[i];totalValue+=val[i];}
            if(totalCost<=available&&totalValue>best){best=totalValue;mask=m;spent=totalCost;}}
        StringBuilder b=new StringBuilder("Suggested cards: ");for(int i=0;i<n;i++)if((mask&(1<<i))!=0)b.append(i+1).append(" ");
        b.append("\nEnergy used: ").append(spent).append("/").append(available).append("\nEstimated value: ").append(best);
        b.append("\n\nThese values are manually entered, not real game predictions.");
        result.setText(b.toString());
    }catch(Exception ex){result.setText("Input error: "+ex.getMessage());}}
}