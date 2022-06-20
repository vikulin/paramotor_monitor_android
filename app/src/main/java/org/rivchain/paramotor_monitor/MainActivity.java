package org.rivchain.paramotor_monitor;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.os.UserHandle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.widget.EditText;
import android.widget.Toast;

import com.rivchain.paramotor_monitor.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnBluetoothDeviceClickedListener {
    private final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1;


    private static final int REQUEST_CONNECT = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 0;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    public static final String EXTRAS_DEVICE_NAME = "extras_device_name";
    public static final String EXTRAS_DEVICE_ADDRESS = "extras_device_address";
    private String mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 1000 * 10;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private MyBluetoothDeviceAdapter mBluetoothDeviceAdapter;
    private final List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();
    private final MyBluetoothScanCallBack mBluetoothScanCallBack = new MyBluetoothScanCallBack();
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        this.requestPermissions();
        initData();
        initService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initReceiver();
        scanLeDevice(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("MainActivity", "unregisterReceiver()");
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check.
            if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_BLUETOOTH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void initView() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
    }

    private void initService() {
        Log.i("MainActivity", "initService()");

        if (mBluetoothLeService == null) {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    private void initData() {
        mHandler = new Handler();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        mBluetoothDeviceAdapter = new MyBluetoothDeviceAdapter(mBluetoothDeviceList, this);
        recyclerView.setAdapter(mBluetoothDeviceAdapter);

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (mBluetoothDeviceList != null) {
                    mBluetoothDeviceList.clear();
                }
                scanLeDevice(true);
            }
        });


    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("MainActivity", "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    private void scanLeDevice(boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipeRefresh.setRefreshing(false);
                    BluetoothScan.stopScan();
                }
            }, SCAN_PERIOD);
            swipeRefresh.setRefreshing(true);
            BluetoothScan.startScan(true, mBluetoothScanCallBack);
        } else {
            swipeRefresh.setRefreshing(false);
            BluetoothScan.stopScan();
        }
    }

    @Override
    public void onBluetoothDeviceClicked(String name, String address) {

        Log.i("MainActivity", "Attempt to connect device : " + name + "(" + address + ")");
        mDeviceName = name;
        mDeviceAddress = address;

        if (mBluetoothLeService != null) {

            if (mBluetoothLeService.connect(mDeviceAddress)) {
                showMsg("Attempt to connect device : " + name);

                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTING;
                swipeRefresh.setRefreshing(true);
            }
        }
    }

    private void initReceiver() {
        Log.i("MainActivity", "initReceiver()");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);

        registerReceiver(mGattUpdateReceiver, intentFilter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("MainActivity", "ACTION_GATT_CONNECTED!!!");
                showMsg("Connected device ..");

                mConnectionState = BluetoothLeService.ACTION_GATT_CONNECTED;
                swipeRefresh.setRefreshing(false);

                inputMessage();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i("MainActivity", "ACTION_GATT_DISCONNECTED!!!");
                showMsg("disconnected");
                mConnectionState = BluetoothLeService.ACTION_GATT_DISCONNECTED;
                swipeRefresh.setRefreshing(false);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mBluetoothLeService.getSupportedGattServices();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                showMsg("Got string : " + new String(data));

                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        char b = (char) byteChar;
                        stringBuilder.append(b);
                    }

                    Log.i("MainActivity", "Get string : " + stringBuilder.toString());
                }
            }
        }
    };

    private void inputMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Message to send");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                btSendBytes(text.getBytes());
            }
        });


        builder.show();
    }

    public void btSendBytes(byte[] data) {
        if (mBluetoothLeService != null &&
                mConnectionState.equals(BluetoothLeService.ACTION_GATT_CONNECTED)) {
            mBluetoothLeService.writeCharacteristic(data);
        }
    }

    private class MyBluetoothScanCallBack extends Context implements BluetoothScan.BluetoothScanCallBack {
        @Override
        public void onLeScanInitFailure(int failureCode) {
            Log.i("MainActivity", "onLeScanInitFailure()");
            switch (failureCode) {
                case BluetoothScan.SCAN_FEATURE_ERROR:
                    showMsg("scan_feature_error");
                    break;
                case BluetoothScan.SCAN_ADAPTER_ERROR:
                    showMsg("scan_adapter_error");
                    break;
                default:
                    showMsg("unKnow_error");
            }
        }

        @Override
        public void onLeScanInitSuccess(int successCode) {
            Log.i("MainActivity", "onLeScanInitSuccess()");
            switch (successCode) {
                case BluetoothScan.SCAN_BEGIN_SCAN:
                    Log.i("MainActivity", "successCode : " + successCode);
                    break;
                case BluetoothScan.SCAN_NEED_ENADLE:
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    break;
                case BluetoothScan.AUTO_ENABLE_FAILURE:
                    showMsg("auto_enable_bluetooth_error");
                    break;
                default:
                    showMsg("unKnow_error");
            }
        }

        @Override
        public void onLeScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (!mBluetoothDeviceList.contains(device) && device != null) {
                mBluetoothDeviceList.add(device);
                mBluetoothDeviceAdapter.notifyDataSetChanged();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Log.i("MainActivity", "notifyDataSetChanged() " + "BluetoothName :　" + device.getName() +
                        "  BluetoothAddress :　" + device.getAddress());
            }
        }

        @Override
        public AssetManager getAssets() {
            return null;
        }

        @Override
        public Resources getResources() {
            return null;
        }

        @Override
        public PackageManager getPackageManager() {
            return null;
        }

        @Override
        public ContentResolver getContentResolver() {
            return null;
        }

        @Override
        public Looper getMainLooper() {
            return null;
        }

        @Override
        public Context getApplicationContext() {
            return null;
        }

        @Override
        public void setTheme(int i) {

        }

        @Override
        public Resources.Theme getTheme() {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public String getPackageName() {
            return null;
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            return null;
        }

        @Override
        public String getPackageResourcePath() {
            return null;
        }

        @Override
        public String getPackageCodePath() {
            return null;
        }

        @Override
        public SharedPreferences getSharedPreferences(String s, int i) {
            return null;
        }

        @Override
        public boolean moveSharedPreferencesFrom(Context context, String s) {
            return false;
        }

        @Override
        public boolean deleteSharedPreferences(String s) {
            return false;
        }

        @Override
        public FileInputStream openFileInput(String s) throws FileNotFoundException {
            return null;
        }

        @Override
        public FileOutputStream openFileOutput(String s, int i) throws FileNotFoundException {
            return null;
        }

        @Override
        public boolean deleteFile(String s) {
            return false;
        }

        @Override
        public File getFileStreamPath(String s) {
            return null;
        }

        @Override
        public File getDataDir() {
            return null;
        }

        @Override
        public File getFilesDir() {
            return null;
        }

        @Override
        public File getNoBackupFilesDir() {
            return null;
        }

        @Nullable
        @Override
        public File getExternalFilesDir(@Nullable String s) {
            return null;
        }

        @Override
        public File[] getExternalFilesDirs(String s) {
            return new File[0];
        }

        @Override
        public File getObbDir() {
            return null;
        }

        @Override
        public File[] getObbDirs() {
            return new File[0];
        }

        @Override
        public File getCacheDir() {
            return null;
        }

        @Override
        public File getCodeCacheDir() {
            return null;
        }

        @Nullable
        @Override
        public File getExternalCacheDir() {
            return null;
        }

        @Override
        public File[] getExternalCacheDirs() {
            return new File[0];
        }

        @Override
        public File[] getExternalMediaDirs() {
            return new File[0];
        }

        @Override
        public String[] fileList() {
            return new String[0];
        }

        @Override
        public File getDir(String s, int i) {
            return null;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String s, int i, SQLiteDatabase.CursorFactory cursorFactory) {
            return null;
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String s, int i, SQLiteDatabase.CursorFactory cursorFactory, @Nullable DatabaseErrorHandler databaseErrorHandler) {
            return null;
        }

        @Override
        public boolean moveDatabaseFrom(Context context, String s) {
            return false;
        }

        @Override
        public boolean deleteDatabase(String s) {
            return false;
        }

        @Override
        public File getDatabasePath(String s) {
            return null;
        }

        @Override
        public String[] databaseList() {
            return new String[0];
        }

        @Override
        public Drawable getWallpaper() {
            return null;
        }

        @Override
        public Drawable peekWallpaper() {
            return null;
        }

        @Override
        public int getWallpaperDesiredMinimumWidth() {
            return 0;
        }

        @Override
        public int getWallpaperDesiredMinimumHeight() {
            return 0;
        }

        @Override
        public void setWallpaper(Bitmap bitmap) throws IOException {

        }

        @Override
        public void setWallpaper(InputStream inputStream) throws IOException {

        }

        @Override
        public void clearWallpaper() throws IOException {

        }

        @Override
        public void startActivity(Intent intent) {

        }

        @Override
        public void startActivity(Intent intent, @Nullable Bundle bundle) {

        }

        @Override
        public void startActivities(Intent[] intents) {

        }

        @Override
        public void startActivities(Intent[] intents, Bundle bundle) {

        }

        @Override
        public void startIntentSender(IntentSender intentSender, @Nullable Intent intent, int i, int i1, int i2) throws IntentSender.SendIntentException {

        }

        @Override
        public void startIntentSender(IntentSender intentSender, @Nullable Intent intent, int i, int i1, int i2, @Nullable Bundle bundle) throws IntentSender.SendIntentException {

        }

        @Override
        public void sendBroadcast(Intent intent) {

        }

        @Override
        public void sendBroadcast(Intent intent, @Nullable String s) {

        }

        @Override
        public void sendOrderedBroadcast(Intent intent, @Nullable String s) {

        }

        @Override
        public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String s, @Nullable BroadcastReceiver broadcastReceiver, @Nullable Handler handler, int i, @Nullable String s1, @Nullable Bundle bundle) {

        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle userHandle) {

        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle userHandle, @Nullable String s) {

        }

        @Override
        public void sendOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, @Nullable String s, BroadcastReceiver broadcastReceiver, @Nullable Handler handler, int i, @Nullable String s1, @Nullable Bundle bundle) {

        }

        @Override
        public void sendStickyBroadcast(Intent intent) {

        }

        @Override
        public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver broadcastReceiver, @Nullable Handler handler, int i, @Nullable String s, @Nullable Bundle bundle) {

        }

        @Override
        public void removeStickyBroadcast(Intent intent) {

        }

        @Override
        public void sendStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {

        }

        @Override
        public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle userHandle, BroadcastReceiver broadcastReceiver, @Nullable Handler handler, int i, @Nullable String s, @Nullable Bundle bundle) {

        }

        @Override
        public void removeStickyBroadcastAsUser(Intent intent, UserHandle userHandle) {

        }

        @Nullable
        @Override
        public Intent registerReceiver(@Nullable BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(@Nullable BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, int i) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, @Nullable String s, @Nullable Handler handler) {
            return null;
        }

        @Nullable
        @Override
        public Intent registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, @Nullable String s, @Nullable Handler handler, int i) {
            return null;
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {

        }

        @Nullable
        @Override
        public ComponentName startService(Intent intent) {
            return null;
        }

        @Nullable
        @Override
        public ComponentName startForegroundService(Intent intent) {
            return null;
        }

        @Override
        public boolean stopService(Intent intent) {
            return false;
        }

        @Override
        public boolean bindService(Intent intent, @NonNull ServiceConnection serviceConnection, int i) {
            return false;
        }

        @Override
        public void unbindService(@NonNull ServiceConnection serviceConnection) {

        }

        @Override
        public boolean startInstrumentation(@NonNull ComponentName componentName, @Nullable String s, @Nullable Bundle bundle) {
            return false;
        }

        @Override
        public Object getSystemService(@NonNull String s) {
            return null;
        }

        @Nullable
        @Override
        public String getSystemServiceName(@NonNull Class<?> aClass) {
            return null;
        }

        @Override
        public int checkPermission(@NonNull String s, int i, int i1) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkCallingPermission(@NonNull String s) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkCallingOrSelfPermission(@NonNull String s) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkSelfPermission(@NonNull String s) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void enforcePermission(@NonNull String s, int i, int i1, @Nullable String s1) {

        }

        @Override
        public void enforceCallingPermission(@NonNull String s, @Nullable String s1) {

        }

        @Override
        public void enforceCallingOrSelfPermission(@NonNull String s, @Nullable String s1) {

        }

        @Override
        public void grantUriPermission(String s, Uri uri, int i) {

        }

        @Override
        public void revokeUriPermission(Uri uri, int i) {

        }

        @Override
        public void revokeUriPermission(String s, Uri uri, int i) {

        }

        @Override
        public int checkUriPermission(Uri uri, int i, int i1, int i2) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkCallingUriPermission(Uri uri, int i) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkCallingOrSelfUriPermission(Uri uri, int i) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public int checkUriPermission(@Nullable Uri uri, @Nullable String s, @Nullable String s1, int i, int i1, int i2) {
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void enforceUriPermission(Uri uri, int i, int i1, int i2, String s) {

        }

        @Override
        public void enforceCallingUriPermission(Uri uri, int i, String s) {

        }

        @Override
        public void enforceCallingOrSelfUriPermission(Uri uri, int i, String s) {

        }

        @Override
        public void enforceUriPermission(@Nullable Uri uri, @Nullable String s, @Nullable String s1, int i, int i1, int i2, @Nullable String s2) {

        }

        @Override
        public Context createPackageContext(String s, int i) throws PackageManager.NameNotFoundException {
            return null;
        }

        @Override
        public Context createContextForSplit(String s) throws PackageManager.NameNotFoundException {
            return null;
        }

        @Override
        public Context createConfigurationContext(@NonNull Configuration configuration) {
            return null;
        }

        @Override
        public Context createDisplayContext(@NonNull Display display) {
            return null;
        }

        @Override
        public Context createDeviceProtectedStorageContext() {
            return null;
        }

        @Override
        public boolean isDeviceProtectedStorage() {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                showMsg("enable_bluetooth_error");
                return;
            } else if (resultCode == Activity.RESULT_OK) {
                if (mBluetoothDeviceList != null) {
                    mBluetoothDeviceList.clear();
                }
                scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static Toast toast = null;

    public static void showMsg(String msg) {


        try {
            if (toast == null) {
                toast = Toast.makeText(MyApplication.context(), msg, Toast.LENGTH_SHORT);
            } else {
                toast.setText(msg);
            }
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}