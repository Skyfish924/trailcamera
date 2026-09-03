package com.BS924.trailcamera;

import ai.djl.Device;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trainer {

    private static final int EPOCHS = 100;

    private static final float LEARNING_RATE =
            0.001f;

    private static final String MODEL_DIR =
            "training/model";

    public static void main(String[] args)
            throws Exception {

        System.setProperty(
                "ai.djl.default_engine",
                "PyTorch"
        );

        Engine engine =
                Engine.getEngine("PyTorch");

        System.out.println(
                "Engine: " +
                        engine.getEngineName()
        );

        System.out.println(
                "PyTorch version: " +
                        engine.getVersion()
        );

        Device device = null;

        for (Device d : engine.getDevices()) {
            if (d.isGpu()) {
                device = d;
                break;
            }
        }

        if (device == null) {
            Device[] devices = engine.getDevices(1);

            if (devices.length == 0) {
                throw new IllegalStateException(
                        "No compute device available."
                );
            }

            device = devices[0];
        }
        System.out.println(
                "Training device: " + device
        );

        if (device.isGpu()) {
            System.out.println(
                    "GPU acceleration ENABLED."
            );
        } else {
            System.out.println(
                    "WARNING: Running on CPU."
            );
        }

        List<ImageDataset.Example> dataset =
                new ArrayList<>();

        dataset.addAll(
                ImageDataset.load(
                        new File(
                                "training/dataset/car"
                        ),
                        1.0f
                )
        );

        dataset.addAll(
                ImageDataset.load(
                        new File(
                                "training/dataset/not_car"
                        ),
                        0.0f
                )
        );

        if (dataset.isEmpty()) {
            throw new IllegalStateException(
                    "Dataset is empty."
            );
        }

        Collections.shuffle(dataset);

        System.out.println(
                "Training examples: " +
                        dataset.size()
        );

        try (NDManager manager =
                     engine.newBaseManager(device)) {

            ArrayDataset trainingSet =
                    ImageDataset.createDataset(
                            manager,
                            dataset
                    );

            NeuralNetwork network =
                    new NeuralNetwork();

            DefaultTrainingConfig config =
                    new DefaultTrainingConfig(
                            Loss.softmaxCrossEntropyLoss()
                    )
                            .optOptimizer(
                                    Adam.builder()
                                            .optLearningRateTracker(
                                                    Tracker.fixed(
                                                            LEARNING_RATE
                                                    )
                                            )
                                            .build()
                            )
                            .addEvaluator(
                                    new Accuracy()
                            )
                            .optDevices(
                                    new Device[]{device}
                            )
                            .addTrainingListeners(
                                    new ProgressListener(EPOCHS)
                            );

            try (ai.djl.training.Trainer djlTrainer =
                         network.getModel()
                                 .newTrainer(config)) {

                djlTrainer.initialize(
                        new Shape(
                                ImageDataset.BATCH_SIZE,
                                3,
                                ImageDataset.MODEL_SIZE,
                                ImageDataset.MODEL_SIZE
                        )
                );

                System.out.println();
                System.out.println(
                        "Starting training..."
                );

                EasyTrain.fit(
                        djlTrainer,
                        EPOCHS,
                        trainingSet,
                        null
                );
            }

            File modelDirectory =
                    new File(MODEL_DIR);

            if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
                throw new IOException(
                        "Failed to create model directory: " +
                                modelDirectory.getAbsolutePath()
                );
            }

            File paramsFile =
                    new File(
                            MODEL_DIR,
                            NeuralNetwork.MODEL_NAME + ".params"
                    );

            try (DataOutputStream output =
                         new DataOutputStream(
                                 new FileOutputStream(paramsFile)
                         )) {

                network.getModel()
                        .getBlock()
                        .saveParameters(output);
            }

            System.out.println();
            System.out.println(
                    "Training finished."
            );

            System.out.println(
                    "Model saved to: " +
                            modelDirectory
                                    .getAbsolutePath()
            );

            network.close();
        }
    }
}