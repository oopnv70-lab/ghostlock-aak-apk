package com.ghostlock.aak;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQ_SHIZUKU = 1001;
    private static final int REQ_PICK_SO = 1002;

    private TextView logView;
    private Button btnPermission;
    private Button btnPick;
    private Button btnRun;
    private Uri selectedSoUri;

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER =
            this::onRequestPermissionsResult;

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

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        refreshState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private void refreshState() {
        boolean hasPerm = false;
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                hasPerm = true;
            }
        } else {
            log("Shizuku service NOT running - start Shizuku first");
        }
        btnPermission.setEnabled(!hasPerm);
        btnPick.setEnabled(hasPerm);
        btnRun.setEnabled(hasPerm && selectedSoUri != null);
    }

    private void requestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            log("Shizuku too old (pre-v11), not supported");
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            log("Shizuku permission already granted");
            refreshState();
            return;
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            log("User denied before, requesting again");
        } else {
            log("Requesting Shizuku permission...");
        }
        Shizuku.requestPermission(REQ_SHIZUKU);
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            log("Shizuku permission GRANTED (uid=" + Shizuku.getUid() + ")");
        } else {
            log("Shizuku permission DENIED");
        }
        refreshState();
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
            refreshState();
        }
    }

    private void log(String msg) {
        logView.append(msg + "\n");
    }

    private void runExploit() {
        if (selectedSoUri == null) {
            log("No .so file selected");
            return;
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            log("Shizuku permission not granted");
            return;
        }
        try {
            String src = copyToDownload();
            if (src == null) {
                log("Failed to copy to /sdcard/Download");
                return;
            }
            log("Copied to: " + src);
            String dst = "/data/local/tmp/preload.so";
            String cmd = "cp " + src + " " + dst + " && chmod 755 " + dst +
                    " && LD_PRELOAD=" + dst + " /system/bin/sh -c 'echo exploit_triggered'";
            log("Running: " + cmd);
            runCommandInService(cmd);
        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    private String copyToDownload() throws Exception {
        String fileName = "ghostlock_preload.so";
        InputStream is = getContentResolver().openInputStream(selectedSoUri);
        if (is == null) return null;

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
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

    private void runCommandInService(String cmd) {
        ComponentName component = new ComponentName(getPackageName(), CommandService.class.getName());
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(component)
                .daemon(false)
                .processNameSuffix("ghostlock")
                .version(1);
        Shizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                log("UserService connected");
                try {
                    ICommandService service = ICommandService.Stub.asInterface(binder);
                    String result = service.runCommand(cmd);
                    log("Output:\n" + result);
                } catch (Exception e) {
                    log("Service error: " + e.getMessage());
                }
                try {
                    Shizuku.unbindUserService(args, this, true);
                } catch (Exception ignored) { }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                log("UserService disconnected");
            }
        });
    }
}
