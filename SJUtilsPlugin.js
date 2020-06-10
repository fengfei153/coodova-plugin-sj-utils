var exec = require('cordova/exec');
function SJUtilsPlugin() { };

SJUtilsPlugin.prototype.getVersionCode = function (success) {
    exec(success, null, "SJUtilsPlugin", "getVersionCode", []);
};
SJUtilsPlugin.prototype.update = function (updateUrl, success) {
    exec(success, null, "SJUtilsPlugin", "update", [updateUrl]);
};
SJUtilsPlugin.prototype.getWifiSSID = function (success) {
    exec(success, null, "SJUtilsPlugin", "getWifiSSID", []);
};
SJUtilsPlugin.prototype.smartConfig = function (ssid, bssid, password, tastCount, success, error) {
    setTimeout(function () {
        exec(success, error, "SJUtilsPlugin", "smartConfig", [ssid, bssid, password, tastCount]);
    }, 30);
};
SJUtilsPlugin.prototype.cancelSmartConfig = function (success) {
    exec(success, null, "SJUtilsPlugin", "cancelSmartConfig", []);
};
SJUtilsPlugin.prototype.sendBroadCast = function (sendIP, sendMess, success) {
    exec(success, null, "SJUtilsPlugin", "sendBroadCast", [sendIP, sendMess]);
};
SJUtilsPlugin.prototype.receiveBroadCast = function (success) {
    exec(success, null, "SJUtilsPlugin", "receiveBroadCast", []);
};
SJUtilsPlugin.prototype.getPreferredLanguage = function (success) {
    exec(success, null, "SJUtilsPlugin", "getPreferredLanguage", []);
};
SJUtilsPlugin.prototype.showMsg = function (msg) {
    exec(null, null, "SJUtilsPlugin", "showMsg", [msg]);
};
SJUtilsPlugin.prototype.hideMsg = function () {
    exec(null, null, "SJUtilsPlugin", "hideMsg", []);
};
SJUtilsPlugin.prototype.connectWiFi = function (ssid, password, success, error) {
    success(0);
};

if (!window.plugins) {
    window.plugins = {};
}
module.exports = new SJUtilsPlugin();
