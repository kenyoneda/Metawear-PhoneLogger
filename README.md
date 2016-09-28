# MetaWear Android Starter
This project is a stripped down version of the MetaWear Android app that provides a template for creating a MetaWear Android 
app.  The provided code handles Bluetooth scanning and maintaining an connection to the board; all users need to do is add their 
own UI elements and setup their board with the MetaWear API.

# Usage
User additions will mostly be added to the [DeviceSetupActivityFragment](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/java/com/mbientlab/metawear/starter/DeviceSetupActivityFragment.java) 
class and the [fragment_device_setup.xml](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/res/layout/fragment_device_setup.xml) 
layout file.  In the DeviceSetupActivityFragment class, users can use the [reconnected](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/java/com/mbientlab/metawear/starter/DeviceSetupActivityFragment.java#L105) 
function to be alerted of when connection is re-established and the [ready](https://github.com/mbientlab-projects/MetaWear-AndroidStarterApp/blob/master/app/src/main/java/com/mbientlab/metawear/starter/DeviceSetupActivityFragment.java#L110) 
function to be notified of when the mwBoard variable has been assigned.

The next section provides a simple example that shows how to add a switch that controls the LED using this app template.

## LED Switch
In the ``fragment_device_setup.xml`` layout file, we will add a switch to turn on/off the LED.  

```xml
<Switch
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="LED"
        android:id="@+id/led_ctrl"
        android:layout_alignParentTop="true"
        android:layout_alignParentStart="true"
        android:layout_alignParentEnd="true"
        android:checked="false" />
```

In the DeviceSetupActivityFragment class, use the ``ready`` function to retrieve a reference to the Led module:

```java
private Led led= null;

public void ready() {
    try {
        led= mwBoard.getModule(Led.class);
    } catch (UnsupportedModuleException e) {
        Snackbar.make(getActivity().findViewById(R.id.device_setup_fragment), e.getMessage(), 
                Snackbar.LENGTH_SHORT).show();
    }
}
```

And, override ``onViewCreated`` to have the switch control the led with an ``OnCheckChangedListener`` class:

```java
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ((Switch) view.findViewById(R.id.led_ctrl)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                led.configureColorChannel(Led.ColorChannel.BLUE)
                        .setHighIntensity((byte) 31).setLowIntensity((byte) 31)
                        .setHighTime((short) 1000).setPulseDuration((short) 1000)
                        .setRepeatCount((byte) -1)
                        .commit();
                led.play(false);
            } else {
                led.stop(true);
            }
        }
    });
}
```

After making your code changes, load the app on your phone and use the switch to turn on/off the LED.
