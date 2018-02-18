# NeuralCandy

The NeuralCandy project combines image classifier and sugar highs in one delicious [Android Things](https://developer.android.com/things/get-started/index.html) project. The application asks for a random animal image (e.g. a platypus or a macaque) to be placed in front of the [camera module](https://www.raspberrypi.org/products/camera-module-v2/) and if it matches the request; then the motor of the candy dispenser is activated to releases the delicious reward.

NeuralCandy uses the  [TensorFlow Lite](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/contrib/lite#tensorflow-lite) inference library for Android to locally classify the captured image against a pre-trained [ImageNet](http://image-net.org/) model; and the [Raspberry Pi 3 model B](https://developer.android.com/things/hardware/raspberrypi.html) is used to handles both the image processing and the motor for the candy release.

> Note that the Android Things project is still in the developer preview stage for early adopters to use for testing. NeuralCandy uses the [preview 6.1 release](https://developer.android.com/things/preview/releases.html#preview-6-1); which may still have some stability issues.  One of them is that you have to reboot the Raspberry Pi after installing the app because the camera permission requested by the app is not granted until the next device reboot.

## What you'll need

- [Android Studio 3.0+](https://developer.android.com/studio/index.html)
- The following individual components:
  - [Raspberry Pi 3 Model B](https://www.adafruit.com/product/3055)
  - [Raspberry Pi Camera Board v2](https://www.adafruit.com/product/3099)
  - [5V 2.4A Power Supply](https://www.adafruit.com/product/1995)
  - [USB to TTL Serial Cable](https://www.adafruit.com/product/954)
  - [Lens Adjustment Tool](https://www.adafruit.com/product/3518) (optional)
  - [Pimoroni Explorer pHAT](https://www.adafruit.com/product/3018)
  - [7" Touchscreen Display](https://www.adafruit.com/product/2718)
  - [SmartiPi Case](https://www.adafruit.com/product/3576)
  - [16GB microSD card](https://www.amazon.com/gp/product/B010Q57SEE/ref=ox_sc_act_title_2?smid=ATVPDKIKX0DER&psc=1)
  - [Mini Candy Dispenser](https://www.amazon.com/gp/product/B00RM5UQP0/ref=ox_sc_act_title_1?smid=A25PA0SPA3UQ4X&psc=1)
- Install Android Things on the Raspberry Pi 3 (flashing instructions [here](https://developer.android.com/things/hardware/raspberrypi.html))

## References
- https://github.com/googlecodelabs/androidthings-imageclassifier
- https://github.com/tensorflow/tensorflow/tree/master/tensorflow/contrib/lite
