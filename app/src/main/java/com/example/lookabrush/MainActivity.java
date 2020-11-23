package com.example.lookabrush;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;

/**
 * @author by talon, Date on 19/6/23.
 * note:
 * [两个类实现Android录制屏幕功能](https://blog.csdn.net/u011368551/article/details/93798251)
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 10;
    private MediaProjectionManager mMediaProjectionManager;

    private StorageReference mStorageRef;
    private Uri file;
    private UploadTask uploadTask;

    Button button_sele,button_up;
    ImageView imageView;
    Intent intent;
    int PICK_CONTACT_REQUEST=1;
    Uri uri;
    String data_list;
    StorageReference storageReference,pic_storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission(this); //检查权限

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        mStorageRef = FirebaseStorage.getInstance().getReference();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        button_sele = (Button)findViewById(R.id.button);
        button_up = (Button)findViewById(R.id.button2);
        imageView = (ImageView)findViewById(R.id.imageView);

        button_sele.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent,1);

            }
        });
        button_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                pic_storage=storageReference.child("m4."+data_list);

                StorageReference riversRef = mStorageRef.child(uri.getLastPathSegment()+"."+data_list);
                riversRef.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i("kate","ok");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.d("kate", "fail");
                    }
                });
            }
        });

    }

    public void StartRecorder(View view) {
        createScreenCapture();
    }

    public void StopRecorder(View view) throws FileNotFoundException {
        Intent service = new Intent(this, ScreenRecordService.class);
        stopService(service);
        //2020.11.16 upload a file by firebase.
//        uploadTask = storageRef.child("video/"+file.getLastPathSegment()).putFile(file, metadata);
//        UploadTask.putFile();
        Toast.makeText(this, "結束录屏", Toast.LENGTH_SHORT).show();
//        UploadTask();

    }

    public static void checkPermission(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission =
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                //动态申请
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
            }
        }
    }


    private void createScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("kate", String.valueOf(requestCode));
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                Toast.makeText(this, "允许录屏", Toast.LENGTH_SHORT).show();

                Intent service = new Intent(this, ScreenRecordService.class);
                service.putExtra("resultCode", resultCode);
                service.putExtra("data", data);
                startService(service);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "拒绝录屏", Toast.LENGTH_SHORT).show();
        }

        //
        if(requestCode==PICK_CONTACT_REQUEST){
            uri = data.getData();
            imageView.setImageURI(uri);
            ContentResolver contentResolver = getContentResolver();
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            data_list = mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));

        }
    }


    //2020.11.16 upload a file by firebase.
    private void UploadTask() throws FileNotFoundException {
        /**
         * "/storage/emulated/0/Pictures/Screenshots/Screenshot_20201123-115050.png"
         * "/storage/emulated/0/DCIM/100ANDRO/DSC_0004.JPG"
         * "/storage/emulated/0/Download/2020-11-23-10-58-21.mp4"
         * Log.d("kate", String.valueOf(file));
         * uri.getLastPathSegment()
         * **/

//        InputStream stream = new FileInputStream(new File("/storage/emulated/0/DCIM/100ANDRO/DSC_0004.JPG"));
//        StorageReference mountainsRef = mStorageRef.child("DSC_0004.JPG");
//        uploadTask = mountainsRef.putStream(stream);
        /**
        String my_android_path = "/storage/emulated/0/DCIM/100ANDRO/DSC_0004.JPG";
//        Uri file = Uri.fromFile(new File(my_android_path));
        Uri file= Uri.parse(my_android_path);
        StorageReference riversRef = mStorageRef.child("images/"+file.getLastPathSegment()); //
        Log.d("kate", String.valueOf(file));
        Log.d("kate", String.valueOf(riversRef));
         **/


        StorageReference riversRef = mStorageRef.child(uri.getLastPathSegment()+"."+data_list);
        riversRef.putFile(uri)
//        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        // Uri downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
//                         Task<Uri> downloadUrl = taskSnapshot.getStorage().getDownloadUrl();
//                        Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                        Log.d("kate", "ok");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                        Log.d("kate", "fail");
                    }
                });

    }

}