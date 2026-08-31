package com.ghostlock.aak;

import android.content.Context;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.BufferedReader;
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

    private static String nowMs() {
        return String.valueOf(System.currentTimeMillis());
    }
}
