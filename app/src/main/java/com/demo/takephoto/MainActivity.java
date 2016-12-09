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
import android.text.TextUtils;
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
        intent.putExtra("outputX", iconSize);
        intent.putExtra("outputY", iconSize);
        intent.putExtra("return-data", isReturnData());
        if (!isSpecifyMachine("meizu")) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri); // 魅族部分手机会有问题,例如 Meizu M351 4.4.4
        }
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
                    if (isSpecifyMachine("meizu")) {
                        Bitmap photo = data.getParcelableExtra("data");
                        /**
                         * 虽然部分魅族的机器没有返回data字段，但是返回了filePath，是相册选中地址的路径，
                         * 可以把这个图片按尺寸获取一下。注意可能丢失剪切效果，目前暂时这样处理的。
                         */
                        if (null == photo) {
                            String filePath = data.getStringExtra("filePath");
                            if (filePath.length() > 0) {
                                photo = decodeSampledBitmapFromFile(filePath, 250, 250);
                            }
                        }
                        imageView.setImageBitmap(photo);
                    } else {
                        if (null != outputUri) {
                            Bitmap photo = BitmapFactory.decodeFile(outputUri.getPath());
                            imageView.setImageBitmap(photo);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 由得到的采样率对图片进行解析
     *
     * @param filename
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filename, options);
    }


    /**
     * 计算实际采样率
     *
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

        }
        return inSampleSize;
    }

    /**
     * 是否裁剪之后返回数据
     **/
    private static boolean isReturnData() {
        if (isSpecifyMachine("lenovo") || isSpecifyMachine("meizu")) {
            return true;
        }
        return false;
    }

    /**
     * 是否是指定机器
     *
     * @return
     */
    private static boolean isSpecifyMachine(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String manufacturer = android.os.Build.MANUFACTURER;
        if (!TextUtils.isEmpty(manufacturer)) {
            if (manufacturer.toLowerCase().contains(name)) {
                return true;
            }
        }
        return false;
    }
}
