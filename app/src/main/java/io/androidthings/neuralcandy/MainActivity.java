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

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final List<String> THINGS = Arrays.asList("TRILOBITE", "AXOLOTL", "TRICERATOPS",
            "GOLDFISH", "PORCUPINE", "HOURGLASS", "ZEBRA", "STARFISH", "PLATYPUS", "TOUCAN", "TERRAPIN",
            "CHITON", "DAISY", "BULLFROG", "AGAMA");

    /**
     * Camera image capture size
     */
    private static final int PREVIEW_IMAGE_WIDTH = 640;
    private static final int PREVIEW_IMAGE_HEIGHT = 480;
    /**
     * Image dimensions required by TF model
     */
    private static final int TF_INPUT_IMAGE_WIDTH = 224;
    private static final int TF_INPUT_IMAGE_HEIGHT = 224;
    /**
     * Dimensions of model inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    /**
     * TF model asset files
     */
    private static final String LABELS_FILE = "labels.txt";
    private static final String MODEL_FILE = "mobilenet_quant_v1_224.tflite";
    private static final String MOTOR_1_NEG = "BCM20";
    private AnimationDrawable mAnimation;
    private ImageView mImgView;
    private TextView mTextInfo;
    private boolean mProcessing;
    private Interpreter mTensorFlowLite;
    private List<String> mLabels;
    private CameraHandler mCameraHandler;
    private ImagePreprocessor mImagePreprocessor;
    private String mCurrentTarget;
    private CandyMachine mCandyMachine;

    private static int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCandyMachine = new CandyMachine(MOTOR_1_NEG);
        mImgView = (ImageView) findViewById(R.id.image);
        mTextInfo = (TextView) findViewById(R.id.textInfo);

        final CountDownTimer timer = new CountDownTimer(45000, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                mImgView.setImageResource(R.drawable.ic_start);
                updateStatus(getString(R.string.start_message));
                mProcessing = false;
            }
        };

        mImgView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final ImageView imgView = (ImageView) view;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if ((imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_start).getConstantState())) || (imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_sorry).getConstantState()))) {
                        mCurrentTarget = randomThing();
                        imgView.setImageResource(R.drawable.ic_camera);
                        updateStatus(getString(R.string.request, mCurrentTarget));
                        timer.start();
                    } else if (imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_camera).getConstantState())) {
                        if (timer != null)
                            timer.cancel();

                        takePhoto();
                        imgView.setImageResource(R.drawable.animation);
                        mAnimation = (AnimationDrawable) imgView.getDrawable();
                        mAnimation.start();
                        updateStatus(getString(R.string.processing));
                        mProcessing = true;
                        timer.start();
                    } else if (imgView.getDrawable().getConstantState().equals
                            (imgView.getContext().getDrawable(R.drawable.ic_thumb_up).getConstantState())) {
                            timer.cancel();
                            mCandyMachine.giveCandies(true);
                            new CountDownTimer(1000, 1000) {
                                public void onTick(long millisUntilFinished) {
//                                    mTextInfo.setText("Candy countdown: " + millisUntilFinished / 1000);
                                }

                                public void onFinish() {
                                    mImgView.setImageResource(R.drawable.ic_start);
                                    updateStatus(getString(R.string.start_message));
                                    mProcessing = false;
                                    mCandyMachine.giveCandies(false);
                                }
                            }.start();
                    }
                }
                return true;
            }
        });

        updateStatus(getString(R.string.initializing));
        initCamera();
        initClassifier();
        updateStatus(getString(R.string.start_message));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            mCandyMachine.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing GPIO", e);
        }

        try {
            destroyClassifier();
            closeCamera();
        } catch (Throwable t) {
            // close quietly
        }
    }

    private String randomThing() {
//        String newLabel;
//        Random random = new Random();
//        do {
//            int idx = random.nextInt(THINGS.size());
//            newLabel = THINGS.get(idx);
//        } while (newLabel == mCurrentTarget);

        String newLabel = THINGS.get(index);
        index = (index < THINGS.size()) ? index + 1 : 0;

        return newLabel;
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
            Log.e(TAG, "Unable to initialize TensorFlow Lite.", e);
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
    private void onPhotoCaptured(final Bitmap bitmap) {
        new CountDownTimer(2000, 1000) {
            public void onTick(long millisUntilFinished) {
                mImgView.setImageBitmap(bitmap);
            }

            public void onFinish() {
                doIdentification(bitmap);
            }
        }.start();
    }

    /**
     * Image classification process complete
     */
    private void onClassificationComplete(Collection<Recognition> results) {
        if (results == null || results.isEmpty()) {
            updateStatus(getString(R.string.empty_result));
        } else {
            Iterator<Recognition> it = results.iterator();
            while (it.hasNext()) {
                Recognition r = it.next();
                if (r.getTitle().toLowerCase().contains(mCurrentTarget.toLowerCase())) {
                    mImgView.setImageResource(R.drawable.ic_thumb_up);
                    updateStatus("It matches!  Tap to claim your candy.");
                    return;
                }
            }
            mImgView.setImageResource(R.drawable.ic_sorry);
            updateStatus(getString(R.string.empty_result));
            mProcessing = false;
        }
    }

    /**
     * Process an image and identify what is in it. When done, the method
     * {@link #onClassificationComplete(Collection)} must be called with the results of
     * the image recognition process.
     *
     * @param image Bitmap containing the image to be classified. The image can be
     *              of any size, but preprocessing might occur to resize it to the
     *              format expected by the classification process, which can be time
     *              and power consuming.
     */
    private void doIdentification(Bitmap image) {
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
        onClassificationComplete(results);
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
                        onPhotoCaptured(bitmap);
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
     * When done, the method {@link #onPhotoCaptured(Bitmap)} must be called with the image.
     */
    private void takePhoto() {
        mCameraHandler.takePicture();
    }
}
