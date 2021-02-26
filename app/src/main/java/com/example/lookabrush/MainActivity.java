package com.example.lookabrush;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * @author by Y.P.LIN, Date on 2017年4月4日 星期二
 * note:
 * [android5.0以上,用MediaProjection API 錄製螢幕畫面](http://yplin123.blogspot.com/2017/04/android50mediaprojection-api.html)
 */
public class MainActivity extends Activity {

    private MediaProjectionManager mpm;
    private MediaProjection mp;
    private VirtualDisplay vp_recorder=null;//recorder screen
    private MediaRecorder mMediaRecorder;
    private static final int REQUEST_NUMBER=1001; //1;

    // Tools>Firebase
    private StorageReference mStorageRef;
    private Uri file;
    private UploadTask uploadTask;
    //公用變數
    Intent intent;
    int PICK_CONTACT_REQUEST=1;
    Uri uri;
    String data_list;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    Date curDate = new Date(System.currentTimeMillis());
    String filename = formatter.format(new Date());
    String filename_ = "/storage/emulated/0/Brush_"+filename+".mp4";
    //宣布UI用到的
    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.imageView);

        //螢幕錄影
        mMediaRecorder=new MediaRecorder();
        mpm=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); //media projection manager
        startActivityForResult(mpm.createScreenCaptureIntent(),REQUEST_NUMBER); //開始授權media projection

        //上傳影片至Firebase
        mStorageRef = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        checkPermission(MainActivity.this); //检查权限
        if(REQUEST_NUMBER==requestCode) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "USER CANCELLED", Toast.LENGTH_LONG).show();
                return;
            }
            DisplayMetrics ds = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(ds);
            int dpi = ds.densityDpi;
            int dw = ds.widthPixels;
            int dh = ds.heightPixels;
            mp = mpm.getMediaProjection(resultCode, data);
            //B.螢幕錄製,AndroidManifest記得給android.permission.RECORD_AUDIO
            if (vp_recorder == null) {//只是確保一個virtual display
                //mMediaRecorder = new MediaRecorder();                                     //這個螢幕錄製,這個和capture screen不相干
                init_media_recorder();             //初始化media recorder
                prepare_media_recorder();      //準備錄製
                //建立虛擬display for recorder screen
                vp_recorder = mp.createVirtualDisplay("ScreenRecorder", dw, dh, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);

            }

        }
    }
    public static void checkPermission(MainActivity activity) {
        /*動態代碼：
        有些權限屬於受保護的權限，類型權限只在AndroidManifest.xml中聲明是無法真正獲取到的，
        還需要在代碼中動態獲取，然後再運行時用戶在權限許可上可以點擊一下“允許”，方可真正獲得此權限。*/
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission =
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                //动态申请
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
            }
        }
    }
    public void start_Recorder(View v){
        //init_media_recorder();         //初始化media recorder
        //prepare_media_recorder();      //準備錄製
        mMediaRecorder.start();        //開始錄製
        this.moveTaskToBack(true);   //app退到後台
        Intent intent = getPackageManager().getLaunchIntentForPackage("cn.com.buildwin.YCamera3"); //第三方APP的id
        startActivity(intent);  //跳轉其他APP
    }
    public void stop_Recorder(View v){
        mMediaRecorder.stop();         //停止錄製
        mMediaRecorder.reset();
        this.upload();
    }
    public void upload(){//View v
        Toast.makeText(this, "uploading...until ok.", Toast.LENGTH_LONG).show();
        // "path/to/images/rivers.jpg"
        // "/storage/emulated/0/Brush_"+filename+".mp4"
         Uri file = Uri.fromFile(new File(filename_)); //只給"/storage/emulated/0/"母目錄不行，要含檔案的整個路徑
         StorageReference riversRef = mStorageRef.child("Brush_"+filename+".mp4"); //指某檔案類別中的某個檔案
        //StorageReference riversRef = mStorageRef.child(uri.getLastPathSegment()+"."+ data_list);
        riversRef.putFile(file) //uri
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Log.i("kate","upload ok");
                        Toast.makeText(MainActivity.this, "upload ok", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.d("kate", "upload fail");
                        Toast.makeText(MainActivity.this, "upload fail", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //初始化media recorder,AndroidManifest記得給android.permission.RECORD_AUDIO
    private void init_media_recorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);    //音源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);//影源
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //影像解碼器
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//音源解碼器
        mMediaRecorder.setVideoEncodingBitRate(512*1000);                 //解碼率
        mMediaRecorder.setVideoFrameRate(30);                             //視窗更新頻率
//        mMediaRecorder.setVideoSize(480,640);                //影像寬高
        int mScreenWidth = ScreenUtils.getScreenWidth(this);
        int mScreenHeight = ScreenUtils.getScreenHeight(this);
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);  //after setVideoSource(), setOutFormat()
//        mMediaRecorder.setOutputFile("/sdcard/capture.mp4");              //儲存路徑

        Log.i("kate","init_media_recorder(位置與檔名)>> "+filename_);
        mMediaRecorder.setOutputFile(filename_);
        //mMediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename + ".mp4");

    }
    //準備錄製
    private void prepare_media_recorder(){
        try {
            mMediaRecorder.prepare();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            finish();
        }
    }
}