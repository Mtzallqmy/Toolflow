# ToolFlow

تطبيق أندرويد خفيف يفتح مشروع Google Flow مباشرة داخل **Chrome Custom Tab**، بحيث يستخدم جلسة Google المحفوظة في Chrome بدل محاولة تسجيل الدخول داخل WebView.

## المشروع الافتراضي

`https://flow.google.com/project/460d7b3e-e421-4f78-a019-82630bc50fa5`

## طريقة تسجيل الدخول

1. سجّل الدخول إلى حساب Google المطلوب في تطبيق Chrome على الهاتف.
2. افتح ToolFlow؛ سيفتح المشروع مباشرة داخل Chrome Custom Tab باستخدام جلسة Chrome نفسها.
3. عند الرجوع إلى ToolFlow تظهر أزرار لإعادة فتح المشروع أو فتح صفحة Flow الرئيسية.

لا يطلب ToolFlow بريد Google أو كلمة المرور، ولا يقرأ الكوكيز أو رموز الجلسة. المصادقة والرفع والتنزيل والكاميرا والميكروفون تتولاها بيئة Chrome الآمنة.

> قد يظهر شريط Chrome صغير أعلى الصفحة. لا يمكن إخفاؤه كليًا لأن نطاق `flow.google.com` مملوك لـ Google ولا يمكن ربطه بالتطبيق كتطبيق ويب موثوق TWA.

## المزايا

- Android 8.0 (API 26) وما فوق.
- فتح مشروع Flow تلقائيًا عند تشغيل التطبيق.
- استخدام جلسة Chrome المحفوظة لتفادي فشل تسجيل Google داخل WebView.
- تفضيل Chrome تلقائيًا، مع الرجوع إلى المتصفح الافتراضي إذا لم يكن مثبتًا.
- دعم روابط `flow.google.com` الواردة إلى التطبيق.
- واجهة رجوع بسيطة لفتح المشروع أو Flow الرئيسية.
- لا توجد صلاحيات حساسة يطلبها التطبيق نفسه.

## 64-bit

التطبيق مكتوب بالكامل بـ Java ولا يحتوي مكتبات Native (`.so`). ملف APK واحد Universal يعمل على أجهزة ARM64 وx86_64، وكذلك الأجهزة الأقدم المدعومة، ومتوافق مع متطلبات 64-bit.

## البناء

```bash
gradle assembleDebug
```

الناتج: `app/build/outputs/apk/debug/app-debug.apk`

كما يبني GitHub Actions ملفًا باسم `ToolFlow-Android8Plus-universal.apk` عند كل push إلى `main`.

## الإصدار

- Version: 1.1.0
- Package: `com.mtzallqmy.toolflow`
