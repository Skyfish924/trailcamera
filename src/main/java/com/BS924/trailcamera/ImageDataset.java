package com.BS924.trailcamera;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.dataset.ArrayDataset;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageDataset {

    public static final int MODEL_SIZE = 64;

    public static final int CHANNELS = 3;

    public static final int INPUTS =
            MODEL_SIZE * MODEL_SIZE * CHANNELS;

    public static final int BATCH_SIZE = 64;

    public record Example(float[] input, float target) {
    }

    public static List<Example> load(
            File directory,
            float target
    ) throws Exception {

        List<Example> examples =
                new ArrayList<>();

        File[] files =
                directory.listFiles((dir, name) ->
                        name.toLowerCase().endsWith(".png"));

        if (files == null) {
            return examples;
        }

        for (File file : files) {

            BufferedImage image =
                    ImageIO.read(file);

            if (image == null) {
                continue;
            }

            BufferedImage resized =
                    resize(
                            image
                    );

            examples.add(
                    new Example(
                            toInput(resized),
                            target
                    )
            );
        }

        return examples;
    }

    public static ArrayDataset createDataset(
            NDManager manager,
            List<Example> examples
    ) {

        float[] data =
                new float[
                        examples.size() * INPUTS
                        ];

        float[] labels =
                new float[examples.size()];

        for (int n = 0; n < examples.size(); n++) {

            Example example =
                    examples.get(n);

            System.arraycopy(
                    example.input,
                    0,
                    data,
                    n * INPUTS,
                    INPUTS
            );

            labels[n] =
                    example.target;
        }

        NDArray features =
                manager.create(data)
                        .reshape(
                                new Shape(
                                        examples.size(),
                                        CHANNELS,
                                        MODEL_SIZE,
                                        MODEL_SIZE
                                )
                        )
                        .toType(
                                DataType.FLOAT32,
                                false
                        );

        NDArray targets =
                manager.create(labels)
                        .toType(
                                DataType.INT64,
                                false
                        );

        return new ArrayDataset.Builder()
                .setData(features)
                .optLabels(targets)
                .setSampling(
                        BATCH_SIZE,
                        true
                )
                .build();
    }

    private static float[] toInput(
            BufferedImage image
    ) {

        float[] input =
                new float[INPUTS];

        int planeSize =
                MODEL_SIZE * MODEL_SIZE;

        for (int y = 0; y < MODEL_SIZE; y++) {

            for (int x = 0; x < MODEL_SIZE; x++) {

                int rgb =
                        image.getRGB(x, y);

                int r =
                        (rgb >> 16) & 0xFF;

                int g =
                        (rgb >> 8) & 0xFF;

                int b =
                        rgb & 0xFF;

                int pixel =
                        y * MODEL_SIZE + x;

                // NCHW format:
                input[pixel] =
                        r / 255.0f;

                input[planeSize + pixel] =
                        g / 255.0f;

                input[planeSize * 2 + pixel] =
                        b / 255.0f;
            }
        }

        return input;
    }

    private static BufferedImage resize(
            BufferedImage source
    ) {

        BufferedImage result =
                new BufferedImage(
                        ImageDataset.MODEL_SIZE,
                        ImageDataset.MODEL_SIZE,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D g =
                result.createGraphics();

        g.drawImage(
                source,
                0,
                0,
                ImageDataset.MODEL_SIZE,
                ImageDataset.MODEL_SIZE,
                null
        );

        g.dispose();

        return result;
    }
}