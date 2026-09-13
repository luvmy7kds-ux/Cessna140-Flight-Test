package com.cessna140.flighttest;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 140;
    private WebView webView;
    private LocationManager locationManager;
    private final List<Location> gpsPoints = new ArrayList<>();
    private boolean gpsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 19, 31));
        getWindow().setNavigationBarColor(Color.rgb(8, 19, 31));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    requestLocationPermission();
                    callback.invoke(origin, false, false);
                }
            }
        });
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");

        if (!hasLocationPermission()) requestLocationPermission();
    }

    private boolean hasLocationPermission() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            synchronized (gpsPoints) {
                if (gpsRecording) gpsPoints.add(new Location(location));
            }
        }
        @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override public void onProviderEnabled(String provider) { }
        @Override public void onProviderDisabled(String provider) { }
    };

    private void startNativeGps() {
        synchronized (gpsPoints) { gpsPoints.clear(); }
        if (!hasLocationPermission()) {
            requestLocationPermission();
            Toast.makeText(this, "Allow location, then start the run again.", Toast.LENGTH_LONG).show();
            return;
        }
        gpsRecording = true;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 250L, 0f, locationListener);
        } catch (SecurityException e) {
            gpsRecording = false;
        }
    }

    private String stopNativeGpsAndGetJson() {
        gpsRecording = false;
        try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
        JSONArray arr = new JSONArray();
        synchronized (gpsPoints) {
            for (Location l : gpsPoints) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("lat", l.getLatitude());
                    o.put("lon", l.getLongitude());
                    o.put("t", l.getTime());
                    o.put("speed", l.hasSpeed() ? l.getSpeed() : JSONObject.NULL);
                    o.put("heading", l.hasBearing() ? l.getBearing() : JSONObject.NULL);
                    o.put("acc", l.hasAccuracy() ? l.getAccuracy() : JSONObject.NULL);
                    arr.put(o);
                } catch (Exception ignored) { }
            }
        }
        return arr.toString();
    }

    private void saveTextFile(String filename, String mime, String content) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            OutputStream out;
            Uri savedUri = null;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Cessna140FlightTest");
                savedUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (savedUri == null) throw new Exception("Could not create file");
                out = getContentResolver().openOutputStream(savedUri);
            } else {
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = getFilesDir();
                File file = new File(dir, filename);
                out = new FileOutputStream(file);
                savedUri = Uri.fromFile(file);
            }
            if (out == null) throw new Exception("Could not open file");
            out.write(bytes);
            out.flush();
            out.close();
            final String msg = "Saved " + filename + " to Downloads/Cessna140FlightTest";
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Could not save file: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public boolean hasLocationPermission() { return MainActivity.this.hasLocationPermission(); }

        @JavascriptInterface
        public void requestLocationPermission() { runOnUiThread(MainActivity.this::requestLocationPermission); }

        @JavascriptInterface
        public void startGps() { runOnUiThread(MainActivity.this::startNativeGps); }

        @JavascriptInterface
        public String stopGpsAndGetJson() { return MainActivity.this.stopNativeGpsAndGetJson(); }

        @JavascriptInterface
        public void saveTextFile(String filename, String mime, String content) {
            MainActivity.this.saveTextFile(filename, mime, content);
        }

        @JavascriptInterface
        public String appVersion() { return "1.2"; }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) { }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
