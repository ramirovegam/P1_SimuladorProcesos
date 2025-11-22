
package principal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de simulación visual para algoritmos de reemplazo de páginas.
 * - Grilla de marcos por paso (snapshots).
 * - Resalta fallas (fondo rojo suave).
 * - Marca el marco actualizado (borde naranja).
 * - Muestra métricas al pie.
 * - Extras:
 *    * Clock: puntero (triángulo) + useBits (u=0/1).
 *    * Second Chance: refBits (u=0/1).
 * - Explicación textual por paso (debajo de cada fila), generada en tiempo de dibujo,
 *   sin modificar las clases de algoritmo.
 */
public class SimulationPanel extends JPanel {

    /** Resultado base en formato FIFO (siempre). */
    private final FIFOPageReplacement.Result result;

    /**
     * Resultado completo del algoritmo (opcional).
     * Ej: ClockPageReplacement.Result o SecondChancePageReplacement.Result.
     * Se usa para dibujar puntero/bits y para generar explicaciones más ricas.
     */
    private final Object fullResult;

    // Geometría/estilo
    private final int leftMargin   = 220;
    private final int topMargin    = 20;
    private final int rightMargin  = 30;
    private final int bottomMargin = 92;  // + espacio extra para métricas y explicaciones
    private final int cellW        = 70;
    private final int cellH        = 44;
    private final int colGap       = 10;
    private final int rowGap       = 18;
    private final Font labelFont   = new Font("SansSerif", Font.PLAIN, 14);
    private final Font cellFont    = new Font("SansSerif", Font.BOLD, 16);
    private final Font explainFont = new Font("SansSerif", Font.ITALIC, 12);
    private final Color explainColor = new Color(80, 80, 80);
    private final int explainLineGap = 4;

    public SimulationPanel(FIFOPageReplacement.Result result) {
        this(result, null);
    }

    public SimulationPanel(FIFOPageReplacement.Result result, Object fullResult) {
        this.result = result;
        this.fullResult = fullResult;
        setBackground(Color.WHITE);
        setToolTipText(""); // activa tooltips
        ToolTipManager.sharedInstance().setInitialDelay(150);
        ToolTipManager.sharedInstance().setReshowDelay(50);
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = result.snapshots.size();
        int gridWidth  = result.frameCount * cellW + (result.frameCount - 1) * colGap;
        int width      = leftMargin + gridWidth + rightMargin;

        int gridHeight = rows * cellH + (rows - 1) * rowGap;

        // altura extra por explicaciones
        int extra = 0;
        FontMetrics fm = getFontMetrics(explainFont);
        int explainH = fm.getHeight();
        for (int i = 0; i < rows; i++) {
            List<String> lines = buildExplanationLines(i);
            if (!lines.isEmpty()) {
                extra += explainH * lines.size() + explainLineGap;
            }
        }

        int height     = topMargin + gridHeight + extra + bottomMargin + 36;
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        Graphics2D g = (Graphics2D) gRaw;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontMetrics fmExplain = g.getFontMetrics(explainFont);
        int yCursor = topMargin;

        for (int i = 0; i < result.snapshots.size(); i++) {
            String   label        = result.labels.get(i);
            String[] frames       = result.snapshots.get(i);
            boolean  faultRow     = result.faultsPerRow.get(i);
            int      changedIndex = result.changedIndexPerRow.get(i);

            int rowTop = yCursor;

            // fondo de falla
            if (faultRow) {
                g.setColor(new Color(255, 235, 238));
                int fullW = getPreferredSize().width - 20;
                g.fillRoundRect(10, rowTop - 4, fullW, cellH + 8, 10, 10);
            }

            // etiqueta del paso
            drawLabel(g, label, 20, rowTop + cellH - 14);

            // grilla + extras
            drawFramesRow(g, frames, leftMargin, rowTop, changedIndex, i);

            // explicación textual
            List<String> lines = buildExplanationLines(i);
            if (!lines.isEmpty()) {
                int xText = leftMargin;
                int yText = rowTop + cellH + fmExplain.getAscent() + 6;
                g.setFont(explainFont);
                g.setColor(explainColor);
                for (String line : lines) {
                    g.drawString(line, xText, yText);
                    yText += fmExplain.getHeight();
                }
                yCursor = yText + explainLineGap;
            } else {
                yCursor = rowTop + cellH + rowGap;
            }
        }

        // final y métricas
        String[] finalFrames = result.finalFrames();
        String finalText = "✅ Resultado final: " + FIFOPageReplacement.Result.formatFrames(finalFrames);
        g.setFont(labelFont.deriveFont(Font.BOLD));
        g.setColor(new Color(0, 128, 0));
        g.drawString(finalText, 20, getPreferredSize().height - 48);

        String metrics = String.format("Fallas: %d | Hits: %d | Pasos: %d",
                result.totalFaults, result.totalHits, Math.max(0, result.snapshots.size() - 1));
        g.setFont(labelFont);
        g.setColor(Color.DARK_GRAY);
        g.drawString(metrics, 20, getPreferredSize().height - 24);
    }

    private void drawLabel(Graphics2D g, String text, int x, int baselineY) {
        g.setFont(labelFont);
        g.setColor(Color.DARK_GRAY);
        g.drawString(text, x, baselineY);
    }

    private void drawFramesRow(Graphics2D g,
                               String[] frames,
                               int xStart,
                               int yTop,
                               int changedIndex,
                               int stepIndex) {

        ClockPageReplacement.Result clock = asClock();
        SecondChancePageReplacement.Result sc = asSecondChance();

        int[]   useBits = null; // u=0/1 (Clock: useBits, SC: refBits)
        Integer pointer = null; // solo Clock

        // Clock extras
        if (clock != null) {
            if (clock.useBitsPerRow != null && stepIndex < clock.useBitsPerRow.size()) {
                useBits = clock.useBitsPerRow.get(stepIndex);
            }
            if (clock.pointerPerRow != null && stepIndex < clock.pointerPerRow.size()) {
                pointer = clock.pointerPerRow.get(stepIndex);
                if (pointer != null && (pointer < 0 || pointer >= result.frameCount)) {
                    pointer = null;
                }
            }
        }

        // SC extras
        if (sc != null && sc.refBitsPerRow != null && stepIndex < sc.refBitsPerRow.size()) {
            useBits = sc.refBitsPerRow.get(stepIndex);
        }

        for (int c = 0; c < frames.length; c++) {
            int x = xStart + c * (cellW + colGap);

            // celda base
            g.setColor(new Color(230, 230, 230));
            g.fillRoundRect(x, yTop, cellW, cellH, 8, 8);
            g.setColor(Color.GRAY);
            g.drawRoundRect(x, yTop, cellW, cellH, 8, 8);

            // texto principal
            String s = (frames[c] == null) ? "null" : frames[c];
            drawCenteredString(g, s, new Rectangle(x, yTop, cellW, cellH), cellFont, Color.BLACK);

            // u=0/1
            if (useBits != null && c < useBits.length) {
                String ub = "u=" + useBits[c];
                Font small = labelFont.deriveFont(Font.PLAIN, 11f);
                g.setFont(small);
                g.setColor(new Color(90, 90, 90, 170));
                g.drawString(ub, x + 6, yTop + cellH - 6);
            }

            // celda actualizada
            if (changedIndex == c) {
                g.setColor(new Color(255, 87, 34));
                Stroke old = g.getStroke();
                g.setStroke(new BasicStroke(2.0f));
                g.drawRoundRect(x - 2, yTop - 2, cellW + 4, cellH + 4, 10, 10);
                g.setStroke(old);
            }

            // puntero (solo Clock)
            if (pointer != null && pointer == c) {
                int triW = 14, triH = 10;
                int px = x + cellW / 2;
                int py = yTop - 8;
                Polygon tri = new Polygon(
                        new int[]{px, px - triW / 2, px + triW / 2},
                        new int[]{py, py - triH, py - triH},
                        3
                );
                g.setColor(new Color(255, 140, 0));
                g.fillPolygon(tri);
                g.setColor(new Color(120, 60, 0));
                g.drawPolygon(tri);
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

    /** Tooltips por celda enriquecidos con bit y puntero si existen. (ÚNICA implementación) */
    @Override
    public String getToolTipText(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        for (int i = 0; i < result.snapshots.size(); i++) {
            int yTop = topMargin + i * (cellH + rowGap);

            if (mouseY >= yTop && mouseY <= yTop + cellH) {
                for (int c = 0; c < result.frameCount; c++) {
                    int xLeft = leftMargin + c * (cellW + colGap);
                    if (mouseX >= xLeft && mouseX <= xLeft + cellW) {
                        String label       = result.labels.get(i);
                        boolean faultRow   = result.faultsPerRow.get(i);
                        int changedIndex   = result.changedIndexPerRow.get(i);
                        String[] framesNow = result.snapshots.get(i);
                        String cellNow     = framesNow[c] == null ? "null" : framesNow[c];

                        String before = null;
                        if (i > 0) {
                            String[] framesPrev = result.snapshots.get(i - 1);
                            before = framesPrev[c] == null ? "null" : framesPrev[c];
                        }

                        ClockPageReplacement.Result clock = asClock();
                        SecondChancePageReplacement.Result sc = asSecondChance();
                        Integer pointer = null;
                        Integer bit     = null;

                        if (clock != null) {
                            if (clock.pointerPerRow != null && i < clock.pointerPerRow.size()) {
                                pointer = clock.pointerPerRow.get(i);
                            }
                            if (clock.useBitsPerRow != null && i < clock.useBitsPerRow.size()
                                    && c < clock.useBitsPerRow.get(i).length) {
                                bit = clock.useBitsPerRow.get(i)[c];
                            }
                        } else if (sc != null) {
                            if (sc.refBitsPerRow != null && i < sc.refBitsPerRow.size()
                                    && c < sc.refBitsPerRow.get(i).length) {
                                bit = sc.refBitsPerRow.get(i)[c];
                            }
                        }

                        StringBuilder tip = new StringBuilder();
                        tip.append(label);
                        if (i == 0) {
                            tip.append(" (inicial)");
                            tip.append("\nMarco ").append(c).append(": ").append(cellNow);
                        } else {
                            tip.append(faultRow ? " → Falla" : " → Hit");
                            tip.append("\nMarco ").append(c).append(": ").append(cellNow);
                            if (changedIndex == c) {
                                tip.append(" (actualizado");
                                if (before != null) tip.append(", antes: ").append(before);
                                tip.append(")");
                            } else if (before != null && !Objects.equals(before, cellNow)) {
                                tip.append(" (cambió de ").append(before).append(")");
                            }
                        }
                        if (bit != null) tip.append(" | u=").append(bit);
                        if (pointer != null && pointer == c) tip.append(" | Puntero");

                        return tip.toString();
                    }
                }
                // zona izquierda: info del paso
                if (mouseX >= 10 && mouseX <= leftMargin - 8) {
                    String label     = result.labels.get(i);
                    boolean faultRow = result.faultsPerRow.get(i);
                    return label + (i == 0 ? "" : (faultRow ? " → Falla" : " → Hit"));
                }
            }
        }
        return null;
    }

    // ===== Explicación por fila (sin cambiar las clases de algoritmo) =====
    private List<String> buildExplanationLines(int stepIndex) {
        List<String> lines = new ArrayList<>();
        if (stepIndex == 0) {
            ClockPageReplacement.Result clock = asClock();
            if (clock != null && clock.pointerPerRow != null && !clock.pointerPerRow.isEmpty()) {
                Integer p = clock.pointerPerRow.get(0);
                lines.add("Estado inicial: todos los marcos vacíos" +
                        (p != null ? (", puntero en posición " + p + ".") : "."));
            } else {
                lines.add("Estado inicial: todos los marcos vacíos.");
            }
            return lines;
        }

        String label = result.labels.get(stepIndex);
        boolean fault = result.faultsPerRow.get(stepIndex);
        int changed = result.changedIndexPerRow.get(stepIndex);
        String page = (result.pagePerRow != null && stepIndex < result.pagePerRow.size())
                ? result.pagePerRow.get(stepIndex) : null;

        ClockPageReplacement.Result clock = asClock();
        SecondChancePageReplacement.Result sc = asSecondChance();

        if (clock != null) {
            Integer pointerAfter = (clock.pointerPerRow != null && stepIndex < clock.pointerPerRow.size())
                    ? clock.pointerPerRow.get(stepIndex) : null;
            int[] bitsNow = (clock.useBitsPerRow != null && stepIndex < clock.useBitsPerRow.size())
                    ? clock.useBitsPerRow.get(stepIndex) : null;
            int[] bitsPrev = (clock.useBitsPerRow != null && stepIndex - 1 < clock.useBitsPerRow.size() && stepIndex > 0)
                    ? clock.useBitsPerRow.get(stepIndex - 1) : null;

            if (fault) {
                if (changed >= 0 && stepIndex > 0) {
                    List<Integer> gaveSecondChance = new ArrayList<>();
                    if (bitsPrev != null && bitsNow != null) {
                        for (int i = 0; i < Math.min(bitsPrev.length, bitsNow.length); i++) {
                            if (bitsPrev[i] == 1 && bitsNow[i] == 0 && i != changed) {
                                gaveSecondChance.add(i);
                            }
                        }
                    }
                    if (!gaveSecondChance.isEmpty()) {

                        lines.add(String.format(
                            "Sin huecos: el puntero recorre marcos con u=1\n" +
                            "y les da segunda oportunidad (u=0): %s.",
                            gaveSecondChance.toString()
                        ));

                    }
                    lines.add(String.format("Reemplaza en marco %d por %s.", changed, String.valueOf(page)));
                } else {
                    lines.add(String.format("Hueco disponible: coloca %s.", String.valueOf(page)));
                }
                if (pointerAfter != null) lines.add("Próximo puntero: posición " + pointerAfter + ".");
            } else {
                lines.add(String.format("Hit en %s: se marca u=1 en su marco.", String.valueOf(page)));
                if (pointerAfter != null) lines.add("Puntero se mantiene: posición " + pointerAfter + ".");
            }
            return lines;
        }

        if (sc != null) {
            int[] bitsNow = (sc.refBitsPerRow != null && stepIndex < sc.refBitsPerRow.size())
                    ? sc.refBitsPerRow.get(stepIndex) : null;
            int[] bitsPrev = (sc.refBitsPerRow != null && stepIndex - 1 < sc.refBitsPerRow.size() && stepIndex > 0)
                    ? sc.refBitsPerRow.get(stepIndex - 1) : null;

            if (fault) {
                if (changed >= 0) {
                    List<Integer> gaveSecondChance = new ArrayList<>();
                    if (bitsPrev != null && bitsNow != null) {
                        for (int i = 0; i < Math.min(bitsPrev.length, bitsNow.length); i++) {
                            if (bitsPrev[i] == 1 && bitsNow[i] == 0 && i != changed) {
                                gaveSecondChance.add(i);
                            }
                        }
                    }
                    if (!gaveSecondChance.isEmpty()) {
                        lines.add("Segunda oportunidad (cola FIFO): marcos con u=1 pasan a u=0 y se reencolan: " + gaveSecondChance + ".");
                    }
                    lines.add(String.format("Reemplaza en marco %d por %s.", changed, String.valueOf(page)));
                } else {
                    lines.add(String.format("Hueco disponible: coloca %s.", String.valueOf(page)));
                }
            } else {
                lines.add(String.format("Hit en %s: su marco queda u=1.", String.valueOf(page)));
            }
            return lines;
        }

        // Genérico (FIFO, LRU, Óptimo, etc.)
        if (fault) {
            if (changed >= 0) lines.add(String.format("Falla: reemplaza en marco %d por %s.", changed, String.valueOf(page)));
            else               lines.add(String.format("Falla: coloca %s en hueco disponible.", String.valueOf(page)));
        } else {
            lines.add(String.format("Hit: no hay reemplazo para %s.", String.valueOf(page)));
        }
        return lines;
    }

    // ===== Helpers de casteo =====
    private ClockPageReplacement.Result asClock() {
        return (fullResult instanceof ClockPageReplacement.Result)
                ? (ClockPageReplacement.Result) fullResult : null;
    }

    private SecondChancePageReplacement.Result asSecondChance() {
        return (fullResult instanceof SecondChancePageReplacement.Result)
                ? (SecondChancePageReplacement.Result) fullResult : null;
    }
}
