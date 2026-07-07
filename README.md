# Dropbox Streamer

MP3-soitin joka streamaa musiikkia suoraan Dropboxista. Spring Boot -backend hakee tilapäiset suoratoistolinkitDropbox API:lta, Vue.js -frontend toistaa ne HTML5-audiona.

## Arkkitehtuuri

```
Selain → localhost:80
              │
         [Nginx]
              ├── GET /        → Vue.js (staattiset tiedostot)
              └── GET /api/*   → Spring Boot :8080
                                    │
                                 Dropbox API
```

Backend ei proxyta audiodataa — se hakee ainoastaan tilapäisen linkin (voimassa 4h), jonka selain käyttää suoraan Dropboxilta streamaukseen.

## Ominaisuudet

- Kansionavigaatio Dropbox-hakemistopuussa
- MP3-tiedostojen listaus
- Toisto, tauko, kelaus ja äänenvoimakkuuden säätö
- Tumma teema

## Vaatimukset

- Docker ja Docker Compose
- Dropbox Developer App (Scoped access) ja access token

## Asennus

**1. Luo Dropbox Developer App**

Mene osoitteeseen [dropbox.com/developers/apps](https://www.dropbox.com/developers/apps) ja luo uusi app seuraavilla asetuksilla:

- API: `Scoped access`
- Access type: `Full Dropbox`
- Permissions: `files.metadata.read`, `files.content.read`

Generoi access token kohdasta Settings → Generated access token.

**2. Lisää access token**

Luo tiedosto `backend/.env`:

```
DROPBOX_ACCESS_TOKEN=sl.u.xxxxxxxx...
```

**3. Käynnistä**

```bash
docker-compose up --build
```

Avaa selaimessa: [http://localhost](http://localhost)

## Kehitysympäristö ilman Dockeria

**Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

Frontend käynnistyy osoitteeseen `http://localhost:5173`. Vite proxyttaa `/api/`-pyynnöt automaattisesti backendille.

## Rakenne

```
dropboxstreamer/
├── backend/
│   ├── src/main/java/com/dropboxstreamer/
│   │   ├── controller/DropboxController.java   # REST-endpointit
│   │   ├── service/DropboxService.java         # Dropbox API -kutsut
│   │   ├── model/DropboxFile.java
│   │   └── config/WebConfig.java               # CORS, RestTemplate
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── FileList.vue                    # Kansionavigaatio
│   │   │   └── AudioPlayer.vue                 # Soitin
│   │   └── App.vue
│   ├── nginx.conf
│   └── Dockerfile
└── docker-compose.yml
```

## API

| Endpoint | Kuvaus |
|----------|--------|
| `GET /api/files?path=/` | Listaa MP3-tiedostot ja kansiot annetussa polussa |
| `GET /api/stream?path=/musiikki/kappale.mp3` | Palauttaa tilapäisen suoratoistolinkin |
