# 🎬 Spring Stream Backend

This is the backend service for the **Spring Stream** application. It handles **video uploads**, **HLS conversion using FFmpeg**, and **retrieval via API**.

---

## ✨ Features  
- ✅ Upload videos and store them in a designated directory 
- ✅ Process videos using FFmpeg to generate HLS (HTTP Live Streaming) playlists  
- ✅ Retrieve video metadata and HLS streams  
- ✅ RESTful API endpoints for video management

---

## 🛠️ Tech Stack  
- 🚀 **Spring Boot**  
- ☕ **Java 17**  
- 🛢️ **MySQL**  
- 🎞️ **FFmpeg**  
- 📦 **Maven**  

---
## API Endpoints

### 1. Upload Video

**POST** `/api/videos/upload`

Uploads a video file and processes it into HLS format.

---

### 2. Get Video by ID

**GET** `/api/videos/{id}`

Retrieves the metadata for a specific video.

---

### 3. Get All Videos

**GET** `/api/videos`

Returns all uploaded videos.

---

### 4. Stream Video (HLS Playlist)

**GET** `/videos_hsl/{videoId}/master.m3u8`

Streams the processed video using HLS format.