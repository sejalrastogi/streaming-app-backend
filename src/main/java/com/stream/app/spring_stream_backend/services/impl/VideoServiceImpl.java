package com.stream.app.spring_stream_backend.services.impl;

import com.stream.app.spring_stream_backend.entities.Video;
import com.stream.app.spring_stream_backend.repositories.VideoRepository;
import com.stream.app.spring_stream_backend.services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository){
        this.videoRepository = videoRepository;
    }

    @Value("${files.video}")
    String DIR;

    @Value("${files.video.hsl}")
    String HSL_DIR;

    // if the DIR is not already there, then create one
    @PostConstruct
    public void init(){
        File file = new File(DIR);

        try {
            Files.createDirectories(Paths.get(HSL_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        File file1 = new File(HSL_DIR);
//        if(!file1.exists()){
//            file1.mkdir();
//        }

        if(!file.exists()){
            file.mkdir();
            System.out.println("Folder Created.");
        }else{
            System.out.println("Folder already present.");
        }

    }

    @Override
    public Video save(Video video, MultipartFile file) {

        try {
            // Check if a video with the same title exists
            Video existingVideo = videoRepository.findById(video.getVideoId()).orElse(null);
            if (existingVideo != null) {
                System.out.println("Video already exists. Skipping upload.");
                return existingVideo; // Return the existing video instead of reprocessing
            }

            // original file name
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // file path
            String cleanFileName = StringUtils.cleanPath(fileName);

            // folder path : create
            String cleanFolder = StringUtils.cleanPath(DIR);

            // folder path with file name
            Path path = Paths.get(cleanFolder, cleanFileName);

            System.out.println(path);

            // copy file with the video
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            // set video details
            video.setContent_type(contentType);
            video.setFilePath(path.toString());

            videoRepository.save(video);

            // Process video only if HLS output does not exist
            Path hlsPath = Paths.get(HSL_DIR, video.getVideoId(), "master.m3u8");
            if (!Files.exists(hlsPath)) {
                System.out.println("Processing video for HLS conversion...");
                processVideo(video.getVideoId());
            } else {
                System.out.println("HLS playlist already exists. Skipping processing.");
            }

            return video;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        return null;
    }

    @Override
    public Video getById(String id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new RuntimeException("video not found"));
        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = this.getById(videoId);
        String filePath = video.getFilePath();

        // path where to store data:
        Path videoPath = Paths.get(filePath);

//        String output360 = HSL_DIR + videoId + "/360p/";
//        String output720 = HSL_DIR + videoId + "/720p/";
//        String output1080 = HSL_DIR + videoId + "/1080p/";

        try{
//            Files.createDirectories(Paths.get(output360));
//            Files.createDirectories(Paths.get(output720));
//            Files.createDirectories(Paths.get(output1080));

            // ffmpeg command
            Path outputPath = Paths.get(HSL_DIR, videoId);

            Files.createDirectories(outputPath);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/master.m3u8\"",
                    videoPath, outputPath, outputPath
            );



            System.out.println(ffmpegCmd);

//            StringBuilder ffmpegCmd = new StringBuilder();
//            ffmpegCmd.append("ffmpeg  -i ")
//                    .append(videoPath.toString())
//                    .append(" -c:v libx264 -c:a aac")
//                    .append(" ")
//                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
//                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
//                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
//                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
//                    .append("-master_pl_name ").append(HSL_DIR).append(videoId).append("/master.m3u8 ")
//                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
//                    .append("-hls_segment_filename \"").append(HSL_DIR).append(videoId).append("/v%v/fileSequence%d.ts\" ")
//                    .append("\"").append(HSL_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");


            System.out.println(ffmpegCmd);
            String[] ffmpegArgs = {
                    "C:\\Users\\LENOVO\\Downloads\\ffmpeg-7.1-essentials_build\\ffmpeg-7.1-essentials_build\\bin\\ffmpeg.exe",
                    "-i", String.valueOf(videoPath),
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-strict", "-2",
                    "-f", "hls",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_segment_filename", outputPath + "\\segment_%3d.ts",
                    outputPath + "\\master.m3u8"
            };
            //file this command
//            ProcessBuilder processBuilder = new ProcessBuilder("C:\\\\Users\\\\LENOVO\\\\Downloads\\\\ffmpeg-7.1-essentials_build\\\\ffmpeg-7.1-essentials_build\\\\bin\\\\ffmpeg.exe", "-c", ffmpegCmd);
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegArgs);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Video processing Failed!");
        }


//        return null;
    }
}
