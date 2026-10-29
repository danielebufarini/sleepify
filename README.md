# Sleepify

App nativa Android e iOS che porta su mobile il calcolatore di `/Users/daniele/Downloads/calcolatore-sonno.html`.

Riprende le scelte tecnologiche del checkout locale di **Trenify**, consultato in sola lettura:

| Ambito | Tecnologia |
| --- | --- |
| Logica e stato | Kotlin Multiplatform 2.4.20, coroutines/StateFlow 1.11.0 |
| Componenti e ripristino | Decompose 3.5.0, Essenty StateKeeper, Kotlin Serialization 1.12.0 |
| Android | Compose 1.12.2, Material 3, Activity 1.13.0, Lifecycle 2.11.0 |
| iOS | SwiftUI, Swift 6, SKIE 0.10.15 |
| Build | Gradle 9.6.0, AGP 9.5.0, JDK 21 |
| Versioni minime | Android 12 / API 31; iOS 27; iPhone e iPad ARM64 |

La dimensione dell'app richiede un solo modulo condiviso `shared/app`, oltre ai due host nativi. La sessione Kotlin è l'unica autorità per gli input e i risultati. SwiftUI osserva StateFlow tramite SKIE; Compose lo osserva con il lifecycle Android. Non servono rete, backend, Ktor o SQLDelight per questo calcolatore offline.

## Funzioni

- Calcolare quando andare a letto partendo dall'ora di risveglio, oppure quando svegliarsi partendo dall'ora di andare a letto.
- Selettore orario nativo, formato 24 ore.
- Tempo per addormentarsi da 0 a 45 minuti, a passi di 5.
- Cicli minimi da 2 a 5 e massimi da 4 a 8; il massimo viene alzato al minimo se necessario, come nell'HTML.
- Cicli di 90 minuti, risultati immediati e ordinati nel tempo anche oltre la mezzanotte, durata effettiva del sonno e giorno relativo all'orario di riferimento.
- Copia dell'orario negli appunti del sistema.
- Interfaccia scura, italiano e inglese, testo scalabile e controlli con etichette di accessibilità.
- Ripristino degli input alla ricreazione dell'Activity Android tramite StateKeeper. Su iOS lo stato vive nella sessione dell'app; non è persistito dopo la terminazione del processo.

Le formule e i valori iniziali coincidono con l'HTML: 07:00 per calcolare il letto, 23:00 per calcolare il risveglio, 15 minuti per addormentarsi, da 3 a 6 cicli. Il cambio modalità reimposta l'orario, mantenendo gli altri parametri. Si tratta di aritmetica su orari, senza date o correzioni per l'ora legale, come nella pagina originale. Nessuna sveglia o notifica viene programmata.

## Avvio

Aprire questa cartella in Android Studio, attendere la sincronizzazione Gradle e avviare la configurazione del modulo `androidApp` su un dispositivo/emulatore API 31+. Configurare l'SDK in `local.properties` se necessario (`sdk.dir=...`).

Per iOS, aprire `iosApp/iosApp.xcodeproj` in Xcode 27+, scegliere lo scheme **Sleepify** e un simulatore iOS 27+. La fase di build compila automaticamente il framework Kotlin. Per un iPhone fisico, impostare il proprio Team in Signing & Capabilities e un identificatore bundle disponibile. Il repository non contiene certificati o Team di Trenify.

Build Android da terminale:

```sh
./gradlew :androidApp:assembleDebug
```

L'APK si trova in `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Verifica

```sh
# Test della logica condivisa su JVM/Android host e simulatore iOS, lint e APK.
./gradlew verifyAll

# Con un emulatore Android già avviato.
./gradlew :androidApp:connectedDebugAndroidTest

# Test UI SwiftUI su un simulatore iOS 27 disponibile.
xcodebuild -project iosApp/iosApp.xcodeproj -scheme Sleepify \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.0' \
  -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO test
```

Si può selezionare un simulatore specifico per i test Kotlin con `-Psleepify.iosSimulator=<UDID>`. Eseguire i test UI Xcode e quelli Kotlin/iOS in sequenza sullo stesso simulatore.

I test coprono i valori iniziali dell'HTML, entrambe le direzioni, mezzanotte, limiti, ogni minuto del giorno con tutte le latenze consentite, normalizzazione, ripristino e chiusura della sessione. I test UI controllano modalità, risultati, appunti, selettore orario nativo e ricreazione Android; su iOS controllano modalità, conferma copia, selettore orario nativo e intervallo di cicli.

Solo i bridge generati da SKIE sono compilati in modalità Swift 5, come in Trenify; il codice Swift dell'app e dei test usa Swift 6.

L'icona iOS si rigenera dalla radice con `swift scripts/generate-icon.swift`. Log e catture della verifica locale sono nella cartella ignorata `review/`.
