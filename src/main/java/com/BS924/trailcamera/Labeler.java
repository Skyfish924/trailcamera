package com.BS924.trailcamera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Labeler extends JFrame {

    private final File[] images;
    private int imageIndex = 0;

    private BufferedImage image;
    private final List<Rectangle> boxes = new ArrayList<>();

    private Point start;
    private Point end;

    private final JPanel panel;

    private static final Logger logger =
            LoggerFactory.getLogger(Labeler.class);

    public Labeler(File directory) throws Exception {

        images = directory.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png")
        );

        if (images == null || images.length == 0) {
            throw new IllegalStateException(
                    "No PNG images found in: " + directory.getAbsolutePath()
            );
        }

        Arrays.sort(images, Comparator.comparing(File::getName));

        setTitle("TrailCamera Labeler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (image == null) {
                    return;
                }

                g.drawImage(
                        image,
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        null
                );

                double scaleX = (double) getWidth() / image.getWidth();
                double scaleY = (double) getHeight() / image.getHeight();

                g.setColor(Color.RED);

                for (Rectangle box : boxes) {
                    int x = (int) (box.x * scaleX);
                    int y = (int) (box.y * scaleY);
                    int w = (int) (box.width * scaleX);
                    int h = (int) (box.height * scaleY);

                    g.drawRect(x, y, w, h);
                }

                if (start != null && end != null) {
                    int x = Math.min(start.x, end.x);
                    int y = Math.min(start.y, end.y);
                    int w = Math.abs(end.x - start.x);
                    int h = Math.abs(end.y - start.y);

                    g.setColor(Color.YELLOW);
                    g.drawRect(x, y, w, h);
                }

                g.setColor(Color.WHITE);
                g.drawString(
                        "Frame " + (imageIndex + 1) + "/" + images.length
                                + " | Cars: " + boxes.size()
                                + " | S = Save | N = Next | R = Undo | C = Clear",
                        10,
                        20
                );
            }
        };

        panel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
                end = start;
                panel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (start == null) {
                    return;
                }

                end = e.getPoint();

                int displayX = Math.min(start.x, end.x);
                int displayY = Math.min(start.y, end.y);
                int displayW = Math.abs(end.x - start.x);
                int displayH = Math.abs(end.y - start.y);

                if (displayW > 5 && displayH > 5) {

                    double scaleX =
                            (double) image.getWidth() / panel.getWidth();

                    double scaleY =
                            (double) image.getHeight() / panel.getHeight();

                    boxes.add(new Rectangle(
                            (int) (displayX * scaleX),
                            (int) (displayY * scaleY),
                            (int) (displayW * scaleX),
                            (int) (displayH * scaleY)
                    ));
                }

                start = null;
                end = null;

                panel.repaint();
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                end = e.getPoint();
                panel.repaint();
            }
        });

        panel.setFocusable(true);

        panel.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                switch (e.getKeyCode()) {

                    case KeyEvent.VK_S:
                        saveLabels();
                        break;

                    case KeyEvent.VK_N:
                        saveLabels();
                        nextImage();
                        break;

                    case KeyEvent.VK_R:
                        if (!boxes.isEmpty()) {
                            boxes.removeLast();
                            panel.repaint();
                        }
                        break;

                    case KeyEvent.VK_C:
                        boxes.clear();
                        panel.repaint();
                        break;
                }
            }
        });

        add(panel);

        setSize(1920, 1080);
        setLocationRelativeTo(null);

        loadImage();

        SwingUtilities.invokeLater(panel::requestFocusInWindow);
    }

    private void loadImage() throws Exception {

        if (imageIndex >= images.length) {
            JOptionPane.showMessageDialog(
                    this,
                    "Finished labeling all frames!"
            );

            dispose();
            return;
        }

        File imageFile = images[imageIndex];

        image = ImageIO.read(imageFile);
        boxes.clear();

        File labelFile = getLabelFile(imageFile);

        if (labelFile.exists()) {
            loadLabels(labelFile);
        }

        setTitle(
                "TrailCamera Labeler - "
                        + imageFile.getName()
        );

        panel.repaint();
    }

    private void loadLabels(File labelFile) throws Exception {

        try (java.util.Scanner scanner =
                     new java.util.Scanner(labelFile)) {

            while (scanner.hasNextInt()) {

                int x = scanner.nextInt();
                int y = scanner.nextInt();
                int width = scanner.nextInt();
                int height = scanner.nextInt();

                boxes.add(
                        new Rectangle(x, y, width, height)
                );
            }
        }
    }

    private void saveLabels() {

        File imageFile = images[imageIndex];
        File labelFile = getLabelFile(imageFile);

        try (PrintWriter writer =
                     new PrintWriter(labelFile)) {

            for (Rectangle box : boxes) {

                writer.println(
                        box.x + " "
                                + box.y + " "
                                + box.width + " "
                                + box.height
                );
            }

            System.out.println(
                    "Saved "
                            + boxes.size()
                            + " car(s): "
                            + labelFile.getName()
            );

        } catch (Exception e) {
            logger.error("Something went wrong", e);
        }
    }

    private File getLabelFile(File imageFile) {

        String name = imageFile.getName();

        name = name.substring(
                0,
                name.lastIndexOf('.')
        );

        return new File(
                imageFile.getParent(),
                name + ".txt"
        );
    }

    private void nextImage() {

        imageIndex++;

        try {
            loadImage();
        } catch (Exception e) {
            logger.error("Something went wrong", e);
        }
    }

    public static void main(String[] args) throws Exception {

        File frames = new File("training/frames");

        if (!frames.exists()) {
            throw new IllegalStateException(
                    "Frames directory not found: "
                            + frames.getAbsolutePath()
            );
        }

        new Labeler(frames).setVisible(true);
    }
}