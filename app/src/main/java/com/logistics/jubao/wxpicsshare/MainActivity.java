package com.logistics.jubao.wxpicsshare;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

public class MainActivity extends AppCompatActivity {

    private List<File> imgFile;
    private Button btnShare;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
    }

    /**
     * 图片地址，自行定义，必须是本地图片，若是网络图片则下载存储到本地
     */
    private void initData() {
        imgFile = new ArrayList<>();
        imgFile.add(new File("/storage/emulated/0/Android/data/com.xu.sharemoreimage/cache/iv_share_4.jpg"));
        imgFile.add(new File("/storage/emulated/0/Android/data/com.xu.sharemoreimage/cache/iv_share_5.jpg"));
        imgFile.add(new File("/storage/emulated/0/Android/data/com.xu.sharemoreimage/cache/iv_share_6.jpg"));
    }

    private void initView() {
        btnShare = (Button) findViewById(R.id.btn_share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
    }

    /**
     * 检查读取系统内存权限
     */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
                return;
            } else {
                shareToWx();
            }
        } else {
            shareToWx();
        }
    }

    /**
     * intent分享多图+文字到微信
     */
    private void shareToWx() {
        Intent intent = new Intent();
//        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI"));  分享给好友
        intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareToTimeLineUI"));
        intent.putExtra("Kdescription", "分享测试");
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        ArrayList<Uri> imageUris = new ArrayList<>();
        for (File f : imgFile) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                imageUris.add(getImageContentUri(MainActivity.this, f));
            } else {
                imageUris.add(Uri.fromFile(f));
            }
        }

        for (int i = 0; i < imageUris.size(); i++) {
            Log.e("uri: ", imageUris.get(i).toString());
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        startActivity(intent);
    }

    /**
     * 获取图片的绝对的分享地址（待研究）
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * 权限回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    shareToWx();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "READ_EXTERNAL_STORAGE_DENIED", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
