# Fermata Auto - BYD & Araç İçi Özel Sürüm (v8 - Nihai Düzeltmeler)

Bu paket, **BYD Seal U DM-i** (ve diğer Android tabanlı araç multimedya sistemlerinde) karşılaşılan siyah ekran, ses duraklaması ve başlangıçta boot etmeme sorunlarını tamamen ortadan kaldırmak için optimize edilmiştir.

---

## 🚀 v8 Sürümünde Yapılan Kritik Düzeltmeler

### 1. Siyah Ekran / Sadece Arkada Ses Çalma Sorunu Çözüldü
* **Önceki Sorun:** Otomatik başlatma yapıldığında video arkada ses olarak çalıyor, ekranda ise görüntü siyah kalıyor veya klasör listesinde bekliyordu (dokunana kadar görüntü gelmiyordu).
* **v8 Çözümü:** Başlatma anında video oynatıcı yüzeyi (`BodyLayout.Mode.VIDEO` ve `setVideoMode(true, vv)`) gecikmesiz olarak doğrudan tam ekrana geçirilir. Donanım video dekoderi yüzeyi hazır bulduğu için video anında görüntüyle birlikte başlar.

### 2. MicroSD Kart ile Hızlı Başlatma (2 Saniye)
* **Önceki Sorun:** Sabit 40 saniye veya 0-150 saniye gecikme ayarı sistemi bekletiyor ve Android'in arka plan engelleyicisine takılıyordu.
* **v8 Çözümü:** Araç açıldığında (`BOOT_COMPLETED`), takılı olan MicroSD kartın hazır olduğu (`Environment.MEDIA_MOUNTED`) saniyede algılanır ve 40 saniye yerine yalnızca **2 saniye** içinde (sistemin oturması için) oynatıcı güvenle açılır. USB takılmışsa veya gecikirse yedek zamanlayıcı çalışmaya devam eder.

### 3. Android 10-14 Arka Planda Başlatma Garantisi (FullScreenIntent)
* **Önceki Sorun:** Hem Samsung telefonlarda (Android 14) hem de BYD DiLink (Android 10/12) sistemlerinde arka plandan Activity başlatma işletim sistemi tarafından sessizce engelleniyordu (`Background activity start blocked`).
* **v8 Çözümü:** `BootReceiver`'a yüksek öncelikli `FullScreenIntent` bildirim köprüsü eklendi. Sistem kısıtlaması olsa dahi işletim sistemi Fermata ekranını ön plana getirmek zorunda kalır.

### 4. BYD Karşılama Sesi ve Sensör Çakışması Giderildi (Akıllı Odak Yönetimi)
* **Önceki Sorun:** Araç açılırken çalan karşılama melodisi veya park sensörü sesi Fermata'ya `AUDIOFOCUS_LOSS` gönderiyor, bu da müziğin durmasına ve ekrandan elle Play'e basma zorunluluğuna neden oluyordu. Ayrıca önceki sürümdeki ses kısma kodu sistem sesini bozuyordu.
* **v8 Çözümü:** 
  * Android sistem sesine (`setStreamVolume`) asla dokunulmaz, araç fabrika sesi korunur.
  * Karşılama melodisi gibi ilk açılış sesleri bittiğinde (3.5 saniyelik akıllı kontrol) müzik otomatik olarak çalmaya devam eder.
  * Geri vitese (360° kamera) geçildiğinde video temiz bir şekilde duraklar, araç tekrar D veya P vitesine alındığında video otomatik devam eder.

---

## 📂 Değiştirilen Dosyalar

1. `fermata/src/main/java/me/aap/fermata/BootReceiver.java`: Hızlı MicroSD algılama + FullScreenIntent arka plan başlatma garantisi.
2. `fermata/src/main/java/me/aap/fermata/ui/activity/MainActivityDelegate.java`: Otomatik VideoLayout aktivasyonu (siyah ekran sonu) + güvenli ses odağı yönetimi.
3. `fermata/src/main/AndroidManifest.xml`: `AUTOSTART_LAUNCH` intent-filter kaydı.
4. `.github/workflows/build-car-apk.yml`: Bulut üzerinde ARM64 APK derleme iş akışı.
