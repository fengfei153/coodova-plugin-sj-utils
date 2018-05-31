package com.fhsjdz.cordova.utils.update;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.widget.ProgressBar;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by LuoWen on 2015/10/27.
 * <p/>
 * Thanks @coolszy
 */
public class UpdateManager {
    public static final String TAG = "UpdateManager";

    //升级包地址：http://www.xxxx.com/android.apk
    private String updateApkUrl;
    private Context mContext;
    private MsgBox msgBox;
    private Boolean isDownloading = false;

    private DownloadApkThread downloadApkThread;

    public UpdateManager(JSONArray args, CallbackContext callbackContext, Context context, String updateUrl) {
        this.updateApkUrl = updateUrl;
        this.mContext = context;
        msgBox = new MsgBox(mContext);
    }

    /**
     * 检测软件更新
     */
    public void update(CordovaWebView webview) {
    	if(isDownloading) {
            msgBox.showDownloadDialog(null);
        } else {
        	isDownloading = true;
        	// 显示下载对话框
            Map<String, Object> ret = msgBox.showDownloadDialog(downloadDialogOnClick);
            // 下载文件
            downloadApk((Dialog)ret.get("dialog"), (ProgressBar)ret.get("progress"), webview);
        }
    }

    private OnClickListener downloadDialogOnClick = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            // 设置取消状态
            //downloadApkThread.cancelBuildUpdate();
        }
    };

    private OnClickListener errorDialogOnClick = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    /**
     * 下载apk文件
     * @param mProgress
     * @param mDownloadDialog
     */
    private void downloadApk(Dialog mDownloadDialog, ProgressBar mProgress, CordovaWebView webview) {
        LOG.d(TAG, "downloadApk" + mProgress);

        // 启动新线程下载软件
        downloadApkThread = new DownloadApkThread(mContext, mProgress, mDownloadDialog, updateApkUrl, webview);
        new Thread(downloadApkThread).start();
    }

}