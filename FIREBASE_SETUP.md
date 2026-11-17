# Firebase setup (Google sign-in + word list sync)

অ্যাপ খুললে Google দিয়ে লগইন লাগে। লগইনের পর Cloud Firestore থেকে শব্দ নামিয়ে ফোনে সেভ করে রাখে (Spark ফ্রি প্ল্যানে চলে, কার্ড লাগে না),
তাই পরে অফলাইনেও সব শব্দ পড়া যায়।

## এককালীন সেটআপ

1. **Firebase প্রজেক্ট:** https://console.firebase.google.com এ নতুন প্রজেক্ট বানান।
2. **Android অ্যাপ যোগ করুন:** package name `com.vocabulary.app`। SHA-1 পরের ধাপে দেবেন।
3. **Authentication:** Build > Authentication > Get started > Sign-in method > **Google** চালু করুন।
4. **SHA-1:** একবার GitHub Actions চালান। রান শেষে **Summary** পেজে "Signing certificate SHA-1" দেখাবে।
   সেটা Project settings > Your apps > SHA certificate fingerprints এ যোগ করুন।
   এরপর `google-services.json` আবার ডাউনলোড করুন (এতে Google sign-in-এর web client id থাকে)।
   > জরুরি: সাইনিং কী বদলালে SHA-1 বদলে যায় এবং লগইন বন্ধ হয়ে যায়। তাই README-র নির্দেশ মতো
   > স্থায়ী keystore Secrets-এ দিন (`KEYSTORE_BASE64` ইত্যাদি), আর SHA-1 ওটারই দিন।
5. **google-services.json GitHub Secret-এ দিন:** `base64 -w0 google-services.json` এর ফলাফল
   repo > Settings > Secrets and variables > Actions > **GOOGLE_SERVICES_JSON** নামে সেভ করুন।
6. **Firestore ডেটাবেজ:** Build > Firestore Database > Create database। Standard edition, Production mode, লোকেশন কাছের একটা (যেমন `asia-south1`)।
   তারপর Rules ট্যাবে `firebase/firestore.rules` এর লেখা পেস্ট করে Publish করুন।
7. **Service account key (শুধু আপলোডের জন্য):** Project settings > Service accounts > Generate new private key।
   ফাইলটা `scripts/serviceAccountKey.json` নামে রাখুন। **এটা কাউকে দেবেন না, GitHub-এ তুলবেন না** (`.gitignore`-এ দেওয়া আছে)।
8. **শব্দ আপলোড:** Node.js (18+) লাগবে।

```
cd scripts
npm install
node upload-words.mjs --dry   # শুধু পরীক্ষা, কিছু আপলোড হয় না
node upload-words.mjs         # আসল আপলোড
```

## ডেটা কীভাবে সাজানো থাকে

- `meta/words` : `{ version, stamp, total, packs: [...] }`
- `packs/{id}` : ক্যাটাগরি (`type`) অনুযায়ী এক বা একাধিক প্যাক, যেমন `group_verb_1`, `idiom_1`, `preposition_1`।
  প্রতিটি প্যাকে শব্দগুলো একটা JSON টেক্সট হিসেবে থাকে (সর্বোচ্চ ৩০০ শব্দ বা ~৭০০ KB)।

## নতুন শব্দ দিতে চাইলে

1. `app/src/main/assets/words.json` (বা অন্য ফাইল) এ শব্দ যোগ করুন, `version` বাড়ান।
2. `node upload-words.mjs` আবার চালান।
3. অ্যাপ পরের বার খুললে (অথবা Settings > Sync words চাপলে) নতুন তালিকা নামিয়ে নেয়।

- প্রতিটি শব্দের `id` একই রাখুন, তাহলে আগের প্রগ্রেস, ফেভারিট আর ভুলের তালিকা থেকে যাবে। ফাইলে যে শব্দ নেই সেটা অ্যাপ থেকে মুছে যাবে।
- একবার Firestore থেকে নামালে অ্যাপে বান্ডেল করা `words.json` আর কখনো ইম্পোর্ট হয় না, ওটা শুধু প্রথম চালুর ব্যাকআপ।
- অ্যাপ আগে শুধু `meta/words` পড়ে (১টা রিড)। `stamp` না বদলালে আর কিছু নামায় না, তাই ফ্রি রিড লিমিট নিয়ে চিন্তা নেই।

## Admin login (অ্যাপ থেকেই words.json আপডেট)

Settings > **Admin login** থেকে `words.json` ইম্পোর্ট করলে শব্দগুলো ডাটাবেজে (Firestore + ফোনের Room) ঢোকে।
এটা **Cumulative**: ফাইলের নতুন `id` যোগ হয়, একই `id` থাকলে আপডেট হয়, ফাইলে নেই এমন শব্দ মোছে না।
ইম্পোর্টের আগে কতগুলো New / Updated / Unchanged হবে দেখিয়ে নিশ্চিত করতে বলে। Export দিয়ে পুরো তালিকার ব্যাকআপ নেওয়া যায়।

সেটআপ (এককালীন):
1. GitHub repo > Settings > Secrets > Actions এ **ADMIN_EMAILS** নামে আপনার Gmail দিন (একাধিক হলে কমা দিয়ে)।
   লোকাল বিল্ডে: `gradle assembleDebug -PadminEmails=you@gmail.com`
2. `firebase/firestore.rules` এ `['you@gmail.com']` জায়গায় একই Gmail বসিয়ে Firebase Console > Firestore > Rules এ **Publish** করুন।
   (আসল সুরক্ষা এই rules; অ্যাপের ভেতরের তালিকা শুধু বোতাম দেখানো/লুকানোর জন্য।)
3. ওই Gmail দিয়ে অ্যাপে লগইন করুন, Settings > Admin login খুলুন।
