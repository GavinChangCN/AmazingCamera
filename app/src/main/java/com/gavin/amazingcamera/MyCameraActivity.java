package com.gavin.amazingcamera;

import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gavin.amazingcamera.base.eventbus.EventBusConfiguration;
import com.gavin.amazingcamera.base.view.BaseActivity;
import com.gavin.amazingcamera.eventbus.OcrEvent;
import com.gavin.amazingcamera.http.IDCardRecog;
import com.gavin.amazingcamera.widget.MySurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Gavin
 * E-mail: gavin.zhang@healthbok.com
 * Date:  2015/12/3 0003
 */
public class MyCameraActivity extends BaseActivity {

    private ImageView btn_camera_capture = null;
    private ImageView btn_back = null;
    private TextView txt_back = null;
    private ImageView btn_resume = null;
    private TextView txt_resume = null;
    private TextView txt_confirm = null;

    private Camera mCamera = null;
    private MySurfaceView mySurfaceView = null;

    private final String SAVE_PIC_DIR = "AmazingCamera";

    private byte[] buffer = null;

    private final int TYPE_FILE_IMAGE = 1;
    private final int TYPE_FILE_VEDIO = 2;

    private IDCardRecog idCardRecog;
    private String apixKey = "1c62211d344a453a720437bad34ad9ea";

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null){
                Log.d("MyPicture", "picture taken data: null");
            }else{
                Log.d("MyPicture", "picture taken data: " + data.length);
            }

            buffer = new byte[data.length];
            buffer = data.clone();
        }
    };

    @Override
    protected EventBusConfiguration loadEventBusConfiguration() {
        return EventBusConfiguration.KEEP_ALIVE_ONCREATE_PAIR;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mycamera_layout);

        btn_camera_capture = (ImageView) findViewById(R.id.camera_capture);
        btn_back = (ImageView) findViewById(R.id.back_Button);
        txt_back = (TextView) findViewById(R.id.back_txt);
        btn_resume = (ImageView) findViewById(R.id.resume_Button);
        txt_resume = (TextView) findViewById(R.id.resume_txt);
        txt_confirm = (TextView) findViewById(R.id.confirm_txt);

        btn_camera_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(shutterCallback);
            }
        });

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        txt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        btn_resume.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mCamera.startPreview();

                intoCameraView();
            }
        });

        txt_resume.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mCamera.startPreview();

                intoCameraView();
            }
        });

        txt_confirm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //保存图片
                saveImageToFile();

                mCamera.startPreview();
                intoCameraView();
            }
        });

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        mCamera.release();
        mCamera = null;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (mCamera == null){
            mCamera = getCameraInstance();
        }
        //必须放在onResume中，不然会出现Home键之后，再回到该APP，黑屏
        mySurfaceView = new MySurfaceView(getApplicationContext(), mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mySurfaceView);

        mySurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mCamera.autoFocus(touchCallback);
                return false;
            }
        });
    }

    /*得到一相机对象*/
    private Camera getCameraInstance(){
        Camera camera = null;
        try{
            camera = camera.open();
        }catch(Exception e){
            e.printStackTrace();
        }
        return camera;
    }

    // -----------------------自定义相机界面布局修改---------------------------------------

    /**
     * 进入拍摄界面
     */
    private void intoCameraView() {
        findViewById(R.id.camera_option_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.reference_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.resume_layout).setVisibility(View.GONE);
        findViewById(R.id.resume_bottom_layout).setVisibility(View.GONE);
        findViewById(R.id.confirm_txt).setVisibility(View.GONE);
    }

    /**
     * 进入选取图片界面
     */
    private void intoChooseView() {
        findViewById(R.id.camera_option_layout).setVisibility(View.GONE);
        findViewById(R.id.reference_layout).setVisibility(View.GONE);
        findViewById(R.id.resume_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.resume_bottom_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.confirm_txt).setVisibility(View.VISIBLE);
    }



    // -----------------------保存图片---------------------------------------
    private void saveImageToFile(){
        File file = getOutFile(TYPE_FILE_IMAGE);

        if (file == null){
            Toast.makeText(getApplicationContext(), "文件创建失败,请检查SD卡读写权限", Toast.LENGTH_SHORT).show();
            return ;
        }
        Log.d("MyPicture", "自定义相机图片路径:" + file.getPath());
        if (buffer == null){
            Log.d("MyPicture", "自定义相机Buffer: null");
        }else{
            try{
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buffer);
                fos.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        idCardRecog = new IDCardRecog(apixKey, 100);
        idCardRecog.recogFront(file.getPath());

    }

    //-----------------------生成Uri---------------------------------------
    //得到输出文件的URI
    private Uri getOutFileUri(int fileType) {
        return Uri.fromFile(getOutFile(fileType));
    }

    //生成输出文件
    private File getOutFile(int fileType) {

        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_REMOVED.equals(storageState)){
            Toast.makeText(getApplicationContext(), "oh,no, SD卡不存在", Toast.LENGTH_SHORT).show();
            return null;
        }

        File mediaStorageDir = new File (Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                , SAVE_PIC_DIR);
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("MyPictures", "创建图片存储路径目录失败");
                Log.d("MyPictures", "mediaStorageDir : " + mediaStorageDir.getPath());
                return null;
            }
        }

        File file = new File(getFilePath(mediaStorageDir,fileType));

        return file;
    }
    //生成输出文件路径
    private String getFilePath(File mediaStorageDir, int fileType){
        String timeStamp =new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String filePath = mediaStorageDir.getPath() + File.separator;
        if (fileType == TYPE_FILE_IMAGE){
            filePath += ("IMG_" + timeStamp + ".jpg");
        }else if (fileType == TYPE_FILE_VEDIO){
            filePath += ("VIDEO_" + timeStamp + ".mp4");
        }else{
            return null;
        }
        return filePath;
    }

    private Camera.AutoFocusCallback shutterCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // success 表示定焦成功
            if (success) {
                mCamera.setOneShotPreviewCallback(null);

                mCamera.takePicture(null, null, pictureCallback);

                intoChooseView();
            } else {
                log("对焦失败了");
            }
        }
    };

    private Camera.AutoFocusCallback touchCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // success 表示定焦成功
            if (success) {
                mCamera.setOneShotPreviewCallback(null);
            } else {
                log("对焦失败了");
            }
        }
    };

     /**
     * Ocr的订阅事件
     */
    public void onEventMainThread(OcrEvent event) {
        JSONObject json = null;
        try {
            json = new JSONObject(event.msg);
            if (json != null) {
                if (json.has("name")){
                    String name = json.getString("name");
                    String sex = json.getString("sex");
                    String nation = json.getString("nation");
                    String birth = json.getString("birth");
                    String address = json.getString("address");
                    String number = json.getString("number");
                    toast(name + sex + nation + birth + address + number);

                    log(name + sex + nation + birth + address + number);

                } else {
                    Toast.makeText(this, "该照片无法识别！", Toast.LENGTH_LONG).show();
                    log("该照片无法识别！");
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "该照片无法识别！", Toast.LENGTH_LONG).show();
            log("该照片无法识别！");
            e.printStackTrace();
        }
    }
}
