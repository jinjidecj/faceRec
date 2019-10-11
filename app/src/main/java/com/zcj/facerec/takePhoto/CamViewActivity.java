package com.zcj.facerec.takePhoto;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Camera;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.zcj.facerec.FaceDetect;
import com.zcj.facerec.LogUtil;
import com.zcj.facerec.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CamViewActivity extends AppCompatActivity {
    private CameraView cameraView;
    private TextView txResult;
    private Timer timerCamRec;
    private final int camRecTime=3000;//每隔几ms进行一次人脸识别
    private final int camRecStart = 3000;//程序启动后延时几秒启动人脸识别
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_view);
        cameraView = findViewById(R.id.camera);
        txResult = findViewById(R.id.textViewResult);

        cameraView.addCameraListener(new CameraListener() {

            @Override
            public void onPictureTaken(byte[] picture1) {
                final byte[]picture = picture1;
                // Create a bitmap or a file...
                // CameraUtils will read EXIF orientation for you, in a worker thread.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.d("get pic"+picture);
                        String galleryPath= Environment.getExternalStorageDirectory()
                                + File.separator + Environment.DIRECTORY_DCIM
                                +File.separator+"FaceRec"+File.separator;
                        File filePath  = new File(galleryPath);
                        if (!filePath.exists()) {
                            /** 注意这里是 mkdirs()方法 可以创建多个文件夹 */
                            filePath.mkdirs();
                        }
                        Date date = new Date();
                        File savedPhoto = new File(galleryPath, "photo"+date.toString()+".jpg");
                        try {
                            FileOutputStream outputStream = new FileOutputStream(savedPhoto.getPath());
                            outputStream.write(picture);
                            outputStream.close();
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                        // rec(picture);
                        new MyThread(picture,handler).start();
                    }
                }).start();

            }
        });
        //cameraView.setFacing(Facing.FRONT);

        //Timer timer = new Timer();
        //timer.schedule(task, 3000, 3000); // 1s后执行task,经过1s再次执行
        Button btn_text = findViewById(R.id.btn_test2);
        btn_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.capturePicture();
            }
        });

        //显示或关闭拍照预览串口
        Switch swShow = findViewById(R.id.sw_cam_show);
        swShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    LogUtil.d("check true");
                    ViewGroup.LayoutParams linearParams = cameraView.getLayoutParams();
                    linearParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    linearParams.width =ViewGroup.LayoutParams.WRAP_CONTENT;
                    cameraView.setLayoutParams(linearParams);
                }else{
                    LogUtil.d("check false");
                    ViewGroup.LayoutParams linearParams = cameraView.getLayoutParams();
                    linearParams.height = 1;
                    linearParams.width = 1;
                    cameraView.setLayoutParams(linearParams);
                }
            }
        });
        //打开或关闭自动人脸识别
        Switch swCamStopStart = findViewById(R.id.sw_cam_stop_start);
        swCamStopStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    LogUtil.d("check true");
                    openCamRec();
                }else{
                    LogUtil.d("check false");
                    closeCamRec();
                }
            }
        });

    }
    private void openCamRec(){
        timerCamRec = new Timer();
        timerCamRec.schedule(task, camRecStart, camRecTime); // 1s后执行task,经过1s再次执行
    }
    private void closeCamRec(){
        if(timerCamRec!=null)
            timerCamRec.cancel();
    }


    private void rec(final byte[]bt){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result  = FaceDetect.detect(bt);//人脸检测
                try {
                    JSONObject root = new JSONObject(result);
                    int errorCode = root.getInt("error_code");
                    if(errorCode==0){
                        JSONObject resultJson = root.getJSONObject("result");
                        int face_num = resultJson.getInt("face_num");
                        int i = 0;
                        JSONArray faceList = resultJson.getJSONArray("face_list");
                        String addResult = "";
                        while (face_num>0){
                            JSONObject resultOneJson = faceList.getJSONObject(i);
                            int age = resultOneJson.getInt("age");
                            JSONObject genderJson = resultOneJson.getJSONObject("gender");
                            String type = genderJson.getString("type");
                            double probability = genderJson.getDouble("probability");
                            if(type.equals("male")&&probability>0.0){
                                addResult=addResult+"性别：男，年龄："+age+"，可靠性："+probability*100+"%\n";
                            }else if(type.equals("female")&&probability>0.0){
                                addResult=addResult+"性别：女，年龄："+age+"，可靠性："+probability*100+"%\n";
                            }
                            i++;
                            face_num--;
                        }
                        txResult.append(" "+addResult);
                    }else{
                        txResult.append(" "+"识别失败");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            cameraView.capturePicture();
        }
    };
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.what==1){
                txResult.append(" "+(String)msg.obj);
                LogUtil.d("ddd "+(String)msg.obj);
            }else if(msg.what==2){
                txResult.append(" "+(String)msg.obj);
                LogUtil.d("ddd "+(String)msg.obj);
            }

            super.handleMessage(msg);
        }
    };
    private static class MyThread extends Thread {

        private byte[] bt;
        private Handler handler;
        public MyThread(byte[] bt) {
            this.bt = bt;
        }
        public MyThread(byte[] bt,Handler ha) {
            this.bt = bt;
            handler=ha;
        }

        @Override
        public void run() {

            String result  = FaceDetect.detect(bt);//人脸检测
            try {
                JSONObject root = new JSONObject(result);
                int errorCode = root.getInt("error_code");
                if(errorCode==0){
                    Message message = new Message();
                    message.what=2;
                    JSONObject resultJson = root.getJSONObject("result");
                    int face_num = resultJson.getInt("face_num");
                    int i = 0;
                    JSONArray faceList = resultJson.getJSONArray("face_list");
                    String addResult = "";
                    while (face_num>0){
                        JSONObject resultOneJson = faceList.getJSONObject(i);
                        int age = resultOneJson.getInt("age");
                        JSONObject genderJson = resultOneJson.getJSONObject("gender");
                        String type = genderJson.getString("type");
                        double probability = genderJson.getDouble("probability");
                        if(type.equals("male")&&probability>0.0){
                            addResult=addResult+"性别：男，年龄："+age+"，可靠性："+probability*100+"%\n";
                        }else if(type.equals("female")&&probability>0.0){
                            addResult=addResult+"性别：女，年龄："+age+"，可靠性："+probability*100+"%\n";
                        }
                        i++;
                        face_num--;
                    }
                    message.obj = addResult;
                    handler.sendMessage(message);
                }else{
                    Message message = new Message();
                    message.what=1;
                    message.obj="识别失败";
                    handler.sendMessage(message);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }
}
