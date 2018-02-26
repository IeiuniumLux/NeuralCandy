# NeuralCandy

The NeuralCandy project combines image classifier and sugar highs in one delicious [Android Things](https://developer.android.com/things/get-started/index.html){:target="_blank"} project. The application asks for a random image to be placed in front of the [camera module](https://www.raspberrypi.org/products/camera-module-v2/) and if it matches the request; then the motor of the candy dispenser is activated to releases the delicious reward.

NeuralCandy uses the  [TensorFlow Lite](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/contrib/lite#tensorflow-lite) inference library for Android to locally classify the captured image against a pre-trained [ImageNet](http://image-net.org/) model. This model is good at recognizing categories that it was trained with. You can use a smartphone to search on Google for the requested target image and put it in front of the Pi camera. The [Raspberry Pi 3 model B](https://developer.android.com/things/hardware/raspberrypi.html) will handle the image processing and the motor for the candy release.

> Note that the Android Things project is still in the early-adopters stage and it may still have some stability issues. NeuralCandy is built using the [preview 6.1 release](https://developer.android.com/things/preview/releases.html#preview-6-1); which requires to reboot the Raspberry Pi after installing the app because the camera permission is not granted until the next device reboot. Another limitation in this preview release is the maximum resolution; which is only [640*480](https://issuetracker.google.com/issues/37134104). Hopefully, the next release of Android Things will support higher resolutions since the Raspberry Pi camera V2.1 is capable of taking 8MP images / 1080p videos at 30fps.

## What you'll need

- [Android Studio 3.0+](https://developer.android.com/studio/index.html)
- The following individual components:
  - [Raspberry Pi 3 Model B](https://www.adafruit.com/product/3055)
  - [Raspberry Pi Camera Board v2](https://www.adafruit.com/product/3099)
  - [5V 2.4A Power Supply](https://www.adafruit.com/product/1995)
  - [Explorer pHAT](https://www.adafruit.com/product/3018) or [L9110 Breakout Board](https://www.ebay.com/sch/i.html?_from=R40&_sacat=0&_nkw=L9110+5V+Fan+Motor+Module&_sop=15)
  - [7" Touchscreen Display](https://www.adafruit.com/product/2718)
  - [SmartiPi Case](https://www.adafruit.com/product/3576)
  - [USB to TTL Serial Cable](https://www.adafruit.com/product/954) (optional)
  - [Lens Adjustment Tool](https://www.adafruit.com/product/3518) (optional)
  - [16GB microSD card](https://www.amazon.com/gp/product/B010Q57SEE/ref=ox_sc_act_title_2?smid=ATVPDKIKX0DER&psc=1)
  - [Mini Candy Dispenser](https://www.amazon.com/gp/product/B00RM5UQP0/ref=ox_sc_act_title_1?smid=A25PA0SPA3UQ4X&psc=1)
- Install Android Things on the Raspberry Pi 3 (flashing instructions [here](https://developer.android.com/things/hardware/raspberrypi.html))

### Why do I need a motor driver?
The Raspberry Pi's GPIO ports can only supply a few mA of current (16mA max). Attempting to draw more than this will damage the Pi. Motors typically require at least 400mA to start spinning (although they draw far less after startup).  Motor drivers are often H-Bridge circuits, capable of driving a motor forwards or backwards. The Explorer pHAT has a dual H-Bridge and circuitry that makes the controlling of the motor easier.

![](/app/assets/class_diagram.gif)

## References
- https://github.com/googlecodelabs/androidthings-imageclassifier
- https://github.com/androidthings/sample-simpleui/tree/master/app
- https://github.com/tensorflow/tensorflow/tree/master/tensorflow/contrib/lite
