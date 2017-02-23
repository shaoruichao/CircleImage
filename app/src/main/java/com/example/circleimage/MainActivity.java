package com.example.circleimage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @BindView(R.id.circleImageView)
    CircleImageView circleImageView;
    @BindView(R.id.activity_main)
    RelativeLayout activityMain;

    private Bitmap head;// 头像Bitmap
    // 截图返回的uri
    private Uri outPutUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //从手机上取已保存的图片
        File tempFiles = getTempFile();
        outPutUri = Uri.fromFile(tempFiles);
        try {
            head = ImageUtil.getBitmapFormUri(MainActivity.this,outPutUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (head != null) {
            circleImageView.setImageBitmap(head);// 用ImageView显示出来
        }

    }

    @OnClick(R.id.circleImageView)
    public void onClick() {
        showPhotoDialog();
    }

    /**
     * 头像
     */
    private void showPhotoDialog() {

        final AlertDialog dlg = new AlertDialog.Builder(this, R.style.MyDialogStyle).create();
        //点击空白区域消失
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
        Window window = dlg.getWindow();
        // 可以在此设置显示动画
        window.setWindowAnimations(R.style.mystyle);
        window.setGravity(Gravity.BOTTOM);
        //内容区域外围的灰色去掉了
//        window.setDimAmount(0);

        WindowManager.LayoutParams wl = window.getAttributes();

        // 以下这两句是为了保证按钮可以水平满屏
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // 设置显示位置
        dlg.onWindowAttributesChanged(wl);

        // 设置窗口的内容页面,shrew_exit_dialog.xml文件中定义view内容
        window.setContentView(R.layout.alertdialog);
        TextView tv_paizhao = (TextView) window.findViewById(R.id.tv_content1);
        tv_paizhao.setText("拍照");
        tv_paizhao.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent2.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(
                        Environment.getExternalStorageDirectory(), "head.jpg")));
                startActivityForResult(intent2, 2);// 采用ForResult打开

                dlg.cancel();
            }
        });
        TextView tv_xiangce = (TextView) window.findViewById(R.id.tv_content2);
        tv_xiangce.setText("相册");
        tv_xiangce.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                intent1.setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent1, 1);
                dlg.cancel();
            }
        });
        //取消
        Button bt_cancel = (Button) window.findViewById(R.id.bt_cancel);
        bt_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dlg.cancel();
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    cropPhoto(data.getData());// 裁剪图片
                }

                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    File temp = new File(Environment.getExternalStorageDirectory()
                            + "/head.jpg");
                    cropPhoto(Uri.fromFile(temp));// 裁剪图片
                }

                break;
            case 3:
                if (data != null) {

//                    Bundle extras = data.getExtras();
//                    head = extras.getParcelable("data");
//                    head = getBitmapFromBigImagByUri(outPutUri);//这个方法也是可行，应该是只是尺寸压缩，没有压缩质量，故调用了之前写的ImageUtil（不这么写的话，在裁剪那里的时候返回就甭了）
                    try {
                        head = ImageUtil.getBitmapFormUri(MainActivity.this,outPutUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (head != null) {
                        /*** 上传服务器代码*/
                        //Base64的编码
                        String imgCode = ImageUtil.getBitmapStrBase64(head);
                        //上传头像（base64格式上传）
//                        upload(imgCode);

                        //setPicToView(head);// 保存在SD卡中
                        circleImageView.setImageBitmap(head);// 用ImageView显示出来
                    }
                }
                break;
            default:
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
 * 通过Uri得到压缩以后的图片
 */
    private Bitmap getBitmapFromBigImagByUri(Uri uri) {
        Bitmap result = null;
        InputStream is1 = null;
        InputStream is2 = null;
        try {
            // 如果图片太大，这个地方依旧会出现问题
            // Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            // 使用两个inputstream的原因
            // http://stackoverflow.com/questions/12841482/resizing-bitmap-from-inputstream
            is1 = getContentResolver().openInputStream(uri);
            is2 = getContentResolver().openInputStream(uri);
            BitmapFactory.Options opts1 = new BitmapFactory.Options();
            opts1.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is1, null, opts1);
            int bmpWidth = opts1.outWidth;
            int bmpHeight = opts1.outHeight;
            int scale = Math.max(bmpWidth / 300, bmpHeight / 300);
            BitmapFactory.Options opts2 = new BitmapFactory.Options();
            // 缩放的比例
            opts2.inSampleSize = scale;
            // 内存不足时可被回收
            opts2.inPurgeable = true;
            // 设置为false,表示不仅Bitmap的属性，也要加载bitmap
            opts2.inJustDecodeBounds = false;
            result = BitmapFactory.decodeStream(is2, null, opts2);
        } catch (Exception ex) {
        } finally {
            if (is1 != null) {
                try {
                    is1.close();
                } catch (IOException e1) {
                }
            }
            if (is2 != null) {
                try {
                    is2.close();
                } catch (IOException e2) {
                }
            }
        }
        return result;
    }

    /**
     * 调用系统的裁剪
     *
     * @param uri
     */
    public void cropPhoto(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);// true:不返回uri，false：返回uri
        //获取uri并压缩  不然在乐视手机上会bug（剪裁的时候返回崩溃）
        File tempFiles = getTempFile();
        outPutUri = Uri.fromFile(tempFiles);
        intent.putExtra("output", outPutUri);
        Log.e(TAG, "cropPhoto: "+outPutUri );

        startActivityForResult(intent, 3);
    }

    private File getTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/crop_image.jpg");
        return file;
    }
}
