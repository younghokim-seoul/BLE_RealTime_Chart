# RxBle_MVVM
RxBle + MVVM Example





## Info

 *This code is Bluetooth LE MVVM code using RxBle.*  
- **Using**
  - [**RxBle**](https://github.com/Polidea/RxAndroidBle)
  - [Koin](https://github.com/InsertKoinIO/koin)
  - Theme Ref : https://material.io/design/material-studies/shrine.html#components

　  
- If you want to see your ble device like this code preview, ***modify UUID*** in **Constants.kt**  
```Kotlin
//사용자 BLE UUID Service/Rx/Tx
const val SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_COMMAND_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_RESPONSE_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
```

        public static final UUID RXTX_SERVICE_UUID 					= UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
        public static final UUID RX_CHAR_UUID 						= UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
        public static final UUID TX_CHAR_UUID						= UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
        public static final UUID CLIENT_CHARACTERISTIC_CONFIG 		= UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
　  




## Preview
<img src = "https://github.com/DDANGEUN/RxBle_MVVM/blob/main/preview/preview.gif" width="30%">


## Also See
- [**AndroidBluetoothLE_MVVM**](https://github.com/DDANGEUN/AndroidBluetoothLE_MVVM)  
- [**AndroidBluetoothLE_MVVM_Service**](https://github.com/DDANGEUN/AndroidBluetoothLE_MVVM_Service)

