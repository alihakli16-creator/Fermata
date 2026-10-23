# Fermata Auto - BYD & Araç İçi Özel Sürüm (Car Edition)

Bu proje, **Fermata Auto** açık kaynak medya oynatıcısının araç multimedya sistemlerinde (özellikle **BYD Seal U DM-i**, Atto 3, Dolphin, Han, Tang ve Android tabanlı araç ünitelerinde) orijinal fabrika çıkışı bir medya oynatıcı gibi çalışması için optimize edilmiş tam paketidir.

---

## 🚗 Eklenen Özel Araç Özellikleri

1. **USB ve MicroSD Kartı Aynı Anda Kullanma & Otomatik Başlatma:**
   * Hem USB bellek hem de araçtaki MicroSD kart aynı anda kullanılabilir.
   * Fermata içindeki **Klasörler (Folders)** sekmesinden hem MicroSD kartın hem de USB belleğin dizinleri kütüphaneye eklenebilir; tüm video ve müzikler tek bir yerde listelenir.
   * En son hangi ortamdaki (USB veya MicroSD) videoyu/müziği oynattıysanız, sistem açılışında (`ACTION_MEDIA_MOUNTED`) o cihazın bağlanmasıyla birlikte otomatik başlatma devreye girer.
   * USB veya MicroSD kart araç çalışırken çıkarılırsa (`ACTION_MEDIA_UNMOUNTED` / `ACTION_MEDIA_EJECT`) oynatıcı hata vermeden temiz bir şekilde kendini duraklatır.

2. **Hatırlanan İçeriği Baştan Başlatma ve Donma Koruması (Anti-Stutter):**
   * Araç ilk açıldığında videonun ortasındaki bir saniyeye seek (atlama) yaparken araç donanım dekoderinin yaşadığı 3-5 saniyelik takılma ve donmalar tamamen engellendi.
   * Uygulama en son hangi videoda/mp3'te kaldığınızı hatırlar ancak oynatmaya doğrudan **00:00** başından başlar.
   * Parça ve video geçişlerinde tertemiz başlar.

3. **Akıllı Ses Kısma (Audio Ducking):**
   * Geri vitese takıldığında (360° park kamerası ve park sensörleri açıldığında) veya navigasyon (Yandex, Google Haritalar) sesli yönlendirme yaptığında video duraklatılmaz; medya sesi otomatik olarak %25 seviyesine kısılır.
   * Manevra bittiğinde (vites tekrar D/P konumuna alındığında) veya navigasyon anonsu bittiğinde ses anında eski seviyesine döner.
   * Telefon görüşmelerinde ise mikrofona ses gitmemesi için video duraklatılır ve çağrı bitince kaldığı saniyeden devam eder.

4. **USB / MicroSD Bağlantı Olayı ile Sıfır Gecikmeli Başlatma (`ACTION_MEDIA_MOUNTED`):**
   * Depolama birimi sisteme bağlandığı milisaniyede algılanır ve video anında başlatılır.
   * 40 saniyelik sabit bekleme süresi sadece yedek olarak tutulur; ortam hazır olur olmaz bekleme iptal edilir.

5. **Kontak / Ekran Kapanınca Otomatik Duraklatma (ACC OFF):**
   * Araç stop edildiğinde, şarj kesildiğinde veya ekran karardığında son saniye hafızaya alınarak oynatıcı duraklatılır (akü tüketimi sıfıra iner).

6. **Kontak / Ekran Açılınca Devam Ettirme (ACC ON / Standby Resume):**
   * Araç uykudan uyandığında veya ekran açıldığında, soğuk açılış (cold boot) beklenmeden video kaldığı videonun başından oynamaya devam eder.

7. **Genişletilmiş Depolama İzinleri:**
   * Android 11 ve 12 (BYD DiLink) sistemlerinde hem USB hem MicroSD karttaki tüm klasörlere tam erişim için `MANAGE_EXTERNAL_STORAGE` ve `requestLegacyExternalStorage` desteği eklendi.

---

## 📦 GitHub'da APK Nasıl Oluşturulur? (3 Adım)

Bu paketteki GitHub Actions iş akışı (`.github/workflows/build-car-apk.yml`) sayesinde bilgisayarınızda Android Studio veya NDK kurulu olmasına gerek kalmadan bulutta doğrudan APK derleyebilirsiniz.

### Adım 1: Dosyaları Repoya Aktarın
İndirdiğiniz bu zip dosyasının içindeki tüm dosya ve klasörleri (`.github`, `fermata`, `settings.gradle` vb.) GitHub'daki Fermata reponuzun ana dizinine açın ve GitHub'a pushlayın:
```bash
git add .
git commit -m "feat: car edition with usb + microsd dual storage and audio ducking"
git push origin master
```

### Adım 2: Derlemeyi Takip Edin
* GitHub'da reponuzun üst kısmındaki **Actions** sekmesine tıklayın.
* **Build Car APK** iş akışının otomatik olarak başladığını göreceksiniz (ortalama 3-5 dakika sürer).

### Adım 3: APK'yı İndirin
* Derleme yeşil onay işaretiyle tamamlandığında iş akışına tıklayın.
* Sayfanın altındaki **Artifacts** bölümünden:
  * **fermata-car-arm64** (BYD Seal U DM-i ve modern 64-bit Android üniteleri için)
  * **fermata-car-arm** (32-bit Android üniteleri için)
  dosyalarından aracınıza uygun olanı indirin.
* İndirdiğiniz zip içindeki `.apk` dosyasını bir USB belleğe atıp araç ekranından yükleyin.
