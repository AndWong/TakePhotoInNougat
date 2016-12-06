package com.demo.takephoto;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    // 截图
    public static final int REQUEST_TYPE_CROP = 100;
    // 请求图库
    public static final int REQUEST_TYPE_FILE = 200;
    // 请求相机
    public static final int REQUEST_TYPE_CAMERA = 300;
    // 相机拍照完的照片文件名称
    public static final String CAMERA_TMP_FILE = "camera_tmp.jpg";

    private Button takePhoto;
    private Button pickPhoto;
    private ImageView imageView;
    private Uri outputUri; //照片 截取输出的outputUri， 只能使用 Uri.fromFile，不能用FileProvider.getUriForFile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePhoto = (Button) findViewById(R.id.button);
        pickPhoto = (Button) findViewById(R.id.button2);
        imageView = (ImageView) findViewById(R.id.imageView);

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File tmpFile = new File(Environment.getExternalStorageDirectory(), CAMERA_TMP_FILE);
                Uri uri = Uri.fromFile(tmpFile);
                uri = convertFileUriToFileProviderUri(uri);
                Intent cameraIntent = getCaptureIntent(uri);
                startActivityForResult(cameraIntent, REQUEST_TYPE_CAMERA);
            }
        });

        pickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_TYPE_FILE);
            }
        });
    }

    /**
     * 获取拍照的Intent
     *
     * @return
     */
    private Intent getCaptureIntent(Uri outPutUri) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); //申请权限
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//设置Action为拍照
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outPutUri);//将拍取的照片保存到指定URI
        return intent;
    }

    /**
     * 获取拍照的Uri
     * 适配7.0
     *
     * @param uri
     * @return
     */
    private Uri convertFileUriToFileProviderUri(Uri uri) {
        if (Build.VERSION.SDK_INT >= 24) {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return FileProvider.getUriForFile(MainActivity.this, "com.demo.takephoto.fileprovider", new File(uri.getPath()));
            }
        }
        return uri;
    }

    /**
     * 进行剪裁
     *
     * @param uri
     */
    protected void doCropPhoto(Uri uri) {
        try {
            File tmpFile = new File(Environment.getExternalStorageDirectory(), CAMERA_TMP_FILE);
            outputUri = Uri.fromFile(tmpFile);
            if (uri == null) {
                uri = outputUri;
            }
            uri = convertFileUriToFileProviderUri(uri);
            final Intent intent = getCropImageIntent(uri, outputUri);
            startActivityForResult(intent, REQUEST_TYPE_CROP);
        } catch (Exception e) {
        }
    }

    /**
     * 裁剪的Intent
     *
     * @param photoUri
     * @param outputUri
     * @return
     */
    public static Intent getCropImageIntent(Uri photoUri, Uri outputUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); //申请权限
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        int iconSize = 200;
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
        intent.putExtra("outputX", iconSize);
        intent.putExtra("outputY", iconSize);
        intent.putExtra("return-data", true);
        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_TYPE_CAMERA:
                    doCropPhoto(null);
                    break;
                case REQUEST_TYPE_FILE:
                    Uri imageUri = data.getData();
                    doCropPhoto(imageUri);
                    break;
                case REQUEST_TYPE_CROP:
                    if (null != outputUri) {
                        Bitmap photo = BitmapFactory.decodeFile(outputUri.getPath());
                        imageView.setImageBitmap(photo);
                    }
                    break;
            }
        }
    }
}
