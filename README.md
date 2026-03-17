# Tawari Emergency App - تطبيق تواري للطوارئ

## الوصف
تطبيق Android أصلي احترافي لنظام الطوارئ - يتصل بشبكات القرى (A و B)

### ✨ المميزات:
- ❤️ **زر SOS** - بلاغ طوارئ فوري بضغطة واحدة
- 🔥 **أنواع الطوارئ** - حريق، إصابة، تهديد، حادث، احتجاز
- 📍 **تحديد الموقع** - GPS تلقائي
- 📝 **بلاغاتي** - عرض حالة البلاغات
- 💬 **التواصل** - محادثة مباشرة مع عملاء الطوارئ
- ℹ️ **المعلومات** - إرشادات السلامة
- 🔔 **إشعارات** - تنبيهات فورية
- 🌐 **دعم متعدد** - قرية A و قرية B

### 🎨 التصميم:
- خطوط عربية عصرية (Cairo)
- تصميم داكن احترافي (Dark Theme)
- أزرار متدرجة بتأثيرات جميلة
- بطاقات مستديرة عصرية
- ألوان متناسقة

### 📡 الشبكات المدعومة:

#### قرية A:
| العقدة | الشبكة | IP |
|--------|--------|----|
| Emergency 1-A | Tawari-Qarya-A | 192.168.10.1 |
| Emergency 2-A | Tawari-Qarya-A2 | 192.168.11.1 |

#### قرية B:
| العقدة | الشبكة | IP |
|--------|--------|----|
| Emergency 1-B | Tawari-Qarya-B | 192.168.12.1 |
| Emergency 2-B | Tawari-Qarya-B2 | 192.168.13.1 |

---

## 🛠️ طريقة البناء

### المتطلبات:
- Android Studio Hedgehog (2023.1.1) أو أحدث
- Android SDK 34
- JDK 17

### خطوة مهمة - إضافة الخطوط:

1. حمل خط Cairo من [Google Fonts](https://fonts.google.com/specimen/Cairo)
2. ضع الملفات في `app/src/main/res/font/`:
   - `cairo_regular.ttf`
   - `cairo_semibold.ttf` (Medium)
   - `cairo_bold.ttf`

### البناء:

```bash
# فتح المشروع في Android Studio
# أو من سطر الأوامر:

cd TawariApp

# بناء APK للتجربة
./gradlew assembleDebug

# بناء APK للإنتاج
./gradlew assembleRelease
```

### موقع ملف APK:
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

---

## 📁 هيكل المشروع

```
TawariApp/
├── app/src/main/
│   ├── java/com/tawari/emergency/
│   │   ├── TawariApp.kt              # Application class
│   │   ├── models/Models.kt          # Data classes
│   │   ├── network/ApiClient.kt      # HTTP client
│   │   ├── services/                 # Notifications
│   │   ├── ui/                       # Activities & Adapters
│   │   └── utils/                    # Location & Preferences
│   ├── res/
│   │   ├── layout/                   # UI layouts
│   │   ├── drawable/                 # Icons & shapes
│   │   ├── values/                   # Colors, strings, styles
│   │   ├── font/                     # Arabic fonts (Cairo)
│   │   └── animator/                 # Button animations
│   └── AndroidManifest.xml
├── build.gradle.kts
└── README.md
```

---

## 🔐 الأذونات

| الإذن | السبب |
|-------|-------|
| `INTERNET` | الاتصال بالشبكة |
| `ACCESS_NETWORK_STATE` | التحقق من حالة الشبكة |
| `ACCESS_WIFI_STATE` | معرفة اسم الشبكة |
| `ACCESS_FINE_LOCATION` | تحديد الموقع GPS |
| `POST_NOTIFICATIONS` | الإشعارات |

---

## 📱 الإصدار: 1.0.0

التطبيق يعمل فقط عند الاتصال بشبكة القرية (Tawari-Qarya)
