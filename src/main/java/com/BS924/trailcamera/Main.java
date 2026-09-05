package com.BS924.trailcamera;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Main {

    public static void main(String[] args) throws Exception {
        FrameExtractor extractor = new FrameExtractor(
                "bin/ffmpeg.exe"
        );
        Path path1 = Path.of("training/frames");
        if (Files.exists(path1)) {
            Files.walk(path1)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        extractor.extractFrames(

                new File("training/hi.mp4"),
                new File("training/frames"),
                10f
        );
        Labeler.start();
    }
}