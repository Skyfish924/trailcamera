package com.BS924.trailcamera;

import java.io.File;
import java.io.IOException;

public class FrameExtractor {

    private static String ffmpegPath;

    public FrameExtractor(String ffmpegPath) {
        FrameExtractor.ffmpegPath = ffmpegPath;
    }

    public void extractFrames(File video, File outputDirectory, float fps)
            throws Exception {

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException(
                    "Failed to create output directory: " +
                            outputDirectory.getAbsolutePath()
            );
        }

        ProcessBuilder process = new ProcessBuilder(
                ffmpegPath,
                "-i", video.getAbsolutePath(),
                "-vf", "fps=" + fps,
                new File(outputDirectory, "frame_%06d_" + System.currentTimeMillis() + ".png").getAbsolutePath()
        );

        process.inheritIO();

        Process ffmpeg = process.start();
        int exitCode = ffmpeg.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "FFmpeg failed with exit code " + exitCode
            );
        }
    }
}