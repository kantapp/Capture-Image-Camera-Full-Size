package com.kantapp.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final String FOLDER_NAME = "CameraShot";
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int BITMAP_SAMPLE_SIZE = 8;
    private static final String KEY_IMAGE_STORAGE_PATH = "image_path";
    private ImageView imgPreview;
    private TextView txt_desc;
    private Button btnCapturePicture;
    private static String imageStorePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isDeviceSupportCamera(getApplicationContext()))
        {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        imgPreview = findViewById(R.id.imgPreview);
        btnCapturePicture = findViewById(R.id.btnCapturePicture);
        txt_desc=findViewById(R.id.txt_desc);

        btnCapturePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission(getApplicationContext()))
                {
                    captureCamera();
                }
                else
                {
                    requestCameraPermission(MEDIA_TYPE_IMAGE);
                }
            }
        });

        restoreFromBundle(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_IMAGE_STORAGE_PATH,imageStorePath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imageStorePath=savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
    }

    private void restoreFromBundle(Bundle savedInstanceState)
    {
        if (savedInstanceState!=null)
        {
            if(!savedInstanceState.containsKey(KEY_IMAGE_STORAGE_PATH))
            {
                imageStorePath=savedInstanceState.getString(KEY_IMAGE_STORAGE_PATH);
                if(!TextUtils.isEmpty(imageStorePath))
                {
                    if(imageStorePath.substring(imageStorePath.lastIndexOf(".")).equals("."+".jpg"))
                    {
                        previewCaptureImages();
                    }
                }
            }
        }
    }
    private void captureCamera() {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file=getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (file!=null)
        {
            imageStorePath=file.getAbsolutePath();
        }

        Uri fileUri=getOutputMediaFile(getApplicationContext(),file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
        startActivityForResult(intent,CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    private void previewCaptureImages()
    {
        try
        {
            txt_desc.setVisibility(View.GONE);
            imgPreview.setVisibility(View.VISIBLE);

            Bitmap bitmap=optimizeBitmap(BITMAP_SAMPLE_SIZE,imageStorePath);
            imgPreview.setImageBitmap(bitmap);
        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==CAMERA_CAPTURE_IMAGE_REQUEST_CODE)
        {
            if (resultCode==RESULT_OK)
            {
                refreshGalary(getApplicationContext(),imageStorePath);
                previewCaptureImages();
            }
            else if (resultCode==RESULT_CANCELED)
            {
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            }
            else
            {
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void requestCameraPermission(final int type) {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted())
                        {
                            captureCamera();
                        }
                        else
                        {
                            showPermissionAlert();
                        }

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public static boolean isDeviceSupportCamera(Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    public static boolean checkPermission(Context context)
    {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;
    }
    public void showPermissionAlert()
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Permissions required!");
        builder.setMessage("Camera needs few permissions to work properly. Grant them in settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openPermissionSetting(MainActivity.this);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
            }
        });
        builder.show();
    }

    public static void openPermissionSetting(Context context)
    {
        Intent intent=new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package",BuildConfig.APPLICATION_ID,null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    public static File getOutputMediaFile(int type)
    {
        File mediaStorageDir=new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES),FOLDER_NAME);
        if (!mediaStorageDir.exists())
        {
            if(!mediaStorageDir.mkdirs())
            {
                Log.e(FOLDER_NAME, " Oops! Failed create "
                        + FOLDER_NAME+ " directory");
                return null;
            }
        }

        String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File mediaFile;
        if (type==MEDIA_TYPE_IMAGE)
        {
            mediaFile=new File(mediaStorageDir.getPath()+File.separator+"IMG_"+timeStamp+".jpg");
        }
        else
        {
            return null;
        }

        return mediaFile;
    }

    public static Uri getOutputMediaFile(Context context,File file)
    {
        return FileProvider.getUriForFile(context,context.getPackageName()+".provider",file);
    }
    public static void refreshGalary(Context context,String filePath)
    {
        MediaScannerConnection.scanFile(context,
                new String[]{filePath},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                }
        );
    }
    public static Bitmap optimizeBitmap(int sampleSize,String filePath)
    {
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inSampleSize=sampleSize;
        return BitmapFactory.decodeFile(filePath,options);
    }
}
