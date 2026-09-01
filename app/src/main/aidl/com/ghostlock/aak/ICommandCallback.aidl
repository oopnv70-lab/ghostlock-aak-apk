package com.ghostlock.aak;

oneway interface ICommandCallback {

    void onLine(String line);      // 每读到一行输出时回调

    void onExit(int exitCode);     // 进程退出时回调
}
