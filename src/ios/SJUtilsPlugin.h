#import "ESPTouchTask.h"
#import "ESP_NetUtil.h"
#import "ESPTouchResult.h"
#import "ESPTouchDelegate.h"
#import <Cordova/CDVPluginResult.h>
#import <SystemConfiguration/CaptiveNetwork.h>
#import <Cordova/CDVPlugin.h>
#import "AsyncUdpSocket.h"

//listener to get result
@interface EspTouchDelegateImpl : NSObject<ESPTouchDelegate>

@property (nonatomic, strong) CDVInvokedUrlCommand *command;
@property (nonatomic, weak) id <CDVCommandDelegate> commandDelegate;

@end


@interface SJUtilsPlugin : CDVPlugin<AsyncUdpSocketDelegate>

@property (nonatomic, strong) CDVInvokedUrlCommand *_command;
@property (nonatomic, strong) NSCondition *_condition;
// to cancel ESPTouchTask when
@property (atomic, strong) ESPTouchTask *_esptouchTask;
@property (nonatomic, strong) AsyncUdpSocket *recvSocket;
@property (nonatomic, strong) AsyncUdpSocket *sendSocket;

//获取版本Code
- (void)getVersionCode:(CDVInvokedUrlCommand*)command;

- (void)update:(CDVInvokedUrlCommand*)command;

//获取网络WiFi-SSID
- (void)getWifiSSID:(CDVInvokedUrlCommand*)command;

- (void)smartConfig:(CDVInvokedUrlCommand*)command;

- (void)cancelSmartConfig:(CDVInvokedUrlCommand*)command;

//发送广播
-(void)sendBroadCast:(CDVInvokedUrlCommand*)command;

//接受广播
-(void)receiveBroadCast:(CDVInvokedUrlCommand*)command;

-(void)getPreferredLanguage:(CDVInvokedUrlCommand*)command;
@end
