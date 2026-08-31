package com.ghostlock.aak;

interface ICommandService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    String runCommand(String cmd) = 2; // Execute a shell command, return output

    boolean writeFile(String path, in byte[] data) = 3; // Write bytes to path (shell privilege)
}
