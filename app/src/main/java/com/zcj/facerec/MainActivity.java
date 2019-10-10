package com.zcj.facerec;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zcj.facerec.takePhoto.CamActivity;
import com.zcj.facerec.takePhoto.CamViewActivity;
import com.zcj.facerec.takePhoto.MyService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private final int PICK_IMAGE = 1;
    private MyThread mMyThread;
    private AlertDialog dialog;
    private TextView txResult;
    // 要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.PACKAGE_USAGE_STATS,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.WAKE_LOCK,
            Manifest.permission.DISABLE_KEYGUARD, Manifest.permission.REBOOT};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txResult = findViewById(R.id.txResult);
        Button btnTakePhoto = findViewById(R.id.button2);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent face = new Intent(MainActivity.this, CamActivity.class);
                startActivity(face);
                //Toast.makeText(getApplicationContext(),"开启人脸",Toast.LENGTH_SHORT).show();
            }
        });
        Button btnCam= findViewById(R.id.button3);
        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cam = new Intent(MainActivity.this, CameraKitActivity.class);
                startActivity(cam);
                //Toast.makeText(getApplicationContext(),"开启人脸",Toast.LENGTH_SHORT).show();
            }
        });
        Button btnCam3 = findViewById(R.id.button4);
        btnCam3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cam = new Intent(MainActivity.this, CamViewActivity.class);
                startActivity(cam);
            }
        });
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 检查该权限是否已经获取
            int isGranted = ContextCompat.checkSelfPermission(this, permissions[0]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (isGranted != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                showDialogTipUserRequestPermission();
            }

        }

    }

    public void onClicked(View view) {
        /**
         * 此处为相册图片代码
         */
     /*   Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);*/
        /**
         * 此处为拍照图片代码
         */
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, PICK_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * 此处为相册图片代码
         */
      /*  if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData
                () != null) {
            Uri uri = data.getData();
            try {
                //将图片显示到ImageView
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);

                //压缩图片并将bitmap转为byte[]
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                byte[] bt = bos.toByteArray();
                mMyThread=new MyThread(bt);
                mMyThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        /**
         * 此处为拍照图片代码
         */
         if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data
                 .getExtras() != null) {
             Bundle bundle = data.getExtras();
             Bitmap bitmap = (Bitmap) bundle.get("data");
             ImageView imageView = findViewById(R.id.imageView1);
             imageView.setImageBitmap(bitmap);
             //压缩图片并将bitmap转为byte[]
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
             byte[] bt = bos.toByteArray();
             mMyThread = new MyThread(bt,handler);
             mMyThread.start();
         }
    }
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

    // 提示用户该请求权限的弹出框
    private void showDialogTipUserRequestPermission() {

        new AlertDialog.Builder(this)
                .setTitle("相关权限不可用")
                .setMessage("由于手机拯救者需要获取相关权限 \n否则，您将无法正常使用")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }
    // 开始提交请求权限
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }
    // 用户权限 申请 的回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        // showDialogTipUserGoToAppSettting();
                    } else
                        finish();
                } else {
                    Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


}
