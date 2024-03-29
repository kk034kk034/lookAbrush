package com.example.lookabrush;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordingService";
    /**
     * 是否为标清视频
     */
    private boolean isVideoSd = false;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;
    //2020.11.16 確認mediaRecorder.setOutputFile裡面是不是放我要的影片路徑



    public ScreenRecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mResultCode = intent.getIntExtra("resultCode", 1);
        mResultData = intent.getParcelableExtra("data");

        getScreenBaseInfo();

        mMediaProjection = createMediaProjection();
        mMediaRecorder = createMediaRecorder();
        mVirtualDisplay = createVirtualDisplay(); // 必须在mediaRecorder.prepare() 之后调用，否则报错"fail to get surface"

        mMediaRecorder.start();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if(mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if(mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaProjection.stop();
            mMediaRecorder.reset();
        }
        if(mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }


    /**
     * 获取屏幕相关数据
     */
    private void getScreenBaseInfo() {
        mScreenWidth = ScreenUtils.getScreenWidth(this);
        mScreenHeight = ScreenUtils.getScreenHeight(this);
//        mScreenDensity = ScreenUtils.getScreenDensityDpi(this);
//        mScreenWidth = 100;
//        mScreenHeight = 200;
        mScreenDensity =100;
    }

    private MediaProjection createMediaProjection() {
        Log.i(TAG, "Create MediaProjection");
        return ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, mResultData);
    }


    private MediaRecorder createMediaRecorder() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate).replace(" ", "");
        //2020.11.16
        String filename = formatter.format(new Date());

        String videoQuality = "HD";
        if (isVideoSd) videoQuality = "SD";

        Log.i(TAG, "Create MediaRecorder");
        MediaRecorder mediaRecorder = new MediaRecorder();
//        if(isAudio) mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        mediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + videoQuality + curTime + ".mp4"); //存影片路徑嗎?
//        mediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename + ".mp4");

        String filename_ = "/storage/emulated/0/Brush_"+filename+".mp4";
        Log.i("kate", "影片路徑存在?>>filename_: " +filename_);
        mediaRecorder.setOutputFile(filename_);

//        File tempFile = new File(Environment.getExternalStorageDirectory(), filename + ".mp4");
//        Uri VideoUri = getVideoContentUri(tempFile);
//        // File or Blob
//        Uri file = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), filename + ".mp4"));
//        // Create the file metadata
//        metadata = new StorageMetadata.Builder().setContentType("video/mp4").build(); //Type("image/jpeg")
//        // Upload file and metadata to the path 'images/mountains.jpg'
//        uploadTask = storageRef.child("video/"+file.getLastPathSegment()).putFile(file, metadata);


        mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);  //after setVideoSource(), setOutFormat()
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
//        if(isAudio) mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  //after setOutputFormat()
        int bitRate;
        if (isVideoSd) {
            mediaRecorder.setVideoEncodingBitRate(mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(30);
            bitRate = mScreenWidth * mScreenHeight / 1000;
        } else {
            mediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
            bitRate = 5 * mScreenWidth * mScreenHeight / 1000;
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mediaRecorder;
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.i(TAG, "Create VirtualDisplay");
        return mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }
}