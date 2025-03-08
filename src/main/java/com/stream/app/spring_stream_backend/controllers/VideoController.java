package com.stream.app.spring_stream_backend.controllers;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.stream.app.spring_stream_backend.AppConstants;
import com.stream.app.spring_stream_backend.entities.Video;
import com.stream.app.spring_stream_backend.payload.CustomMessage;
import com.stream.app.spring_stream_backend.services.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/videos")
@CrossOrigin("*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @PostMapping
    public ResponseEntity<?> create(@RequestParam("file")MultipartFile file, @RequestParam("title") String title, @RequestParam("description") String description){

        // create video
        Video video = new Video();
        video.setVideoId(UUID.randomUUID().toString());
        video.setTitle(title);
        video.setDescription(description);

        Video savedVideo = videoService.save(video, file);
        if(savedVideo != null){
            return ResponseEntity.status(HttpStatus.OK).body(video);
        }else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Video not uploaded.");
        }

//        return null;
    }

    // get all videos
    @GetMapping
    public List<Video> getAll(){
        return videoService.getAll();
    }

    // stream video
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId){
        Video video = videoService.getById(videoId);
        String content_type = video.getContent_type();
        String filePath = video.getFilePath();

        Resource resource = new FileSystemResource(filePath);

        if(content_type == null){
            content_type = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content_type)).body((Resource) resource);
    }

    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streanVideoRange(@PathVariable String videoId, @RequestHeader(value = "Range", required = false) String range){
        System.out.println(range);

        Video video = videoService.getById(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);
        String contentType = video.getContent_type();

        if(contentType == null) contentType = "application/octet-stream";

        // length of the file
        long fileLength = path.toFile().length();

        // when range header is null, we will return the whole file
        if(range == null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
        }

        // calculate start and end range
        long rangeStart;
        long rangeEnd;

        String[] ranges = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(ranges[0]);

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE - 1;

//        if(ranges.length > 1){ // means both start and end is given
//            rangeEnd = Long.parseLong(ranges[1]);
//        }else{ // only start is given, so we will read the whole file starting from the rangeStart
//            rangeEnd = fileLength - 1;
//        }
//
        // if rangeEnd exceeds the file length
        if(rangeEnd > fileLength - 1){
            rangeEnd = fileLength - 1; // then we will read till the end of the file
        }

        System.out.println("range start:" + rangeStart);
        System.out.println("range end:" + rangeEnd);

        InputStream inputStream;
        try{
            inputStream = Files.newInputStream(path);
            inputStream.skip(rangeStart);
            long contentLength = rangeEnd - rangeStart + 1;

            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);
            System.out.println("read(number of bytes) : " + read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("X-Content-Type-Options", "nosniff");
            headers.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }


    @Value("${files.video.hsl}")
    private String HSL_DIR;

    // serve hls playlist
    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serveMasterFile(@PathVariable String videoId){

        Path path = Paths.get(HSL_DIR, videoId, "master.m3u8");
        System.out.println(path);

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpequrl").body(resource);

    }

    // serve segments
    @GetMapping("/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> serveSegments(@PathVariable String videoId, @PathVariable String segment){
        // create path for segments
        Path path = Paths.get(HSL_DIR, videoId, segment+".ts");

        if(!Files.exists(path)){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "video/mp2t").body(resource);
    }

}
