#import "SJUtilsPlugin.h"

@implementation EspTouchDelegateImpl

-(void) onEsptouchResultAddedWithResult: (ESPTouchResult *) result
{
    NSString *inetAddress = [ESP_NetUtil descriptionInetAddr4ByData:result.ipAddrData];
    if (inetAddress == nil) {
        inetAddress = [ESP_NetUtil descriptionInetAddr6ByData:result.ipAddrData];
    }
    NSLog(@"bssid:%@,InetAddress:%@", result.bssid, inetAddress);
}

@end


@implementation SJUtilsPlugin

- (void)getVersionCode:(CDVInvokedUrlCommand*)command
{
    NSString* version = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:version];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)update:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:0];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getWifiSSID:(CDVInvokedUrlCommand*)command
{
    NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
    NSArray *interfaceNames = CFBridgingRelease(CNCopySupportedInterfaces());
    NSDictionary *SSIDInfo;
    for (NSString *interfaceName in interfaceNames) {
        SSIDInfo = CFBridgingRelease(CNCopyCurrentNetworkInfo((__bridge CFStringRef)interfaceName));
        //NSLog(@"%s: %@ => %@", __func__, interfaceName, SSIDInfo);
        BOOL isNotEmpty = (SSIDInfo.count > 0);
        if (isNotEmpty) {
            break;
        }
    }
    [dictionary setObject:[SSIDInfo objectForKey:@"SSID"] forKey:@"ssid"];
    [dictionary setObject:[SSIDInfo objectForKey:@"BSSID"] forKey:@"bssid"];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)smartConfig:(CDVInvokedUrlCommand*)command
{
    [self._condition lock];
    NSString *apSsid = (NSString *)[command.arguments objectAtIndex:0];
    NSString *apBssid = (NSString *)[command.arguments objectAtIndex:1];
    NSString *apPwd = (NSString *)[command.arguments objectAtIndex:2];
    NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
    self._esptouchTask = [[ESPTouchTask alloc]initWithApSsid:apSsid andApBssid:apBssid andApPwd:apPwd andIsSsidHiden:false];
    EspTouchDelegateImpl *esptouchDelegate = [[EspTouchDelegateImpl alloc]init];
    esptouchDelegate.command = command;
    esptouchDelegate.commandDelegate = self.commandDelegate;
    [self._esptouchTask setEsptouchDelegate:esptouchDelegate];
    [self._condition unlock];
    NSArray * esptouchResults = [self._esptouchTask executeForResults:1];
    [self.commandDelegate runInBackground:^{
        dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
        dispatch_async(queue, ^{
            dispatch_async(dispatch_get_main_queue(), ^{
                ESPTouchResult *firstResult = [esptouchResults objectAtIndex:0];
                if (!firstResult.isCancelled)
                {
                    if ([firstResult isSuc])
                    {
                        [dictionary setObject:@(0) forKey:@"retCode"];
                        [dictionary setObject:firstResult.bssid forKey:@"bssid"];
                        NSString *inetAddress = [ESP_NetUtil descriptionInetAddr4ByData:firstResult.ipAddrData];
                        if (inetAddress == nil) {
                            inetAddress = [ESP_NetUtil descriptionInetAddr6ByData:firstResult.ipAddrData];
                        }
                        [dictionary setObject:inetAddress forKey:@"ip"];
                    }else{
                        [dictionary setObject:@(-1) forKey:@"retCode"];
                    }
                }else{
                    [dictionary setObject:@(1) forKey:@"retCode"];
                }
                CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            });
        });
    }];
}

- (void)cancelSmartConfig:(CDVInvokedUrlCommand*)command
{
    [self._condition lock];
    if (self._esptouchTask != nil)
    {
        [self._esptouchTask interrupt];
    }
    [self._condition unlock];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:0];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

//发送广播
-(void)sendBroadCast:(CDVInvokedUrlCommand*)command{

    //NSLog(@" sendBroadCast ==============");
    //实例化
    NSError *error = nil;
    UInt16 port = 1025;//端口
    NSTimeInterval timeout = 10*1000;//发送超时时间
    //NSString *request = [command argumentAtIndex:1];//发送的内容
    NSString *request = (NSString *)[command.arguments objectAtIndex:1];
    NSData *data = [request dataUsingEncoding:NSUTF8StringEncoding];
    NSString *sendIp = [command argumentAtIndex:0];
    if(sendIp == nil || sendIp == NULL){
        NSString *localIP = [self getIPAddress];
        NSRange range = [localIP rangeOfString:@"." options:NSBackwardsSearch];
        sendIp = [[localIP substringWithRange:NSMakeRange(0, range.location)] stringByAppendingString: @".255"];
    }
    if(self.sendSocket){
        [self.sendSocket close];
    }
    self.sendSocket = [[AsyncUdpSocket alloc] initWithDelegate:self];
    [self.sendSocket enableBroadcast:YES error:&error];
    [self.sendSocket bindToPort:0 error:&error];
    if(error){
        NSLog(@"sendSocket bindToPort error : %@", error);
    }
    BOOL isOK = [self.sendSocket sendData:data toHost:sendIp port:port withTimeout:timeout tag:100];
    
    if(isOK){
        //udp请求成功
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:0];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        //NSLog(@"send ok ==============%@",data);
    }else{
        //udp请求失败
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:-1];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        //NSLog(@"send error ==============");
    }
}

//接收广播
-(void)receiveBroadCast:(CDVInvokedUrlCommand*)command{
    //NSLog(@"receiveBroadCast ==============");
    self._command = command;
    NSError *error = nil;
    UInt16 port = 1034;//端口
    NSTimeInterval timeout = 20*1000;//发送超时时间
    if(self.recvSocket){
        [self.recvSocket close];
    }
    self.recvSocket = [[AsyncUdpSocket alloc] initWithDelegate:self];
    [self.recvSocket bindToPort:port error:&error];
    if(error) {
        NSLog(@"接收绑定错误:%@", error);
    }
    // 接收数据包 -1表示一直等接收, tag 200表示当前接收数据包
    [self.recvSocket receiveWithTimeout:timeout tag:200];
    // 上面这个函数不会等待,不会阻塞blocked
}

- (BOOL) onUdpSocket:(AsyncUdpSocket *)sock didReceiveData:(NSData *)data withTag:(long)tag fromHost:(NSString *)host port:(UInt16)port
{
    // data 对方出过来的数据
    // tag == 200
    // host从哪里来数据 ip
    // port 对象的端口
    
    NSLog(@"recv data from %@:%d", host, port);
    if (tag == 200) {
        NSString *recvData = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        NSLog(@"recv data : %@", recvData);
        // 此处处理接受到的数据
        if(recvData != nil && [recvData length]>1){
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:recvData];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self._command.callbackId];
        }else{
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{}"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self._command.callbackId];
        }
        //[sock close];
    }
    return YES;
}

- (void)onUdpSocket:(AsyncUdpSocket *)sock didNotReceiveDataWithTag:(long)tag dueToError:(NSError *)error
{
    NSLog(@"didNotReceiveDataWithTag = %@", error);
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{}"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self._command.callbackId];
}

// Get IP Address
- (NSString *)getIPAddress {
    NSString *address = @"error";
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;
    // retrieve the current interfaces - returns 0 on success
    success = getifaddrs(&interfaces);
    if (success == 0) {
        // Loop through linked list of interfaces
        temp_addr = interfaces;
        while(temp_addr != NULL) {
            if(temp_addr->ifa_addr->sa_family == AF_INET) {
                // Check if interface is en0 which is the wifi connection on the iPhone
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                    // Get NSString from C String
                    address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                }
            }
            temp_addr = temp_addr->ifa_next;
        }
    }
    // Free memory
    freeifaddrs(interfaces);
    return address;
}

- (void)getPreferredLanguage:(CDVInvokedUrlCommand*)command
{
    NSString* lang = [[NSLocale preferredLanguages] objectAtIndex:0];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:lang];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
