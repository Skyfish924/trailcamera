package com.BS924.trailcamera;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Tester {

    public static void main(String[] args)
            throws Exception {

        NeuralNetwork network =
                NeuralNetwork.load(
                        new File("training/model")
                );

        File inputFile =
                new File("training/testing/img_1.png");

        BufferedImage frame =
                ImageIO.read(inputFile);

        File outputDir =
                new File("training/detections");

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException(
                    "Failed to create output directory: " +
                            outputDir.getAbsolutePath()
            );
        }

        List<CarScanner.Detection> detections =
                CarScanner.scan(
                        frame,
                        network
                );

        System.out.println(
                "Candidate regions: " +
                        detections.size()
        );

        int saved = 0;

        for (CarScanner.Detection detection :
                detections) {

            System.out.printf(
                    "x=%d y=%d probability=%.2f%%%n",
                    detection.bounds().x,
                    detection.bounds().y,
                    detection.probability() * 100
            );

            BufferedImage crop =
                    frame.getSubimage(
                            detection.bounds().x,
                            detection.bounds().y,
                            detection.bounds().width,
                            detection.bounds().height
                    );

            File output =
                    new File(
                            outputDir,
                            String.format(
                                    "car_candidate_%03d_%.2f_" + System.currentTimeMillis() + ".png",
                                    saved++,
                                    detection.probability()
                            )
                    );

            ImageIO.write(
                    crop,
                    "png",
                    output
            );
        }

        System.out.println(
                "Saved " + saved +
                        " candidate images to: " +
                        outputDir.getAbsolutePath()
        );
    }
}