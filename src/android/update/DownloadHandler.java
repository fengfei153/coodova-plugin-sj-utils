package com.fhsjdz.cordova.utils.update;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.FileProvider;
import android.widget.ProgressBar;

import com.fh3jyun.appbasic.BuildConfig;

import java.io.File;

import org.apache.cordova.CordovaWebView;

/**
 * Created by LuoWen on 2015/12/14.
 */
public class DownloadHandler extends Handler {
    private String TAG = "DownloadHandler";

    private Context mContext;
    /* 更新进度条 */
    private ProgressBar mProgress;
    /* 记录进度条数量 */
    private int progress;
    /* 下载保存路径 */
    private String mSavePath;
    /* 保存解析的XML信息 */
    private String updateApkUrl;
    private CordovaWebView mWebView;

    public DownloadHandler(Context mContext, ProgressBar mProgress, String mSavePath, String updateApkUrl, CordovaWebView webview) {
        this.mContext = mContext;
        this.mProgress = mProgress;
        this.mSavePath = mSavePath;
        this.updateApkUrl = updateApkUrl;
        this.mWebView = webview;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            // 正在下载
            case Constants.DOWNLOAD:
                // 设置进度条位置
                mProgress.setProgress(progress);
                break;
            case Constants.DOWNLOAD_FINISH:
                // 安装文件
                installApk();
                break;
            default:
                break;
        }
    }

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    /**
     * 安装APK文件
     */
    private void installApk() {
        File apkFile = new File(mSavePath, updateApkUrl.substring(updateApkUrl.lastIndexOf("/")+1));
        if (!apkFile.exists()) {
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", apkFile);
            i.setDataAndType(contentUri, "application/vnd.android.package-archive");
        }else{
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        mContext.startActivity(i);
        mWebView.getPluginManager().postMessage("exit", null);
    }
}
