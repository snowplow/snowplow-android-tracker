package com.snowplowanalytics.snowplowtrackerdemo;

import android.os.Bundle;
import android.app.Activity;
import android.widget.Button;
import android.view.View;
import android.content.Intent;

public class MainActivity extends Activity {

    Button _rxDemoBtn;
    Button _rxLiteBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _rxDemoBtn = ( Button ) findViewById(R.id.btn_rx);
        _rxLiteBtn = ( Button ) findViewById(R.id.btn_lite);

        _rxDemoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RxDemo.class);
                startActivity(intent);
            }
        });

        _rxLiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LiteDemo.class);
                startActivity(intent);
            }
        });
    }
}
