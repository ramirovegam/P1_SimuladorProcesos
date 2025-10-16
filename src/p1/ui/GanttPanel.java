package p1.ui;

import p1.engine.Segment;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GanttPanel extends JPanel {

    private List<Segment> segments = Collections.emptyList();

    public void setSegments(List<Segment> segments) {
        this.segments = segments != null ? segments : Collections.emptyList();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (segments.isEmpty()) {
            // Mensaje vacío
            g.setColor(Color.GRAY);
            g.drawString("Sin datos de simulación", 10, 20);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int margen = 30; // espacio para eje temporal

        // Calcular tiempo total
        int tMax = segments.stream().mapToInt(Segment::getFin).max().orElse(0);
        if (tMax == 0) return;

        // Escala horizontal
        double pxPorUnidad = (w - 20.0) / tMax;

        // Paleta por proceso
        Map<String, Color> colores = new HashMap<>();
        Color idleColor = new Color(220, 220, 220);
        Random rnd = new Random(42);

        int y = margen;
        int altoBarra = Math.max(20, h - margen - 35);

        // Dibujar cada segmento como rectángulo
        for (Segment s : segments) {
            int x = (int) Math.round(s.getInicio() * pxPorUnidad);
            int ancho = Math.max(1, (int) Math.round(s.getDuracion() * pxPorUnidad));

            Color c;
            if ("IDLE".equals(s.getProcesoId())) {
                c = idleColor;
            } else {
                c = colores.computeIfAbsent(s.getProcesoId(), k ->
                        new Color(60 + rnd.nextInt(160), 60 + rnd.nextInt(160), 60 + rnd.nextInt(160)));
            }

            g2.setColor(c);
            g2.fillRect(x + 10, y, ancho, altoBarra);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(x + 10, y, ancho, altoBarra);

            // Etiqueta del proceso
            g2.setColor(Color.BLACK);
            String label = s.getProcesoId();
            int tx = x + 12;
            int ty = y + altoBarra / 2 + g2.getFontMetrics().getAscent()/2 - 2;
            g2.drawString(label, tx, ty);
        }

        // Eje de tiempo (marcas cada 1 unidad, simplificado)
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(10, y + altoBarra + 10, w - 10, y + altoBarra + 10);
        for (int t = 0; t <= tMax; t++) {
            int x = (int) Math.round(t * pxPorUnidad) + 10;
            g2.drawLine(x, y + altoBarra + 8, x, y + altoBarra + 12);
            if (t % Math.max(1, tMax / 10) == 0) {
                String txt = String.valueOf(t);
                int tw = g2.getFontMetrics().stringWidth(txt);
                g2.drawString(txt, x - tw/2, y + altoBarra + 25);
            }
        }
        g2.dispose();
    }
}