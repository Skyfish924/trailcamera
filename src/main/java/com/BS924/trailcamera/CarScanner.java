package com.BS924.trailcamera;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CarScanner {

    private static final int[] WINDOW_SIZES = {
            256,
            128,
            64
    };

    private static final int STEP = 64;

    private static final double DETECTION_THRESHOLD = 0.75;

    private static final double NMS_IOU_THRESHOLD = 0.10;

    public record Detection(Rectangle bounds, double probability) {

    }

    public static List<Detection> scan(
            BufferedImage frame,
            NeuralNetwork network
    ) {

        List<Detection> allDetections =
                new ArrayList<>();

        Detection bestDetection = null;

        for (int windowSize : WINDOW_SIZES) {

            int[] xs =
                    getPositions(
                            frame.getWidth(),
                            windowSize
                    );

            int[] ys =
                    getPositions(
                            frame.getHeight(),
                            windowSize
                    );

            for (int y : ys) {

                for (int x : xs) {

                    int width =
                            Math.min(
                                    windowSize,
                                    frame.getWidth() - x
                            );

                    int height =
                            Math.min(
                                    windowSize,
                                    frame.getHeight() - y
                            );

                    if (width <= 0 ||
                            height <= 0) {
                        continue;
                    }

                    BufferedImage window =
                            frame.getSubimage(
                                    x,
                                    y,
                                    width,
                                    height
                            );

                    double[] input =
                            imageToInput(window);

                    double probability =
                            network.predict(input);

                    Detection detection =
                            new Detection(
                                    new Rectangle(
                                            x,
                                            y,
                                            width,
                                            height
                                    ),
                                    probability
                            );

                    if (bestDetection == null ||
                            probability >
                                    bestDetection.probability) {

                        bestDetection = detection;
                    }

                    if (probability >=
                            DETECTION_THRESHOLD) {

                        allDetections.add(
                                detection
                        );
                    }
                }
            }
        }

        List<Detection> detections =
                nonMaximumSuppression(
                        allDetections
                );

        if (detections.isEmpty() &&
                bestDetection != null) {

            detections.add(
                    bestDetection
            );
        }

        return detections;
    }

    private static int[] getPositions(
            int dimension,
            int windowSize
    ) {

        if (dimension <= windowSize) {
            return new int[]{0};
        }

        List<Integer> positions =
                new ArrayList<>();

        for (
                int position = 0;
                position <= dimension - windowSize;
                position += STEP
        ) {

            positions.add(position);
        }

        int last =
                dimension - windowSize;

        if (positions.isEmpty() ||
                positions.getLast(
                ) != last) {

            positions.add(last);
        }

        return positions.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static List<Detection>
    nonMaximumSuppression(
            List<Detection> detections
    ) {

        if (detections.isEmpty()) {
            return new ArrayList<>();
        }

        List<Detection> sorted =
                new ArrayList<>(
                        detections
                );

        sorted.sort(
                Comparator.comparingDouble(
                        (Detection d) ->
                                d.probability
                ).reversed()
        );

        List<Detection> result =
                new ArrayList<>();

        for (Detection candidate :
                sorted) {

            boolean overlaps =
                    false;

            for (Detection kept :
                    result) {

                if (iou(
                        candidate.bounds,
                        kept.bounds
                ) >= NMS_IOU_THRESHOLD) {

                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                result.add(candidate);
            }
        }

        return result;
    }

    private static double iou(
            Rectangle a,
            Rectangle b
    ) {

        Rectangle intersection =
                a.intersection(b);

        if (intersection.width <= 0 ||
                intersection.height <= 0) {

            return 0.0;
        }

        double intersectionArea =
                (double) intersection.width *
                        intersection.height;

        double areaA =
                (double) a.width *
                        a.height;

        double areaB =
                (double) b.width *
                        b.height;

        double unionArea =
                areaA +
                        areaB -
                        intersectionArea;

        if (unionArea <= 0) {
            return 0.0;
        }

        return intersectionArea /
                unionArea;
    }

    private static double[] imageToInput(
            BufferedImage image
    ) {

        int size =
                ImageDataset.MODEL_SIZE;

        BufferedImage resized =
                new BufferedImage(
                        size,
                        size,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g =
                resized.createGraphics();

        g.drawImage(
                image,
                0,
                0,
                size,
                size,
                null
        );

        g.dispose();

        double[] input =
                new double[
                        ImageDataset.INPUTS
                        ];

        int index = 0;

        for (int y = 0; y < size; y++) {

            for (int x = 0; x < size; x++) {

                int rgb =
                        resized.getRGB(
                                x,
                                y
                        );

                input[index++] =
                        ((rgb >> 16) & 0xFF)
                                / 255.0;

                input[index++] =
                        ((rgb >> 8) & 0xFF)
                                / 255.0;

                input[index++] =
                        (rgb & 0xFF)
                                / 255.0;
            }
        }

        return input;
    }
}