package com.BS924.trailcamera;

import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener;

public class ProgressListener implements TrainingListener {

    private final int totalEpochs;
    private int currentEpoch = 0;

    public ProgressListener(int totalEpochs) {
        this.totalEpochs = totalEpochs;
    }

    @Override
    public void onEpoch(Trainer trainer) {
        currentEpoch++;

        int percent =
                (currentEpoch * 100) / totalEpochs;

        int filled =
                percent / 5;

        String bar =
                "#".repeat(filled) +
                        "-".repeat(20 - filled);

        System.out.printf(
                "\rEpoch %d/%d [%s] %d%%",
                currentEpoch,
                totalEpochs,
                bar,
                percent
        );

        if (currentEpoch == totalEpochs) {
            System.out.println();
        }
    }

    @Override
    public void onTrainingBegin(Trainer trainer) {
        System.out.println("Training started.");
    }

    @Override
    public void onTrainingEnd(Trainer trainer) {
        System.out.println("Training complete.");
    }

    @Override
    public void onTrainingBatch(
            Trainer trainer,
            TrainingListener.BatchData batchData
    ) {
    }

    @Override
    public void onValidationBatch(
            Trainer trainer,
            TrainingListener.BatchData batchData
    ) {
    }
}