package com.BS924.trailcamera;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class DatasetBuilder {

    private static final int SIZE = 256;
    private static BufferedImage resize(
            BufferedImage source
    ) {
        BufferedImage result =
                new BufferedImage(
                        DatasetBuilder.SIZE,
                        DatasetBuilder.SIZE,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g =
                result.createGraphics();

        g.drawImage(
                source,
                0,
                0,
                DatasetBuilder.SIZE,
                DatasetBuilder.SIZE,
                null
        );

        g.dispose();

        return result;
    }

    public static void main(String[] args) throws Exception {
        File framesDir = new File("training/frames");

        File carDir = new File("training/dataset/car");
        File nonCarDir = new File("training/dataset/not_car");

        Files.createDirectories(carDir.toPath());
        Files.createDirectories(nonCarDir.toPath());

        File[] frames = framesDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png")
        );

        if (frames == null) {
            throw new IllegalStateException("No frames found.");
        }

        int carCount = 0;
        int nonCarCount = 0;

        for (File frameFile : frames) {

            File labelFile = new File(
                    frameFile.getParent(),
                    frameFile.getName().replaceFirst(
                            "\\.png$",
                            ".txt"
                    )
            );

            if (!labelFile.exists()) {
                continue;
            }

            BufferedImage image = ImageIO.read(frameFile);

            List<Rectangle> cars = readLabels(labelFile);

            if (cars.isEmpty()) {

                BufferedImage negative =
                        resize(image);

                save(
                        negative,
                        new File(
                                nonCarDir,
                                String.format(
                                        "not_car_%06d_" + System.currentTimeMillis() + ".png",
                                        nonCarCount++
                                )
                        )
                );

            } else {

                for (Rectangle car : cars) {

                    BufferedImage crop =
                            crop(image, car);

                    if (crop == null) {
                        continue;
                    }

                    save(
                            crop,
                            new File(
                                    carDir,
                                    String.format(
                                            "car_%06d_" + System.currentTimeMillis() + ".png",
                                            carCount++
                                    )
                            )
                    );
                }
            }
        }

        System.out.println(
                "Cars: " + carCount
        );

        System.out.println(
                "Not cars: " + nonCarCount
        );
    }

    private static List<Rectangle> readLabels(
            File file
    ) throws Exception {

        List<Rectangle> boxes =
                new ArrayList<>();

        try (Scanner scanner =
                     new Scanner(file)) {

            while (scanner.hasNextInt()) {

                int x = scanner.nextInt();
                int y = scanner.nextInt();
                int w = scanner.nextInt();
                int h = scanner.nextInt();

                boxes.add(
                        new Rectangle(
                                x, y, w, h
                        )
                );
            }
        }

        return boxes;
    }

    private static BufferedImage crop(
            BufferedImage image,
            Rectangle box
    ) {

        Rectangle bounds =
                new Rectangle(
                        0,
                        0,
                        image.getWidth(),
                        image.getHeight()
                );

        Rectangle clipped =
                box.intersection(bounds);

        if (clipped.width <= 2 ||
                clipped.height <= 2) {
            return null;
        }

        BufferedImage result =
                new BufferedImage(
                        SIZE,
                        SIZE,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g =
                result.createGraphics();

        g.drawImage(
                image,
                0,
                0,
                SIZE,
                SIZE,
                clipped.x,
                clipped.y,
                clipped.x + clipped.width,
                clipped.y + clipped.height,
                null
        );

        g.dispose();

        return result;
    }

    private static void save(
            BufferedImage image,
            File file
    ) throws Exception {

        ImageIO.write(
                image,
                "png",
                file
        );
    }
}