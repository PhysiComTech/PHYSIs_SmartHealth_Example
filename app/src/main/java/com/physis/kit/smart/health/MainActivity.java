package com.physis.kit.smart.health;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.physicomtech.kit.physislibrary.PHYSIsBLEActivity;
import com.physicomtech.kit.physislibrary.ble.BluetoothLEManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends PHYSIsBLEActivity {

    // region Check Bluetooth Permission
    private static final int REQ_APP_PERMISSION = 1000;
    private static final List<String> appPermissions
            = Collections.singletonList(Manifest.permission.ACCESS_COARSE_LOCATION);

    /*
        # 애플리케이션의 정상 동작을 위한 권한 체크
        - 안드로이드 마시멜로우 버전 이상에서는 일부 권한에 대한 사용자의 허용이 필요
        - 권한을 허용하지 않을 경우, 관련 기능의 정상 동작을 보장하지 않음.
        - 권한 정보 URL : https://developer.android.com/guide/topics/security/permissions?hl=ko
        - PHYSIs Maker Kit에서는 블루투스 사용을 위한 위치 권한이 필요.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> reqPermissions = new ArrayList<>();
            for(String permission : appPermissions){
                if(checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED){
                    reqPermissions.add(permission);
                }
            }
            if(reqPermissions.size() != 0){
                requestPermissions(reqPermissions.toArray(new String[reqPermissions.size()]), REQ_APP_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQ_APP_PERMISSION){
            boolean accessStatus = true;
            for(int grantResult : grantResults){
                if(grantResult == PackageManager.PERMISSION_DENIED)
                    accessStatus = false;
            }
            if(!accessStatus){
                Toast.makeText(getApplicationContext(), "위치 권한 거부로 인해 애플리케이션을 종료합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    // endregion

    private final String SERIAL_NUMBER = "20C38F8E85AA";       // PHYSIs Maker Kit 시리얼번호

    private static final String MSG_STX = "$";               // 메시지 프로토콜 STX/ETX
    private static final String MSG_ETX = "#";

    Button btnConnect, btnDisconnect;                       // 액티비티 위젯
    ProgressBar pgbConnect;
    TextView tvHeartbeatValue, tvTemperatureValue, tvWeightValue;

    private boolean isConnected = false;             // BLE 연결 상태 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!BluetoothLEManager.getInstance(getApplicationContext()).getEnable()){
            Toast.makeText(getApplicationContext(), "블루투스 활성화 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
        }

        checkPermissions();                 // 앱 권한 체크 함수 호출
        initWidget();                       // 위젯 생성 및 초기화 함수 호출
        setEventListener();                 // 이벤트 리스너 설정 함수 호출
    }

    /*
      # 위젯 객체 생성 및 초기화
   */
    private void initWidget() {
        btnConnect = findViewById(R.id.btn_connect);                // 버튼 생성
        btnDisconnect = findViewById(R.id.btn_disconnect);

        pgbConnect = findViewById(R.id.pgb_connect);                // 프로그래스 생성

        tvHeartbeatValue = findViewById(R.id.tv_heartbeat_value);
        tvTemperatureValue = findViewById(R.id.tv_temperature_value);
        tvWeightValue = findViewById(R.id.tv_weight_value);
    }

    /*
     # 뷰 (버튼) 이벤트 리스너 설정
  */
    private void setEventListener() {
        btnConnect.setOnClickListener(new View.OnClickListener() {              // 연결 버튼
            @Override
            public void onClick(View v) {                   // 버튼 클릭 시 호출
                btnConnect.setEnabled(false);                       // 연결 버튼 비활성화 설정
                pgbConnect.setVisibility(View.VISIBLE);             // 연결 프로그래스 가시화 설정
                connectDevice(SERIAL_NUMBER);                       // PHYSIs Maker Kit BLE 연결 시도
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {           // 연결 종료 버튼
            @Override
            public void onClick(View v) {
                disconnectDevice();                                  // PHYSIs Maker Kit BLE 연결 종료
            }
        });
    }

    /*
    # BLE 연결 결과 수신
    - 블루투스 연결에 따른 결과를 전달받을 때 호출 (BLE 연결 상태가 변경됐을 경우)
    - 연결 결과 : CONNECTED(연결 성공), DISCONNECTED(연결 종료/실패), NO_DISCOVERY(디바이스 X)
  */
    @Override
    protected void onBLEConnectedStatus(int result) {
        super.onBLEConnectedStatus(result);
        setConnectedResult(result);                             // BLE 연결 결과 처리 함수 호출
    }

    /*
        # BLE 연결 결과 처리
     */
    private void setConnectedResult(int result){
        pgbConnect.setVisibility(View.INVISIBLE);               // 연결 프로그래스 비가시화 설정
        isConnected = result == CONNECTED;                      // 연결 결과 확인

        String toastMsg;                                        // 연결 결과에 따른 Toast 메시지 출력
        if(result == CONNECTED){
            toastMsg = "Physi Kit와 연결되었습니다.";
        }else if(result == DISCONNECTED){
            toastMsg = "Physi Kit 연결이 실패/종료되었습니다.";
        }else{
            toastMsg = "연결할 Physi Kit가 존재하지 않습니다.";
        }
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();

        btnConnect.setEnabled(!isConnected);                     // 연결 버튼 활성화 상태 설정
        btnDisconnect.setEnabled(isConnected);
    }

    /*
         # BLE 메시지(데이터) 수신 콜백 함수
        - BLE 메시지가 수신될 경우 호출되는 함수
        - 측정 신체 정보 메시지를 수신 ( Data Format : $ 심박수, 체온값, 체중값 # )
        - 구분자(,)를 기준으로 측정 신체 정보 구분 및 텍스트 출력
       */
    @Override
    protected void onBLEReceiveMsg(String msg) {
        if(msg.startsWith(MSG_STX) && msg.endsWith(MSG_ETX))    // 수신 메시지의 STX(시작 문자), ETX(종료 문자) 확인
        {
            msg = msg.substring(1, msg.length() - 1);           // STX(시작 문자) / ETX(종료 문자) 제거
            String[] data = msg.split(",");               // 구분자 기준 문자열 자르기
            if(data.length == 3){
                tvHeartbeatValue.setText(data[0]);              // 측정 결과값 출력
                tvTemperatureValue.setText(data[1]);
                tvWeightValue.setText(data[2]);
            }
        }
    }
}