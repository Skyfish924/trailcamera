package com.BS924.trailcamera;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Block;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.nn.pooling.Pool;
import ai.djl.training.ParameterStore;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class NeuralNetwork implements AutoCloseable {

    public static final String MODEL_NAME = "car-detector";

    private static final int CHANNELS = 3;
    private static final int IMAGE_SIZE = 64;

    private final Model model;
    private final Device device;
    private final NDManager manager;
    private final ParameterStore parameterStore;

    public NeuralNetwork() {

        Engine engine =
                Engine.getEngine("PyTorch");

        Device[] devices =
                engine.getDevices(1);

        if (devices.length == 0) {
            throw new IllegalStateException(
                    "No PyTorch compute device found."
            );
        }

        device = devices[0];

        manager =
                engine.newBaseManager(device);

        model =
                Model.newInstance(
                        MODEL_NAME,
                        "PyTorch"
                );

        model.setBlock(createNetwork());

        parameterStore =
                new ParameterStore(
                        manager,
                        false
                );

        model.getBlock().initialize(
                manager,
                DataType.FLOAT32,
                new Shape(
                        1,
                        CHANNELS,
                        IMAGE_SIZE,
                        IMAGE_SIZE
                )
        );
    }

    private NeuralNetwork(
            Model model,
            Device device,
            NDManager manager
    ) {

        this.model = model;
        this.device = device;
        this.manager = manager;

        this.parameterStore =
                new ParameterStore(
                        manager,
                        false
                );
    }

    private static Block createNetwork() {

        SequentialBlock network =
                new SequentialBlock();

        network.add(
                Conv2d.builder()
                        .setFilters(16)
                        .setKernelShape(
                                new Shape(3, 3)
                        )
                        .optPadding(
                                new Shape(1, 1)
                        )
                        .build()
        );

        network.add(Activation::relu);

        network.add(
                Pool.maxPool2dBlock(
                        new Shape(2, 2),
                        new Shape(2, 2)
                )
        );

        network.add(
                Conv2d.builder()
                        .setFilters(32)
                        .setKernelShape(
                                new Shape(3, 3)
                        )
                        .optPadding(
                                new Shape(1, 1)
                        )
                        .build()
        );

        network.add(Activation::relu);

        network.add(
                Pool.maxPool2dBlock(
                        new Shape(2, 2),
                        new Shape(2, 2)
                )
        );

        network.add(
                Blocks.batchFlattenBlock()
        );

        network.add(
                Linear.builder()
                        .setUnits(128)
                        .build()
        );

        network.add(Activation::relu);

        network.add(
                Linear.builder()
                        .setUnits(2)
                        .build()
        );

        return network;
    }

    public double predict(double[] input) {

        if (input.length != ImageDataset.INPUTS) {
            throw new IllegalArgumentException(
                    "Expected " +
                            ImageDataset.INPUTS +
                            " inputs, got " +
                            input.length
            );
        }

        float[] converted = new float[input.length];

        for (int i = 0; i < input.length; i++) {
            converted[i] = (float) input[i];
        }

        int planeSize =
                IMAGE_SIZE * IMAGE_SIZE;

        float[] nchw =
                new float[input.length];

        for (int pixel = 0; pixel < planeSize; pixel++) {

            int rgbIndex =
                    pixel * 3;

            nchw[pixel] =
                    converted[rgbIndex];

            nchw[planeSize + pixel] =
                    converted[rgbIndex + 1];

            nchw[planeSize * 2 + pixel] =
                    converted[rgbIndex + 2];
        }

        try (NDArray inputArray =
                     manager.create(nchw)
                             .reshape(
                                     new Shape(
                                             1,
                                             3,
                                             IMAGE_SIZE,
                                             IMAGE_SIZE
                                     )
                             )) {

            NDList output =
                    model.getBlock().forward(
                            parameterStore,
                            new NDList(inputArray),
                            false
                    );

            NDArray logits =
                    output.singletonOrThrow();

            NDArray probabilities =
                    logits.softmax(1);

            return probabilities
                    .getFloat(0, 1);
        }
    }

    public Model getModel() {
        return model;
    }

    public static NeuralNetwork load(
            File directory
    ) throws IOException, MalformedModelException {

        Engine engine =
                Engine.getEngine("PyTorch");

        Device[] devices =
                engine.getDevices(1);

        if (devices.length == 0) {
            throw new IllegalStateException(
                    "No PyTorch compute device found."
            );
        }

        Device device = devices[0];

        NDManager manager =
                engine.newBaseManager(device);

        Model model =
                Model.newInstance(
                        MODEL_NAME,
                        "PyTorch"
                );

        model.setBlock(createNetwork());

        File params =
                new File(
                        directory,
                        "car-detector.params"
                );

        if (!params.exists()) {
            throw new IOException(
                    "Parameter file not found: " +
                            params.getAbsolutePath()
            );
        }

        try (DataInputStream input =
                     new DataInputStream(
                             new FileInputStream(params)
                     )) {

            model.getBlock().loadParameters(
                    manager,
                    input
            );
        }

        return new NeuralNetwork(
                model,
                device,
                manager
        );
    }

    @Override
    public void close() {
        model.close();
        manager.close();
    }
}