import { NativeModules, PushNotificationIOS, Platform } from "react-native";

const isIOS = Platform.OS === "ios";

const { SFMobilePush } = NativeModules;

const promiseIOSCheckPermissions = () =>
  new Promise(resolve => {
    PushNotificationIOS.checkPermissions(resolve);
  });

const checkPermissions = async () => {
  if (isIOS) {
    const { alert, badge, sound } = await promiseIOSCheckPermissions();
    return alert || badge || sound;
  }
  return SFMobilePush.checkPermissions();
};

export default {
  ...SFMobilePush,
  checkPermissions
};
