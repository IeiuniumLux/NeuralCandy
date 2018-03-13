package io.androidthings.neuralcandy;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;


public class CandyMachine implements AutoCloseable {

    private static final String TAG = CandyMachine.class.getSimpleName();

    private Gpio mMotorGpio;

    public CandyMachine(String gpio) {
        PeripheralManager pioService = PeripheralManager.getInstance();
        this.mMotorGpio = createGpio(pioService, gpio);
    }

    @Nullable
    private Gpio createGpio(PeripheralManager pioService, String pinName) {
        try {
            Gpio gpio = pioService.openGpio(pinName);
            gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW); // Configure as an output
            gpio.setActiveType(Gpio.ACTIVE_HIGH);
            return gpio;
        } catch (IOException e) {
            Log.e(TAG, "Error initializing GPIO: " + pinName, e);
        }
        return null;
    }

    public void giveCandies(boolean value) {
        setGpioValue(mMotorGpio, value);
    }

    private void closeGpio(Gpio gpio) {
        if (gpio != null) {
            try {
                gpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing GPIO", e);
            }
        }
    }


    private void setGpioValue(Gpio gpio, boolean value) {
        if (gpio != null) {
            try {
                gpio.setValue(value);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void close() throws Exception {
        closeGpio(mMotorGpio);
        mMotorGpio = null;
    }
}
