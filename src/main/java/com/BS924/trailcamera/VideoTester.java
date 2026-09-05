package com.BS924.trailcamera;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VideoTester {

    private static final double CONFIDENCE_THRESHOLD = 0.75;

    private static final int MAX_BOXES = 5;

    private static final int[] WINDOW_SIZES = {
            256,
            128,
            64
    };

    private static final int STEP = 64;

    private static final double NMS_IOU_THRESHOLD = 0.40;

    private static final int FPS = 10;

    public static void main(String[] args) throws Exception {

        GraphicsEnvironment environment =
                GraphicsEnvironment
                        .getLocalGraphicsEnvironment();

        GraphicsDevice[] monitors =
                environment.getScreenDevices();

        System.out.println("Available monitors:");

        for (int i = 0; i < monitors.length; i++) {

            Rectangle bounds =
                    monitors[i]
                            .getDefaultConfiguration()
                            .getBounds();

            System.out.printf(
                    "[%d] %dx%d at (%d, %d)%n",
                    i,
                    bounds.width,
                    bounds.height,
                    bounds.x,
                    bounds.y
            );
        }

        int monitorIndex = 0;

        if (args.length > 0) {
            monitorIndex =
                    Integer.parseInt(args[0]);
        }

        if (monitorIndex < 0 ||
                monitorIndex >= monitors.length) {

            throw new IllegalArgumentException(
                    "Invalid monitor index."
            );
        }

        GraphicsDevice monitor =
                monitors[monitorIndex];

        Rectangle monitorBounds =
                monitor
                        .getDefaultConfiguration()
                        .getBounds();

        System.out.println(
                "Using monitor " +
                        monitorIndex +
                        ": " +
                        monitorBounds
        );

        try (NeuralNetwork network =
                     NeuralNetwork.load(
                             new File("training/model")
                     )) {

            Robot robot =
                    new Robot(monitor);

            Overlay overlay =
                    new Overlay(monitorBounds);

            overlay.setVisible(true);

            final long frameTime =
                    1000L / FPS;

            while (true) {

                long start =
                        System.currentTimeMillis();

                BufferedImage frame =
                        robot.createScreenCapture(
                                new Rectangle(
                                        0,
                                        0,
                                        monitorBounds.width,
                                        monitorBounds.height
                                )
                        );

                List<CarScanner.Detection> detections =
                        scan(
                                frame,
                                network
                        );

                overlay.update(
                        detections
                );

                long elapsed =
                        System.currentTimeMillis()
                                - start;

                long sleep =
                        frameTime - elapsed;

                if (sleep > 0) {
                    Thread.sleep(sleep);
                }
            }
        }
    }

    private static List<CarScanner.Detection> scan(
            BufferedImage frame,
            NeuralNetwork network
    ) {

        List<CarScanner.Detection> detections =
                new ArrayList<>();

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

                    double probability =
                            network.predict(
                                    imageToInput(window)
                            );

                    if (probability >=
                            CONFIDENCE_THRESHOLD) {

                        detections.add(
                                new CarScanner.Detection(
                                        new Rectangle(
                                                x,
                                                y,
                                                width,
                                                height
                                        ),
                                        probability
                                )
                        );

                        /*
                         * THIS is the CPU-saving part.
                         *
                         * Once enough qualifying detections
                         * have been found, stop running CNN
                         * predictions for this frame.
                         */
                        if (detections.size() >= MAX_BOXES) {

                            return nonMaximumSuppression(
                                    detections
                            );
                        }
                    }
                }
            }
        }

        return nonMaximumSuppression(
                detections
        );
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
                positions.getLast() != last) {

            positions.add(last);
        }

        return positions.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static List<CarScanner.Detection>
    nonMaximumSuppression(
            List<CarScanner.Detection> detections
    ) {

        if (detections.isEmpty()) {
            return new ArrayList<>();
        }

        List<CarScanner.Detection> sorted =
                new ArrayList<>(
                        detections
                );

        sorted.sort(
                Comparator.comparingDouble(
                        CarScanner.Detection::probability
                ).reversed()
        );

        List<CarScanner.Detection> result =
                new ArrayList<>();

        for (CarScanner.Detection candidate :
                sorted) {

            boolean overlaps =
                    false;

            for (CarScanner.Detection kept :
                    result) {

                if (iou(
                        candidate.bounds(),
                        kept.bounds()
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

    private static class Overlay
            extends JWindow {

        private volatile List<CarScanner.Detection>
                detections = List.of();

        public Overlay(
                Rectangle monitorBounds
        ) {

            setAlwaysOnTop(true);

            setBackground(
                    new Color(
                            0,
                            0,
                            0,
                            0
                    )
            );

            setBounds(
                    monitorBounds
            );

            setFocusableWindowState(false);

            JPanel panel =
                    new JPanel() {

                        {
                            setOpaque(false);
                        }

                        @Override
                        protected void paintComponent(
                                Graphics graphics
                        ) {

                            super.paintComponent(
                                    graphics
                            );

                            Graphics2D g =
                                    (Graphics2D)
                                            graphics.create();

                            g.setStroke(
                                    new BasicStroke(4)
                            );

                            for (
                                    CarScanner.Detection detection
                                    : detections
                            ) {

                                Rectangle box =
                                        detection.bounds();

                                g.setColor(
                                        Color.RED
                                );

                                g.drawRect(
                                        box.x,
                                        box.y,
                                        box.width,
                                        box.height
                                );

                                String text =
                                        String.format(
                                                "%.1f%% CAR",
                                                detection.probability()
                                                        * 100
                                        );

                                FontMetrics metrics =
                                        g.getFontMetrics();

                                int textWidth =
                                        metrics.stringWidth(
                                                text
                                        );

                                int textHeight =
                                        metrics.getHeight();

                                int textX =
                                        box.x;

                                int textY =
                                        Math.max(
                                                textHeight,
                                                box.y
                                        );

                                g.fillRect(
                                        textX,
                                        textY - textHeight,
                                        textWidth + 10,
                                        textHeight
                                );

                                g.setColor(
                                        Color.WHITE
                                );

                                g.drawString(
                                        text,
                                        textX + 5,
                                        textY - 4
                                );
                            }

                            g.dispose();
                        }
                    };

            add(panel);
        }

        public void update(
                List<CarScanner.Detection> detections
        ) {

            this.detections =
                    detections;

            repaint();
        }
    }
}