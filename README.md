# Muzyka

Osobna aplikacja Android z samą muzyką: **Radio** (stacje internetowe, ulubione) i **YT Music** (wyszukiwarka, własne playlisty, historia, crossfade, bez reklam przez NewPipeExtractor).
Kod pochodzi z modułu Muzyka w MD-Tools (skopiowany, MD-Tools nie zmieniono).

## Budowanie
GitHub Actions buduje APK przy każdym pushu na `main` (wydanie `debug`) oraz przy tagu `v*` (wydanie podpisane).
Sekrety repo do podpisu (jak w MD-Tools): `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Bez nich build jest podpisany kluczem debug.
