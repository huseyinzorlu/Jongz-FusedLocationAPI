# Jongz-FusedLocationAPI

#### BASIC USEFUL FEATURE LIST

 * Support usage in Activity, Application (Singleton) and Service class.
 * Support use case W/BroadcastReceiver to check location providers changed.
 * Handle fix for google play services and location settings not prompt.
 * One click to enable location permission like google map.
 * Lean and lite google-play-services.jar to prevent dex overflow 56k methods.
 * Less power consumption by cache location result.

#### ADD DEPENDENCY

* Add Google-Play-Services-lib
* Add Jongz-FusedLocationAPI library
* ADD Gson.jar

#### SETUP MANIFEST 

* Add Permission

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```

* Add Meta Data

```xml
	<meta-data
		android:name="com.google.android.gms.version"
		android:value="@integer/google_play_services_version" />
```

* Add Fix Permission Activity

```xml
<activity
	android:name="com.puangput.jongz.fusedlocation.FixLocationPermissionActivity"
	android:label="@string/app_name">
	<intent-filter>
		<category android:name="android.intent.category.DEFAULT"/>
	</intent-filter>
</activity>
```

#### BASIC USAGE EXAMPLE ....

* Singleton for all activity:

```java
public class SampleApp extends Application {

    private static SampleApp app;
    private FusedLocationManager manager;

    public static SampleApp getInstance() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public FusedLocationManager getLocationManager() {
        if (manager == null) {
            manager = new FusedLocationManager.Builder(this)
                    .setIsRequestDistance(false) // disable update by distance update
                    .setCachedExpiredTime(15 * 1000)  // set zero value to disable library use cached location
                    .build();
        }
        return manager;
    }
}
```

* Get Last Location:

```java
public class SampleActivity extends Activity implements View.OnClickListener{

    private final String TAG = getClass().getSimpleName();
    public LoadingDialog loadingDialog;
    public SampleApp app;
    private Button btnGet;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        btnGet = (Button)findViewById(R.id.btnGet);
        btnGet.setOnClickListener(this);
        app = SampleApp.getInstance();
        loadingDialog = new LoadingDialog(this);
    }

    @Override
    public void onClick(View v) {

        loadingDialog.show();
        app.getLocationManager().start();
        app.getLocationManager().getLastLocation(new OnLocationResponse() {
            @Override
            public void LocationResponseSuccess(Location loc) {
                Log.i(TAG, "Longitude: " + loc.getLongitude());
        		Log.i(TAG, "Latitude: " + loc.getLatitude());
                loadingDialog.dismiss();
                // After get last location,
                // call stop() for stop using location and save power consumption.
                app.getLocationManager().stop(); 
            }

            @Override
            public void LocationResponseFailure(String error) {
                Log.e(TAG, error);
                loadingDialog.dismiss();
                app.getLocationManager().stop();
            }
        });
    }
}
```

* Use Service for keep track location update:

```java
public class SampleService extends Service implements OnLocationUpdate {

    private final String TAG = getClass().getSimpleName();
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        FusedLocationManager manager = new FusedLocationManager.Builder(this)
                .setRequestInterval(60 * 1000)
                .setRequestFastInterval(60 * 1000)
                .setRequestDistance(100)
                .setIsRequestDistance(true)
                .setCachedExpiredTime(0)
                .setRetryTimeout(20 * 1000)
                .setMaxRetry(3)
                .build();
        manager.setOnLocationUpdateListener(this);
        manager.start();
    }
    
     @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand()");

        if (manager.isConnect()) {
            // show fix activity, if location settings not satisfied
            manager.checkLocationProviderPrompt();
        }

        return START_STICKY;
    }

    @Override
    public void locationUpdate(Location loc) {
        Log.i(TAG, "update: keep tracking user location...");
        Log.i(TAG, "Longitude: " + loc.getLongitude());
        Log.i(TAG, "Latitude: " + loc.getLatitude());
    }
}
```

* Keep Monitor Location Providers W/BroadcastReceiver:

```java
public class SampleReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
			Log.e(TAG, "onReceive was call because location providers have changes");
			// start your service and call checkLocationProviderPrompt()
            // for start fix dialog, if provide change is not meet
            // fused location api requirement.
            context.startService(new Intent(context, SampleService.class));
        }
    }
}
```
* DON'T Forget to Define Receiver in Android Manifest:

```xml
<receiver
	android:name=".SampleReceiver"
	android:enabled="true"
	android:exported="false">
	<intent-filter>
		<action android:name="android.location.PROVIDERS_CHANGED"/>
	</intent-filter>
</receiver>
```