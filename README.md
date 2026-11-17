# EpisodeDay Android project

See on EpisodeDay Androidi lähtekoodiprojekt. Rakendus:
- otsib seriaale TVmaze'i avalikust API-st;
- salvestab valitud seriaalid telefoni;
- näitab teadaolevaid tulevasi osi;
- kontrollib andmeid äpi avamisel ja kord päevas WorkManageri kaudu;
- kuvab kohalikke teavitusi osade ilmumispäeval.

## Oluline
See pakett on Android Studio projekt, mitte veel installitav APK. Selles töökeskkonnas puudub Android SDK/Gradle'i ehituskeskkond, seega APK-d ei ole siin võimalik kompileerida ega seadmes testida.

APK ehitamiseks ava projekt Android Studios, lase Gradle'il sünkroonida ning vali Build > Build APK(s). APK asub tavaliselt `app/build/outputs/apk/debug/app-debug.apk`.

Rakendus kasutab TVmaze'i avalikku API-t (internet on vajalik). Kuupäevad ja saated võivad andmeallika järgi muutuda. Android võib piirata taustatööd ja teavituste kuvamiseks peab kasutaja andma loa.
