# Fermata Auto - Araç İçi Entegrasyon İyileştirmeleri (v7)

Bu paket, Fermata Auto uygulamasının araç içi multimedya (BYD DiLink ve Android Automotive/head unit) sistemlerinde fabrika çıkışı bir medya oynatıcı gibi çalışması için geliştirilen yeni özellikleri içerir.

## Eklenen Özellikler

### 1. Araç Uyku ve Uyanma Desteği (ACC ON / Standby Resume)

* **Nasıl Çalışır:** Araçlar çoğu zaman tamamen kapanmaz, akü tasarrufu için derin uykuya (standby/suspend) geçer. Sistem ekranın uyanmasını (`ACTION_SCREEN_ON`) ve güç beslemesini (`ACTION_POWER_CONNECTED`) dinler.

* **Sonuç:** Araca binip kontağı açtığınızda uygulama kendiliğinden kaldığı yerden çalmaya devam eder.

### 2. USB Bağlantı Olayı ile Sıfır Gecikmeli Başlatma (`ACTION_MEDIA_MOUNTED`)

* **Nasıl Çalışır:** Önceki sürümdeki sabit 40 saniyelik gecikme yerine doğrudan Android'in `ACTION_MEDIA_MOUNTED` yayını yakalanır.

* **Sonuç:** USB bellek sisteme bağlandığı milisaniyede 40 saniyelik sayaç iptal edilir ve video anında başlatılır. USB takılı değilse veya sinyal gecikirse 40 saniyelik yedek alarm güvenli bir şekilde devreye girer.

### 3. Kontak/Ekran Kapanınca Otomatik Duraklatma (ACC OFF)

* **Nasıl Çalışır:** Araç stop ettiğinde, şarj kesildiğinde (`ACTION_POWER_DISCONNECTED`) veya ekran karardığında (`ACTION_SCREEN_OFF`) anlık pozisyon hafızaya alınır ve oynatıcı duraklatılır.

* **Sonuç:** Araç kilitliyken arka planda video oynamaya devam etmez, akü tükenmez ve içerik kaçırılmaz.

### 4. Akıllı Ses Odağı ve Geri Vites Yönetimi (Audio Focus)

* **Nasıl Çalışır:** `AudioManager.OnAudioFocusChangeListener` entegrasyonuyla geçici ses kesintileri (`AUDIOFOCUS_LOSS_TRANSIENT` ve `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`) dinlenir.

* **Sonuç:**

  * Geri vitese takıldığında (360° park kamerası ve sensörler açıldığında) video otomatik olarak duraklatılır.

  * Park işlemi bitip araç tekrar D veya P konumuna alındığında video kaldığı saniyeden otomatik devam eder.

  * Navigasyon (Yandex/Google Haritalar) sesli yönlendirme yaptığında veya telefon çağrısı geldiğinde aynı akıllı duraklatma ve geri sürdürme mekanizması çalışır.

## Değiştirilen Dosyalar

1. `fermata/src/main/AndroidManifest.xml`: `BootReceiver` için `ACTION_MEDIA_MOUNTED` intent-filter ve veri şeması eklendi.

2. `fermata/src/main/java/me/aap/fermata/BootReceiver.java`: USB mount tespiti ve zamanlayıcı iptal mantığı eklendi.

3. `fermata/src/main/java/me/aap/fermata/ui/activity/MainActivityPrefs.java`: Yeni araç içi ayar anahtarları (`AUTO_PAUSE_ON_ACC_OFF`, `AUTORESUME_ON_ACC_ON`, `CAR_AUDIO_FOCUS_MANAGE`) tanımlandı.

4. `fermata/src/main/java/me/aap/fermata/ui/activity/MainActivityDelegate.java`: Araç durum dinleyicisi (`carStateReceiver`) ve akıllı ses odağı dinleyicisi (`carAudioFocusListener`) entegre edildi.

5. `fermata/src/main/java/me/aap/fermata/ui/fragment/SettingsFragment.java`: Ayarlar arayüzüne yeni özelliklerin açma/kapatma anahtarları eklendi.

6. `fermata/src/main/res/values/strings.xml` & `values-tr/strings.xml`: Türkçe ve İngilizce arayüz metinleri eklendi.