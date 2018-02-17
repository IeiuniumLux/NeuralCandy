package io.androidthings.neuralcandy;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class PeripheralManagerService.
 * For example, the snippet below will open a GPIO pin and set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    AnimationDrawable processingAnimation;
    TextView textInfo;

    public static final List<String> LABELS = Arrays.asList("DOG",
            "CAT","FISH","SHEEP","COW","BEE","LION","PENGUIN","BIRD",
            "RABBIT","ELEPHANT","FLOWER");

    private Map<String, Gpio> mGpioMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imgView = (ImageView) findViewById(R.id.image);
        textInfo = (TextView) findViewById(R.id.textInfo);

        imgView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final ImageView img = (ImageView) view;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (img.getDrawable().getConstantState().equals
                            (img.getContext().getDrawable(R.drawable.ic_start).getConstantState())) {
                        img.setImageResource(R.drawable.ic_camera);
                        textInfo.setText("Processing...");
                    } else if (img.getDrawable().getConstantState().equals
                            (img.getContext().getDrawable(R.drawable.ic_camera).getConstantState())) {
                        img.setImageResource(R.drawable.animation);
                        processingAnimation = (AnimationDrawable) img.getDrawable();
                        processingAnimation.start();
                        new CountDownTimer(5000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                textInfo.setText("Analyzing... " + millisUntilFinished / 1000);
                            }

                            public void onFinish() {
                                processingAnimation.stop();
                                img.setImageResource(R.drawable.ic_thumb_up);
                                textInfo.setText("done!");
                            }
                        }.start();
                    }
                }
                return true;
            }
        });

        PeripheralManagerService pioService = new PeripheralManagerService();

//        for (String name : pioService.getGpioList()) {
//            View child = inflater.inflate(R.layout.list_item_gpio, gpioPinsView, false);
//            Switch button = (Switch) child.findViewById(R.id.gpio_switch);
//            button.setText(name);
//            gpioPinsView.addView(button);
//            Log.d(TAG, "Added button for GPIO: " + name);
//
//            try {
//                final Gpio ledPin = pioService.openGpio(name);
//                ledPin.setEdgeTriggerType(Gpio.EDGE_NONE);
//                ledPin.setActiveType(Gpio.ACTIVE_HIGH);
//                ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//
//                button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                    @Override
//                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                        try {
//                            ledPin.setValue(isChecked);
//                        } catch (IOException e) {
//                            Log.e(TAG, "error toggling gpio:", e);
//                            buttonView.setOnCheckedChangeListener(null);
//                            // reset button to previous state.
//                            buttonView.setChecked(!isChecked);
//                            buttonView.setOnCheckedChangeListener(this);
//                        }
//                    }
//                });
//
//                mGpioMap.put(name, ledPin);
//            } catch (IOException e) {
//                Log.e(TAG, "Error initializing GPIO: " + name, e);
//                // disable button
//                button.setEnabled(false);
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (Map.Entry<String, Gpio> entry : mGpioMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing GPIO " + entry.getKey(), e);
            }
        }
        mGpioMap.clear();
    }
}
