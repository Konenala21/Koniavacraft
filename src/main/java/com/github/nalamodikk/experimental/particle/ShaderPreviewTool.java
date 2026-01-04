package com.github.nalamodikk.experimental.particle;// ========================================
// 在 IDEA 中創建簡單的 OpenGL 預覽視窗
// ========================================

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;

/**
 * 簡單的著色器預覽工具（可以在 IDEA 中運行）
 */
public class ShaderPreviewTool {

    public static void main(String[] args) {
        // 這是一個獨立的 Java 應用程式，可以在 IDEA 中直接運行
        SwingUtilities.invokeLater(() -> {
            try {
                new ShaderPreviewWindow().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

/**
 * 著色器預覽視窗
 */
class ShaderPreviewWindow extends JFrame {
    private ShaderPreviewPanel glPanel;
    private JSlider ringCountSlider;
    private JSlider complexitySlider;
    private JSlider brightnessSlider;

    public ShaderPreviewWindow() throws HeadlessException {
        initComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initComponents() {
        setTitle("Koniavacraft 魔法陣著色器預覽");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // OpenGL 面板
        glPanel = new ShaderPreviewPanel();

        // 控制滑桿
        ringCountSlider = new JSlider(1, 12, 5);
        complexitySlider = new JSlider(10, 200, 80);  // *0.1 = 1.0 to 20.0
        brightnessSlider = new JSlider(10, 300, 100); // *0.01 = 0.1 to 3.0

        ringCountSlider.setMajorTickSpacing(2);
        ringCountSlider.setPaintTicks(true);
        ringCountSlider.setPaintLabels(true);

        complexitySlider.setMajorTickSpacing(50);
        complexitySlider.setPaintTicks(true);
        complexitySlider.setPaintLabels(true);

        brightnessSlider.setMajorTickSpacing(50);
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 中央 OpenGL 面板
        add(glPanel, BorderLayout.CENTER);

        // 右側控制面板
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        controlPanel.add(new JLabel("圓環數量:"), gbc);
        gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(ringCountSlider, gbc);

        gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(new JLabel("複雜度:"), gbc);
        gbc.gridy = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(complexitySlider, gbc);

        gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        controlPanel.add(new JLabel("亮度:"), gbc);
        gbc.gridy = 5; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(brightnessSlider, gbc);

        // 按鈕
        gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE;
        JButton exportButton = new JButton("匯出參數");
        controlPanel.add(exportButton, gbc);

        exportButton.addActionListener(e -> exportParameters());

        add(controlPanel, BorderLayout.EAST);
    }

    private void setupEventHandlers() {
        // 滑桿變化時更新著色器參數
        ChangeListener parameterUpdater = e -> updateShaderParameters();

        ringCountSlider.addChangeListener(parameterUpdater);
        complexitySlider.addChangeListener(parameterUpdater);
        brightnessSlider.addChangeListener(parameterUpdater);
    }

    private void updateShaderParameters() {
        if (glPanel != null) {
            int ringCount = ringCountSlider.getValue();
            float complexity = complexitySlider.getValue() * 0.1f;
            float brightness = brightnessSlider.getValue() * 0.01f;

            glPanel.updateParameters(ringCount, complexity, brightness);
        }
    }

    private void exportParameters() {
        int ringCount = ringCountSlider.getValue();
        float complexity = complexitySlider.getValue() * 0.1f;
        float brightness = brightnessSlider.getValue() * 0.01f;

        String parameters = String.format(
                "魔法陣參數:\n" +
                        "ringCount = %d\n" +
                        "complexity = %.1f\n" +
                        "brightness = %.2f\n",
                ringCount, complexity, brightness
        );

        // 複製到剪貼簿
        StringSelection selection = new StringSelection(parameters);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);

        JOptionPane.showMessageDialog(this, "參數已複製到剪貼簿!");
    }
}

/**
 * OpenGL 渲染面板（簡化版）
 */
class ShaderPreviewPanel extends JPanel {
    private BufferedImage previewImage;
    private int ringCount = 5;
    private float complexity = 8.0f;
    private float brightness = 1.0f;

    public ShaderPreviewPanel() {
        setPreferredSize(new Dimension(512, 512));
        generatePreviewImage();

        // 定時重繪（模擬動畫）
        Timer timer = new Timer(50, e -> {
            generatePreviewImage();
            repaint();
        });
        timer.start();
    }

    public void updateParameters(int ringCount, float complexity, float brightness) {
        this.ringCount = ringCount;
        this.complexity = complexity;
        this.brightness = brightness;
    }

    private void generatePreviewImage() {
        int width = 512;
        int height = 512;
        previewImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        float time = System.currentTimeMillis() * 0.001f * 0.05f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 標準化坐標
                float u = (x - width * 0.5f) / (height * 0.5f);
                float v = (y - height * 0.5f) / (height * 0.5f);

                // 計算魔法陣效果（簡化版）
                float intensity = calculateMagicCircleIntensity(u, v, time);

                // 顏色漸變
                float dist = (float) Math.sqrt(u * u + v * v);
                float colorMix = Math.min(dist * 0.8f, 1.0f);

                float r = lerp(0.616f, 0.231f, colorMix) * intensity * brightness;
                float g = lerp(0.275f, 0.510f, colorMix) * intensity * brightness;
                float b = lerp(0.867f, 0.965f, colorMix) * intensity * brightness;
                float a = intensity * 0.8f;

                int color = ((int)(a * 255) << 24) |
                        ((int)(r * 255) << 16) |
                        ((int)(g * 255) << 8) |
                        ((int)(b * 255));

                previewImage.setRGB(x, y, color);
            }
        }
    }

    private float calculateMagicCircleIntensity(float u, float v, float time) {
        float dist = (float) Math.sqrt(u * u + v * v);
        float angle = (float) Math.atan2(v, u);

        float rings = 0;
        for (int i = 0; i < ringCount; i++) {
            float ringRadius = 0.2f + i * 0.25f;
            float thickness = 0.03f + (float) Math.sin(time * 2 + i) * 0.01f;

            float ring = Math.max(0, 1 - Math.abs(dist - ringRadius) / (thickness * 0.5f));
            rings += ring * (0.8f + 0.2f * (float) Math.sin(time * 3 + i * 0.5f));
        }

        // 符文圖案
        float runes = (float) Math.pow(Math.abs(Math.sin(angle * complexity + time * 2)), 3);
        float runeArea = (dist > 0.8f && dist < 1.6f) ? 1.0f : 0.0f;
        runes *= runeArea;

        // 中心發光
        float centerGlow = 1.0f / (1.0f + dist * 8.0f);
        centerGlow *= 0.7f + 0.3f * (float) Math.sin(time * 4);

        // 邊緣衰減
        float edgeFade = Math.max(0, 1 - smoothstep(0.8f, 1.2f, dist));

        return Math.min(1.0f, (rings * 0.6f + runes * 0.8f + centerGlow * 0.4f) * edgeFade);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(Math.max(t, 0), 1);
    }

    private float smoothstep(float edge0, float edge1, float x) {
        float t = Math.min(Math.max((x - edge0) / (edge1 - edge0), 0), 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (previewImage != null) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(previewImage, 0, 0, getWidth(), getHeight(), null);
        }
    }
}