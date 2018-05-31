package com.fhsjdz.cordova.utils.update;

import android.app.Dialog;
import android.content.Context;
import android.os.Environment;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.cordova.CordovaWebView;

/**
 * 下载文件线程
 */
public class DownloadApkThread implements Runnable {
    private String TAG = "DownloadApkThread";

    /* 保存解析的XML信息 */
    String updateApkUrl;
    /* 下载保存路径 */
    private String mSavePath;
    /* 记录进度条数量 */
    private int progress;
    /* 更新进度条 */
    private ProgressBar mProgress;
    /* 是否取消更新 */
    private boolean cancelUpdate = false;
    private Dialog mDownloadDialog;
    private Context mContext;
    private DownloadHandler mHandler;

    public DownloadApkThread(Context mContext, ProgressBar mProgress, Dialog mDownloadDialog, String updateApkUrl, CordovaWebView webview) {
        this.mContext = mContext;
        this.mProgress = mProgress;
        this.mDownloadDialog = mDownloadDialog;
        this.updateApkUrl = updateApkUrl;

        this.mSavePath = Environment.getExternalStorageDirectory() + "/" + "Download"; // SD Path
        this.mHandler = new DownloadHandler(mContext, mProgress, mSavePath, updateApkUrl, webview);
    }


    @Override
    public void run() {
        downloadAndInstall();
        // 取消下载对话框显示
        mDownloadDialog.dismiss();
    }

    public void cancelBuildUpdate() {
        this.cancelUpdate = true;
    }

    private void downloadAndInstall() {
        try {
            // 判断SD卡是否存在，并且是否具有读写权限
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // 获得存储卡的路径
                URL url = new URL(updateApkUrl);
                // 创建连接
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                // 获取文件大小
                int length = conn.getContentLength();
                // 创建输入流
                InputStream is = conn.getInputStream();

                File file = new File(mSavePath);
                // 判断文件目录是否存在
                if (!file.exists()) {
                    file.mkdir();
                }
                File apkFile = new File(mSavePath, updateApkUrl.substring(updateApkUrl.lastIndexOf("/")+1));
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                // 缓存
                byte buf[] = new byte[1024];

                // 写入到文件中
                do {
                    int numread = is.read(buf);
                    count += numread;
                    // 计算进度条位置
                    progress = (int) (((float) count / length) * 100);
                    mHandler.updateProgress(progress);
                    // 更新进度
                    mHandler.sendEmptyMessage(Constants.DOWNLOAD);
                    if (numread <= 0) {
                        // 下载完成
                        mHandler.sendEmptyMessage(Constants.DOWNLOAD_FINISH);
                        break;
                    }
                    // 写入文件
                    fos.write(buf, 0, numread);
                } while (!cancelUpdate);// 点击取消就停止下载.
                fos.close();
                is.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}