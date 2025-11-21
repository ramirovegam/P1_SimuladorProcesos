package principal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class SimulationPanel extends JPanel {

    private final FIFOPageReplacement.Result result;

    // Geometría y estilo
    private final int leftMargin = 220;   // espacio para "Paso i: entra X"
    private final int topMargin = 20;
    private final int rightMargin = 30;
    private final int bottomMargin = 72;  // más espacio para métricas

    private final int cellW = 70;
    private final int cellH = 44;
    private final int colGap = 10;
    private final int rowGap = 16;

    private final Font labelFont = new Font("SansSerif", Font.PLAIN, 14);
    private final Font cellFont = new Font("SansSerif", Font.BOLD, 16);

    public SimulationPanel(FIFOPageReplacement.Result result) {
        this.result = result;
        setBackground(Color.WHITE);
        // Activa tooltips; el texto se calcula en getToolTipText(...)
        setToolTipText("");
        // (Opcional) ajuste de tiempos del tooltip
        ToolTipManager.sharedInstance().setInitialDelay(150);
        ToolTipManager.sharedInstance().setReshowDelay(50);
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = result.snapshots.size();
        int gridWidth = result.frameCount * cellW + (result.frameCount - 1) * colGap;
        int width = leftMargin + gridWidth + rightMargin;

        int gridHeight = rows * cellH + (rows - 1) * rowGap;
        int height = topMargin + gridHeight + bottomMargin + 36; // espacio extra para métricas
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        Graphics2D g = (Graphics2D) gRaw;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < result.snapshots.size(); i++) {
            String label = result.labels.get(i);
            String[] frames = result.snapshots.get(i);
            boolean faultRow = result.faultsPerRow.get(i);
            int changedIndex = result.changedIndexPerRow.get(i);

            int yTop = topMargin + i * (cellH + rowGap);

            // Fondo suave si hubo falla de página en esta fila
            if (faultRow) {
                g.setColor(new Color(255, 235, 238)); // rojo muy suave
                int fullW = getPreferredSize().width - 20;
                g.fillRoundRect(10, yTop - 4, fullW, cellH + 8, 10, 10);
            }

            // Etiqueta del paso
            drawLabel(g, label, 20, yTop + cellH - 14);

            // Filas de marcos (celdas)
            drawFramesRow(g, frames, leftMargin, yTop, changedIndex);
        }

        // Resultado final y métricas
        String[] finalFrames = result.finalFrames();
        String finalText = "✅ Resultado final: " + FIFOPageReplacement.Result.formatFrames(finalFrames);
        g.setFont(labelFont.deriveFont(Font.BOLD));
        g.setColor(new Color(0, 128, 0));
        g.drawString(finalText, 20, getPreferredSize().height - 48);

        String metrics = String.format("Fallas: %d   |   Hits: %d   |   Pasos: %d",
                result.totalFaults, result.totalHits, result.snapshots.size() - 1);
        g.setFont(labelFont);
        g.setColor(Color.DARK_GRAY);
        g.drawString(metrics, 20, getPreferredSize().height - 24);
    }

    private void drawLabel(Graphics2D g, String text, int x, int baselineY) {
        g.setFont(labelFont);
        g.setColor(Color.DARK_GRAY);
        g.drawString(text, x, baselineY);
    }

    private void drawFramesRow(Graphics2D g, String[] frames, int xStart, int yTop, int changedIndex) {
        for (int c = 0; c < frames.length; c++) {
            int x = xStart + c * (cellW + colGap);
            // celda base
            g.setColor(new Color(230, 230, 230));
            g.fillRoundRect(x, yTop, cellW, cellH, 8, 8);
            g.setColor(Color.GRAY);
            g.drawRoundRect(x, yTop, cellW, cellH, 8, 8);

            // contenido
            String s = (frames[c] == null) ? "null" : frames[c];
            drawCenteredString(g, s, new Rectangle(x, yTop, cellW, cellH),
                    cellFont, Color.BLACK);

            // resaltado si esta celda fue actualizada en este paso
            if (changedIndex == c) {
                g.setColor(new Color(255, 87, 34)); // naranja acento
                Stroke old = g.getStroke();
                g.setStroke(new BasicStroke(2.0f));
                g.drawRoundRect(x - 2, yTop - 2, cellW + 4, cellH + 4, 10, 10);
                g.setStroke(old);
            }
        }
    }

    private void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font, Color color) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    /** Tooltips por celda: muestra Hit/Falla y contenido antes/después si cambió. */
    @Override
    public String getToolTipText(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        // Localiza la fila (estado)
        for (int i = 0; i < result.snapshots.size(); i++) {
            int yTop = topMargin + i * (cellH + rowGap);
            if (mouseY >= yTop && mouseY <= yTop + cellH) {
                // Localiza la columna (marco)
                for (int c = 0; c < result.frameCount; c++) {
                    int xLeft = leftMargin + c * (cellW + colGap);
                    if (mouseX >= xLeft && mouseX <= xLeft + cellW) {
                        // Construye tooltip
                        String label = result.labels.get(i);
                        boolean faultRow = result.faultsPerRow.get(i);
                        int changedIndex = result.changedIndexPerRow.get(i);
                        String[] framesNow = result.snapshots.get(i);
                        String cellNow = framesNow[c] == null ? "null" : framesNow[c];

                        // Para comparar antes/después, toma la fila previa si existe
                        String before = null;
                        if (i > 0) {
                            String[] framesPrev = result.snapshots.get(i - 1);
                            before = framesPrev[c] == null ? "null" : framesPrev[c];
                        }

                        StringBuilder tip = new StringBuilder();
                        tip.append(label);
                        if (i == 0) {
                            tip.append("  (inicial)");
                            tip.append("  | Marco ").append(c).append(": ").append(cellNow);
                            return tip.toString();
                        }

                        tip.append(faultRow ? "  → Falla de página" : "  → Hit");
                        tip.append("  | Marco ").append(c).append(": ").append(cellNow);

                        if (changedIndex == c) {
                            tip.append("  (actualizado");
                            if (before != null) tip.append(", antes: ").append(before);
                            tip.append(")");
                        } else if (before != null && !Objects.equals(before, cellNow)) {
                            // Cambió pero no es el marco marcado (raro); lo anotamos por claridad
                            tip.append("  (cambió de ").append(before).append(")");
                        }
                        return tip.toString();
                    }
                }
                // Si clicas en la zona de etiqueta (izquierda), muestra info del paso
                if (mouseX >= 10 && mouseX <= leftMargin - 8) {
                    String label = result.labels.get(i);
                    boolean faultRow = result.faultsPerRow.get(i);
                    return label + (i == 0 ? "" : (faultRow ? "  → Falla" : "  → Hit"));
                }
            }
        }
        return null;
    }
}