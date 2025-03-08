package com.stream.app.spring_stream_backend.services;

import com.stream.app.spring_stream_backend.entities.Video;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

    // save video
    Video save(Video video, MultipartFile file);

    // get video by id
    Video getById(String id);

    // get video by title
    Video getByTitle(String title);

    // get all the videos
    List<Video> getAll();

    // video processing
    String processVideo(String videoId);
}
