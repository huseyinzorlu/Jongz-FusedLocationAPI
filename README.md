# Jongz-FusedLocationAPI

BASIC USEFUL FEATURE LIST

 * support usage in Activity, Application (Singleton) and Service class
 * support use case for BroadcastReceiver to check location providers changed
 * Handle fix for google play services and location settings not ready
 * Just one click to enable location permission like google map

ADD DEPENDENCY

* Add Google-Play-Services-lib
* Add Jongz-FusedLocationAPI library



SETUP MANIFEST 

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

BASIC USAGE EXAMPLE ....

### Singleton for all activity:

```android
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
            manager = new FusedLocationManager(this);
        }
        return manager;
    }
}
```

### Get Last Location:

```android
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

        app.getLocationManager().getLastLocation(new OnLocationResponse() {
            @Override
            public void LocationResponseSuccess(Location loc) {
                Log.i(TAG, "Longitude: " + loc.getLongitude());
        		Log.i(TAG, "Latitude: " + loc.getLatitude());
                loadingDialog.dismiss();
            }

            @Override
            public void LocationResponseFailure(String error) {
                Log.e(TAG, error);
                loadingDialog.dismiss();
            }
        });
    }
}
```

### Use Service for keep track location update:

```android
public class SampleService extends Service implements OnLocationUpdate {

    private final String TAG = getClass().getSimpleName();
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        FusedLocationManager manager = new FusedLocationManager(this);
        manager.setOnLocationUpdateListener(this);
        manager.isLocationServiceCanUse();
    }

    @Override
    public void locationUpdate(Location loc) {
        Log.i(TAG, "update: keep tracking user location...");
        Log.i(TAG, "Longitude: " + loc.getLongitude());
        Log.i(TAG, "Latitude: " + loc.getLatitude());
    }
}
```

### Keep Monitor Location Providers:

```android
public class SampleReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            Log.e(TAG, "onReceive was call because location providers have changes");
            FusedLocationManager manager = new FusedLocationManager(context);
            manager.isLocationServiceCanUse();
        }
    }
}
```
