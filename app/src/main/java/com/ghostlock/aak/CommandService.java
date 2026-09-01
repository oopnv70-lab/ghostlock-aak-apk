package com.ghostlock.aak;

import android.content.Context;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Shizuku UserService that runs shell commands with shell (uid 2000) or root (uid 0) privilege.
 *
 * Official Shizuku UserService pattern:
 *   - must implement IBinder (here: ICommandService.Stub)
 *   - default constructor is required
 *   - Context constructor (v13+) must be annotated @Keep
 */
public class CommandService extends ICommandService.Stub {

    private static final String TAG = "CommandService";

    /**
     * Constructor is required.
     */
    public CommandService() {
        Log.i(TAG, "constructor");
    }

    /**
     * Constructor with Context. Available from Shizuku API v13.
     * Must be annotated with @Keep to prevent ProGuard removing it.
     */
    @Keep
    public CommandService(Context context) {
        Log.i(TAG, "constructor with Context: " + context);
    }

    /**
     * Reserved destroy method defined by Shizuku server.
     */
    @Override
    public void destroy() {
        Log.i(TAG, "destroy");
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    /**
     * Run a shell command with the service's privilege (shell uid 2000 or root).
     * Returns combined stdout+stderr output.
     */
    @Override
    public String runCommand(String cmd) {
        StringBuilder out = new StringBuilder();
        long startMs = System.currentTimeMillis();
        out.append("uid=").append(Os.getuid()).append(" pid=").append(Os.getpid()).append("\n");
        out.append("$ ").append(cmd).append("\n");
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            out.append("started at ").append(startMs).append("ms\n");

            // 并行读取 stdout 与 stderr，避免单流缓冲区满导致死锁
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            Thread outThread = new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        synchronized (out) {
                            out.append("[").append(nowMs()).append("] ").append(line).append("\n");
                        }
                    }
                } catch (Exception ignored) { }
            });
            Thread errThread = new Thread(() -> {
                String line;
                try {
                    while ((line = errReader.readLine()) != null) {
                        synchronized (out) {
                            out.append("[").append(nowMs()).append("] [stderr] ").append(line).append("\n");
                        }
                    }
                } catch (Exception ignored) { }
            });
            outThread.start();
            errThread.start();

            int exit = process.waitFor();
            outThread.join(5000);
            errThread.join(5000);
            long elapsed = System.currentTimeMillis() - startMs;
            out.append("exit=").append(exit).append(" elapsed=").append(elapsed).append("ms\n");
        } catch (Exception e) {
            out.append("ERROR: ").append(e).append("\n");
        }
        return out.toString();
    }

    /**
     * Write binary data to a file path with shell privilege (uid 2000).
     * File is created or overwritten, then chmod 755.
     * Returns true on success, false on error.
     */
    @Override
    public boolean writeFile(String path, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.write(data);
            fos.flush();
            fos.close();
            Runtime.getRuntime().exec(new String[]{"chmod", "755", path});
            Log.i(TAG, "writeFile OK: " + path + " (" + data.length + " bytes)");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeFile FAILED: " + path, e);
            return false;
        }
    }

    /**
     * Execute a shell command with streaming output via callback.
     * Each line of stdout/stderr is pushed to callback.onLine() in real time.
     * When the process exits, callback.onExit() is called.
     */
    @Override
    public void runCommandStream(String cmd, ICommandCallback callback) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                safeLine(callback, "uid=" + Os.getuid() + " pid=" + Os.getpid());
                safeLine(callback, "$ " + cmd);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                Thread outThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            safeLine(callback, line);
                        }
                    } catch (Exception ignored) { }
                });
                Thread errThread = new Thread(() -> {
                    String line;
                    try {
                        while ((line = errReader.readLine()) != null) {
                            safeLine(callback, "[stderr] " + line);
                        }
                    } catch (Exception ignored) { }
                });
                outThread.start();
                errThread.start();

                int exit = process.waitFor();
                outThread.join(5000);
                errThread.join(5000);
                safeExit(callback, exit);
            } catch (Exception e) {
                safeLine(callback, "ERROR: " + e);
                safeExit(callback, -1);
            }
        }).start();
    }

    /** Call callback.onLine() with RemoteException handled. */
    private static void safeLine(ICommandCallback callback, String line) {
        try {
            callback.onLine(line);
        } catch (RemoteException re) {
            Log.e(TAG, "callback.onLine failed", re);
        }
    }

    /** Call callback.onExit() with RemoteException handled. */
    private static void safeExit(ICommandCallback callback, int code) {
        try {
            callback.onExit(code);
        } catch (RemoteException re) {
            Log.e(TAG, "callback.onExit failed", re);
        }
    }

    private static String nowMs() {
        return String.valueOf(System.currentTimeMillis());
    }
}
