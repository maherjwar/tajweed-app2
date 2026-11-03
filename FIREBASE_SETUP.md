# إعداد تسجيل الدخول بحساب Google (مرة واحدة)

## بصمة التوقيع (SHA-1) للمفتاح الثابت المرفق في المشروع
B7:6F:59:29:02:9C:10:A2:8C:C5:5E:80:08:F8:9D:1D:F5:69:F7:16

## قواعد Firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
