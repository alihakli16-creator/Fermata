# Fermata Auto - Araç İçi Entegrasyon İyileştirmeleri (v7.3)

Bu paket, Fermata Auto uygulamasının araç içi multimedya (özellikle **BYD Seal U DM-i** DiLink ve Android Automotive/head unit) sistemlerinde fabrika çıkışı bir medya oynatıcı gibi çalışması için geliştirilen yeni özellikleri içerir.

---

## Eklenen Özellikler

### 1. Hem USB Hem MicroSD Kart Eşzamanlı Kullanım ve Otomatik Başlatma
* **Nasıl Çalışır:** Fermata Auto dosya kütüphanesine hem USB belleğin (`/storage/XXXX-XXXX` vb.) hem de MicroSD kartın (`/storage/sdcard1` vb.) klasörleri eklenebilir. Android çekirdeği hem USB hem MicroSD mount işlemlerinde `Intent.ACTION_MEDIA_MOUNTED` sinyali fırlatır.
* **Sonuç:** Hangi ortamdan video/müzik dinliyorsanız sistem o ortamı tanır ve başlatır. Ayrıca ortam araç çalışırken çıkarılırsa (`ACTION_MEDIA_UNMOUNTED` / `ACTION_MEDIA_EJECT`) oynatıcı çökmeden kendini duraklatır.

### 2. Hatırlanan İçeriği Baştan Başlatma ve Donma Koruması (Anti-Stutter & Clean Start)
* **Nasıl Çalışır:** Araç açıldığında videonun ortasındaki bir saniyeye seek yaparken USB/MicroSD okuma gecikmesi ve dekoderin ana kare (I-frame) araması nedeniyle oluşan 3-5 saniyelik görüntü donması engellendi. Uygulama en son hangi videoda/şarkıda kaldığını hatırlar, ancak oynatmaya doğrudan **00:00** başından başlar.
* **Sonuç:** Takılma ve gecikme olmadan anında ve pürüzsüz başlangıç.

### 3. Akıllı Ses Kısma ve Geri Vites Yönetimi (Audio Ducking)
* **Nasıl Çalışır:** `AudioManager.OnAudioFocusChangeListener` entegrasyonuyla geçici ses kesintileri (`AUDIOFOCUS_LOSS_TRANSIENT` ve `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`) dinlenir.
* **Sonuç:** 
  * Geri vitese takıldığında (360° park kamerası ve sensörler açıldığında) video duraklatılmaz, ses seviyesi otomatik olarak %25'e kısılır (audio ducking). Manevra bitip D/P konumuna geçince ses anında eski seviyesine döner.
  * Navigasyon (Yandex/Google Haritalar) sesli yönlendirme yaptığında aynı şekilde ses kısılır, anons bittiğinde hemen geri yükselir.
  * Telefon görüşmelerinde ise mikrofona müzik sesi gitmemesi için video duraklatılır ve görüşme sonlandığında kaldığı saniyeden devam eder.

### 4. Araç Uyku ve Uyanma Desteği (ACC ON / Standby Resume)
* **Nasıl Çalışır:** Ekranın uyanmasını (`ACTION_SCREEN_ON`) ve güç beslemesini (`ACTION_POWER_CONNECTED`) dinler.
* **Sonuç:** Araca binip kontağı açtığınızda uygulama kendiliğinden en son videoyu baştan tertemiz başlatır.

### 5. Kontak/Ekran Kapanınca Otomatik Duraklatma (ACC OFF)
* **Nasıl Çalışır:** Araç stop ettiğinde, şarj kesildiğinde (`ACTION_POWER_DISCONNECTED`) veya ekran karardığında (`ACTION_SCREEN_OFF`) anlık pozisyon hafızaya alınır ve oynatıcı duraklatılır.
* **Sonuç:** Araç kilitliyken arka planda video oynamaya devam etmez, akü tükenmez.

---

## Değiştirilen Dosyalar

1. `fermata/src/main/AndroidManifest.xml`: `MANAGE_EXTERNAL_STORAGE` ve `WRITE_EXTERNAL_STORAGE` izinleri, `BootReceiver` için `ACTION_MEDIA_MOUNTED` ve `ACTION_MEDIA_UNMOUNTED` intent-filter'ları eklendi.
2. `fermata/src/main/java/me/aap/fermata/BootReceiver.java`: USB ve MicroSD mount tespiti ve zamanlayıcı iptal mantığı eklendi.
3. `fermata/src/main/java/me/aap/fermata/ui/activity/MainActivityPrefs.java`: `AUTOPLAY_FROM_BEGINNING`, `AUTO_PAUSE_ON_ACC_OFF`, `AUTORESUME_ON_ACC_ON` ve `CAR_AUDIO_FOCUS_MANAGE` ayar anahtarları eklendi.
4. `fermata/src/main/java/me/aap/fermata/ui/activity/MainActivityDelegate.java`: 00:00 başlangıç konumu mantığı, USB/MicroSD çıkarılma duraklatması, araç durum dinleyicisi (`carStateReceiver`) ve akıllı ses kısma yöneticisi (`carAudioFocusListener`) entegre edildi.
5. `fermata/src/main/java/me/aap/fermata/ui/fragment/SettingsFragment.java`: Ayarlar arayüzüne yeni özelliklerin açma/kapatma anahtarları eklendi.
6. `fermata/src/main/res/values/strings.xml` & `values-tr/strings.xml`: Türkçe ve İngilizce arayüz metinleri güncellendi.
