package com.BS924.trailcamera;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        FrameExtractor extractor = new FrameExtractor(
                "bin/ffmpeg.exe"
        );

        extractor.extractFrames(
                new File("training/TRAINING-VIDEO-HERE.mp4"),
                new File("training/frames"),
                1f
        );

        System.out.println("Training frames extracted.");
    }
}