package com.ghostlock.aak;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQ_SHIZUKU = 1001;
    private static final int REQ_PICK_SO = 1002;

    private TextView logView;
    private Button btnPermission;
    private Button btnPick;
    private Button btnRun;
    private Uri selectedSoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logView = findViewById(R.id.logView);
        btnPermission = findViewById(R.id.btnPermission);
        btnPick = findViewById(R.id.btnPick);
        btnRun = findViewById(R.id.btnRun);

        btnPermission.setOnClickListener(v -> requestShizukuPermission());
        btnPick.setOnClickListener(v -> pickSoFile());
        btnRun.setOnClickListener(v -> runExploit());

        if (Shizuku.pingBinder()) {
            log("Shizuku service available");
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                log("Shizuku permission already granted");
                btnPermission.setEnabled(false);
                btnPick.setEnabled(true);
            }
        } else {
            log("Shizuku service NOT running - start Shizuku first");
        }
    }

    private void requestShizukuPermission() {
        if (Shizuku.pingBinder()) {
            Shizuku.requestPermission(REQ_SHIZUKU);
            log("Requesting Shizuku permission...");
        } else {
            log("Shizuku service not running");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SHIZUKU) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                log("Shizuku permission GRANTED (uid=2000 shell access)");
                btnPermission.setEnabled(false);
                btnPick.setEnabled(true);
            } else {
                log("Shizuku permission DENIED");
            }
        }
    }

    private void pickSoFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_PICK_SO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_SO && resultCode == RESULT_OK && data != null) {
            selectedSoUri = data.getData();
            log("Selected: " + selectedSoUri.getPath());
            btnRun.setEnabled(true);
        }
    }

    private void log(String msg) {
        logView.append(msg + "\n");
    }

    private void runExploit() {
        if (selectedSoUri == null) {
            log("No .so selected");
            return;
        }
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            log("Shizuku permission not granted");
            return;
        }
        try {
            // Step 1: copy selected content:// file to /sdcard/Download/ (shell uid 2000 can read this)
            String downloadPath = copyToDownload();
            if (downloadPath == null) {
                log("Failed to copy to /sdcard/Download");
                return;
            }
            log("Copied to: " + downloadPath);

            // Step 2: via Shizuku, move to /data/local/tmp and run exploit
            String dst = "/data/local/tmp/preload.so";
            String cmd = "cp " + downloadPath + " " + dst + " && chmod 755 " + dst +
                    " && LD_PRELOAD=" + dst + " /system/bin/sh -c 'echo exploit_triggered'";
            log("Running: " + cmd);
            execAsShell(cmd);
            log("Exploit triggered");
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    private String copyToDownload() throws Exception {
        String fileName = "ghostlock_preload.so";
        InputStream is = getContentResolver().openInputStream(selectedSoUri);
        if (is == null) return null;

        if (Build.VERSION.SDK_INT >= 29) {
            // MediaStore for API 29+
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri == null) { is.close(); return null; }
            OutputStream os = getContentResolver().openOutputStream(uri);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
            os.close();
            is.close();
            return "/sdcard/Download/" + fileName;
        } else {
            // Legacy direct write (API < 29)
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "GhostLockAAK");
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, fileName);
            OutputStream os = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
            os.close();
            is.close();
            return outFile.getAbsolutePath();
        }
    }

    private void execAsShell(String command) throws Exception {
        String[] cmd = { "sh", "-c", command };
        ParcelFileDescriptor pfd = Shizuku.newProcess(cmd, null, null);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pfd.getFileDescriptor())));
        String line;
        while ((line = reader.readLine()) != null) {
            log("  " + line);
        }
        reader.close();
    }
}
