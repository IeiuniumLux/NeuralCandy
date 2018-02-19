package io.androidthings.neuralcandy;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.things.pio.PeripheralManagerService;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

    /** Camera image capture size */
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    /** Image dimensions required by TF model */
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    /** Dimensions of model inputs. */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    /** TF model asset files */
    private static final String LABELS_FILE = "labels.txt";
    private static final String MODEL_FILE = "mobilenet_quant_v1_224.tflite";

    private AnimationDrawable mAnimation;
    private ImageView mImgView;
    private TextView mTextInfo;
    private boolean mProcessing;
    private Interpreter mTensorFlowLite;
    private List<String> mLabels;
    private CameraHandler mCameraHandler;
    private ImagePreprocessor mImagePreprocessor;
    private String mCurrentTarget;

    public static final List<String> TARGETS = Arrays.asList("TRILOBITE","AXOLOTL","TRICERATOPS",
            "GOLDFISH","PORCUPINE","ZEBRA","STARFISH","PLATYPUS","TOUCAN","TERRAPIN","CRAB","DAISY");

//    public static final List<String> TARGETS = Arrays.asList("LION","CAT","DOG","BEE","ZEBRA");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImgView = (ImageView) findViewById(R.id.image);
        mTextInfo = (TextView) findViewById(R.id.textInfo);

        final CountDownTimer timer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {

            }
            public void onFinish() {
                mImgView.setImageResource(R.drawable.ic_start);
                updateStatus(getString(R.string.start_message));
            }
        };

        mImgView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final ImageView imgView = (ImageView) view;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_start).getConstantState())) {
                        mCurrentTarget = randomAnimal();
                        imgView.setImageResource(R.drawable.ic_camera);
                        updateStatus(getString(R.string.request, mCurrentTarget));
                        timer.start();
                    } else if (imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_camera).getConstantState())) {
                        if (timer != null)
                            timer.cancel();
                        takePhoto();
                        timer.start();
//                        imgView.setImageResource(R.drawable.animation);
//                        mAnimation = (AnimationDrawable) imgView.getDrawable();
//                        mAnimation.start();
//                        new CountDownTimer(5000, 1000) {
//
//                            public void onTick(long millisUntilFinished) {
//                                mTextInfo.setText("Analyzing... " + millisUntilFinished / 1000);
//                            }
//
//                            public void onFinish() {
//                                mAnimation.stop();
//                                imgView.setImageResource(R.drawable.ic_thumb_up);
//                                mTextInfo.setText("done!");
//                            }
//                        }.start();
                    } else {
                        imgView.setImageResource(R.drawable.ic_start);
                        updateStatus(getString(R.string.start_message));
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

        updateStatus(getString(R.string.initializing));
        initCamera();
        initClassifier();
//        initButton();
        updateStatus(getString(R.string.start_message));
    }

    private String randomAnimal(){
        String newLabel;
        Random random = new Random();
        do {
            int idx = random.nextInt(TARGETS.size());
            newLabel = TARGETS.get(idx);
        } while (newLabel == mCurrentTarget);

        return newLabel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyClassifier();
        } catch (Throwable t) {
            // close quietly
        }
        try {
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
    }

    private void updateStatus(String status) {
        Log.d(TAG, status);
        mTextInfo.setText(status);
    }

    /**
     * Initialize the classifier that will be used to process images.
     */
    private void initClassifier() {
        try {
            mTensorFlowLite = new Interpreter(TensorFlowHelper.loadModelFile(this, MODEL_FILE));
            mLabels = TensorFlowHelper.readLabels(this, LABELS_FILE);
        } catch (IOException e) {
            Log.w(TAG, "Unable to initialize TensorFlow Lite.", e);
        }
    }

    /**
     * Clean up the resources used by the classifier.
     */
    private void destroyClassifier() {
        mTensorFlowLite.close();
    }

    /**
     * Image capture process complete
     */
    private void onPhotoReady(Bitmap bitmap) {
        mImgView.setImageBitmap(bitmap);
        doRecognize(bitmap);
    }

    /**
     * Image classification process complete
     */
    private void onPhotoRecognitionReady(Collection<Recognition> results) {
        updateStatus(formatResults(results));
        mProcessing = false;
    }

    /**
     * Format results list for display
     */
    private String formatResults(Collection<Recognition> results) {
        if (results == null || results.isEmpty()) {
            return getString(R.string.empty_result);
        } else {
            StringBuilder sb = new StringBuilder();
            Iterator<Recognition> it = results.iterator();
            int counter = 0;
            while (it.hasNext()) {
                Recognition r = it.next();
                if (r.getTitle().equalsIgnoreCase(mCurrentTarget)) {
                    mImgView.setImageResource(R.drawable.ic_thumb_up);
                    return "Won!";
                }
                sb.append(r.getTitle());
                counter++;
                if (counter < results.size() - 1) {
                    sb.append(", ");
                } else if (counter == results.size() - 1) {
                    sb.append(" or ");
                }
            }
            mImgView.setImageResource(R.drawable.ic_thumb_down);
            return sb.toString();
        }
    }

    /**
     * Process an image and identify what is in it. When done, the method
     * {@link #onPhotoRecognitionReady(Collection)} must be called with the results of
     * the image recognition process.
     *
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    private void doRecognize(Bitmap image) {
        // Allocate space for the inference results
        byte[][] confidencePerLabel = new byte[1][mLabels.size()];
        // Allocate buffer for image pixels.
        int[] intValues = new int[TF_INPUT_IMAGE_WIDTH * TF_INPUT_IMAGE_HEIGHT];
        ByteBuffer imgData = ByteBuffer.allocateDirect(
                DIM_BATCH_SIZE * TF_INPUT_IMAGE_WIDTH * TF_INPUT_IMAGE_HEIGHT * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        // Read image data into buffer formatted for the TensorFlow model
        TensorFlowHelper.convertBitmapToByteBuffer(image, intValues, imgData);

        // Run inference on the network with the image bytes in imgData as input,
        // storing results on the confidencePerLabel array.
        mTensorFlowLite.run(imgData, confidencePerLabel);

        // Get the results with the highest confidence and map them to their labels
        Collection<Recognition> results = TensorFlowHelper.getBestResults(confidencePerLabel, mLabels);
        // Report the results with the highest confidence
        onPhotoRecognitionReady(results);
    }

    /**
     * Initialize the camera that will be used to capture images.
     */
    private void initCamera() {
        mImagePreprocessor = new ImagePreprocessor(PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT,
                TF_INPUT_IMAGE_WIDTH, TF_INPUT_IMAGE_HEIGHT);
        mCameraHandler = CameraHandler.getInstance();
        mCameraHandler.initializeCamera(this,
                PREVIEW_IMAGE_WIDTH, PREVIEW_IMAGE_HEIGHT, null,
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Bitmap bitmap = mImagePreprocessor.preprocessImage(imageReader.acquireNextImage());
                        onPhotoReady(bitmap);
                    }
                });
    }

    /**
     * Clean up resources used by the camera.
     */
    private void closeCamera() {
        mCameraHandler.shutDown();
    }

    /**
     * Load the image that will be used in the classification process.
     * When done, the method {@link #onPhotoReady(Bitmap)} must be called with the image.
     */
    private void takePhoto() {
        mCameraHandler.takePicture();
    }
}
