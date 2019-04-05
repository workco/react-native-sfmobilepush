import {
  NativeEventEmitter,
  NativeModules,
  PushNotificationIOS,
  Platform
} from "react-native";

const isIOS = Platform.OS === "ios";

const { SFMobilePush } = NativeModules;

const eventConverter = event => {
  if (isIOS) {
    return event;
  } else {
    return JSON.parse(event);
  }
};

const convertListener = listener => event =>
  listener(eventConverter(event));

const notificationEmitter = new NativeEventEmitter(SFMobilePush);

const registerNotificationHandler = listener => {
  if (!isIOS) {
    SFMobilePush.getInitialNotification().then(convertListener(listener));
  }
  return notificationEmitter.addListener(SFMobilePush.notificationEvent, convertListener(listener))
}

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
  checkPermissions,
  registerNotificationHandler
};
