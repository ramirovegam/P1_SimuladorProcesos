package principal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import p1.engine.SimulationResult;
import p1.model.Proceso;
import p1.scheduler.FCFS;
import p1.scheduler.Planificador;
import p1.scheduler.SJF;
import p1.scheduler.SRTF;
import p1.ui.GanttPanel;
import p3.FileSystem;
import p3.TreeAdapter;

/**
 *
 * @author RVega
 */
public class VentanaPrincipal extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName());
    private DefaultTableModel modeloTabla;
    private final GanttPanel gantt = new GanttPanel();
    private SimulationResult ultimoResultado;
    private List<String> listaPaginas;
    private javax.swing.JTextArea resultadosRemplazoArea;

    private enum ModoArchivos {
        FS, ORGANIZACION
    }
    private ModoArchivos modoArchivos = ModoArchivos.ORGANIZACION; // o FS si prefieres

    // --- Panel de archivos en memoria ---
    private javax.swing.JScrollPane jScrollPaneArchivosMemoria;
    private javax.swing.JTable jTableArchivosMemoria;

    // === CLI del sistema de archivos ===
    private FileSystem fs = new FileSystem();  // NO final, para poder reasignarlo en 'reset'
    private boolean updatingTree = false;      // Flag anti-bucle al refrescar el JTree

    // Consola: helper para imprimir líneas
    private void log(String text) {
        jTextAreaConsola.append(text + "\n");
        jTextAreaConsola.setCaretPosition(jTextAreaConsola.getDocument().getLength());
    }

    // Consola: helper para mostrar el prompt
    private void prompt() {
        log("simfs:" + fs.pwd() + "$ ");
    }

    public VentanaPrincipal() {
        initComponents();
        initCustom();
        gantt.setPreferredSize(new Dimension(600, 200));
        listaPaginas = new ArrayList<>();
    }

    private void initCustom() {
        // --- Consola: limpiar y saludar ---
        jTextAreaConsola.setEnabled(true);
        jTextAreaConsola.setEditable(false);
        jTextAreaConsola.setText("");
        log("Bienvenido. Escribe 'help'.");
        prompt();

        // --- Árbol inicial (con flag anti-bucle) ---
        updatingTree = true;
        p3.TreeAdapter.refreshJTree(jTreeArbol, fs.getRoot(), fs.getCurrent());
        updatingTree = false;

        // --- Renderer para íconos (carpeta vs archivo) basado en FsNode ---
        jTreeArbol.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public java.awt.Component getTreeCellRendererComponent(
                    javax.swing.JTree tree, Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                java.awt.Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (value instanceof javax.swing.tree.DefaultMutableTreeNode) {
                    Object userObj = ((javax.swing.tree.DefaultMutableTreeNode) value).getUserObject();
                    if (userObj instanceof p3.TreeAdapter.FsNode) {
                        p3.TreeAdapter.FsNode node = (p3.TreeAdapter.FsNode) userObj;
                        if (node.isDirectory) {
                            setIcon(javax.swing.UIManager.getIcon("FileView.directoryIcon")); // ícono carpeta
                        } else {
                            setIcon(javax.swing.UIManager.getIcon("FileView.fileIcon")); // ícono archivo
                        }
                    }
                }
                return c;
            }
        });

        // --- ENTER en la entrada = ejecutar comando ---
        jTextFieldEntrada.addActionListener(e -> ejecutarComandoDesdeUI());

        // --- Selección del JTree = cambiar cwd (NO refrescar árbol aquí para evitar ciclo) ---
        jTreeArbol.addTreeSelectionListener(e -> {
            if (updatingTree) {
                return;
            }
            javax.swing.tree.TreePath tp = e.getPath();
            StringBuilder sb = new StringBuilder();
            Object[] segs = tp.getPath();
            for (Object o : segs) {
                String s;
                // Recupera el nombre desde FsNode para evitar problemas con texto
                if (o instanceof javax.swing.tree.DefaultMutableTreeNode) {
                    Object uo = ((javax.swing.tree.DefaultMutableTreeNode) o).getUserObject();
                    if (uo instanceof p3.TreeAdapter.FsNode) {
                        s = ((p3.TreeAdapter.FsNode) uo).name;
                    } else {
                        s = o.toString();
                    }
                } else {
                    s = o.toString();
                }
                if ("/".equals(s)) {
                    continue; // raíz textual del JTree
                }
                if (sb.length() == 0) {
                    sb.append("/").append(s);
                } else {
                    sb.append("/").append(s);
                }
            }
            String target = sb.length() == 0 ? "/" : sb.toString();
            String r = fs.cd(target);
            log(r);
            // Prompt sin refrescar árbol aquí para evitar bucle
            prompt();
            // Refresca panel de archivos (lista del directorio actual)
            try {
                refreshArchivosMemoriaPanel();
            } catch (Exception ignore) {
            }
        });

        // --- Simulador de planificación (tu código existente) ---
        modeloTabla = (DefaultTableModel) tablaProcesos.getModel();
        modeloTabla.setRowCount(0);
        if (modeloTabla.getColumnCount() < 3) {
            modeloTabla.setColumnCount(3);
        }
        modeloTabla.setColumnIdentifiers(new Object[]{"ID", "Llegada", "Ráfaga"});
        gantt.setPreferredSize(new Dimension(600, 200));
        panelGantt.setLayout(new BorderLayout());
        panelGantt.add(gantt, BorderLayout.CENTER);
        panelGantt.revalidate();
        panelGantt.repaint();

        // --- Panel de archivos (memoria): crear dinámicamente para evitar NPE ---
        // Si usas el diseñador y ya existe jTableArchivosMemoria, NO creamos otra; si es null, la creamos.
        if (jTableArchivosMemoria == null) {
            jPanelArchivosMemoria.setLayout(new java.awt.BorderLayout());

            jTableArchivosMemoria = new javax.swing.JTable(new javax.swing.table.DefaultTableModel(
                    new Object[][]{},
                    // Columnas pensadas para "vista organización" (puedes cambiarlas a FS si prefieres)
                    new String[]{"Archivo", "Clave", "Org Info", "Tamaño", "Preview"}
            ) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            });

            jTableArchivosMemoria.setRowHeight(22);
            jTableArchivosMemoria.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);

            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(jTableArchivosMemoria);
            jPanelArchivosMemoria.add(scroll, java.awt.BorderLayout.CENTER);

            // Ajuste dinámico de columnas al redimensionar el panel
            jPanelArchivosMemoria.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    try {
                        int width = jPanelArchivosMemoria.getWidth();
                        if (width > 0 && jTableArchivosMemoria.getColumnModel().getColumnCount() >= 5) {
                            int col0 = (int) (width * 0.20); // Archivo
                            int col1 = (int) (width * 0.15); // Clave
                            int col2 = (int) (width * 0.20); // Org Info
                            int col3 = (int) (width * 0.10); // Tamaño
                            int col4 = width - col0 - col1 - col2 - col3 - 60; // Preview
                            jTableArchivosMemoria.getColumnModel().getColumn(0).setPreferredWidth(col0);
                            jTableArchivosMemoria.getColumnModel().getColumn(1).setPreferredWidth(col1);
                            jTableArchivosMemoria.getColumnModel().getColumn(2).setPreferredWidth(col2);
                            jTableArchivosMemoria.getColumnModel().getColumn(3).setPreferredWidth(col3);
                            jTableArchivosMemoria.getColumnModel().getColumn(4).setPreferredWidth(col4);
                        }
                    } catch (Exception ignore) {
                    }
                }
            });
        } else {
            // Si la tabla ya existe (viene del diseñador), asegúrate de que el panel tenga BorderLayout y la tabla esté dentro de un JScrollPane
            if (!(jPanelArchivosMemoria.getLayout() instanceof java.awt.BorderLayout)) {
                jPanelArchivosMemoria.setLayout(new java.awt.BorderLayout());
            }
            // Si la tabla no tiene scroll, lo agregamos
            boolean tieneScroll = false;
            for (java.awt.Component comp : jPanelArchivosMemoria.getComponents()) {
                if (comp instanceof javax.swing.JScrollPane) {
                    tieneScroll = true;
                    break;
                }
            }
            if (!tieneScroll) {
                javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(jTableArchivosMemoria);
                jPanelArchivosMemoria.add(scroll, java.awt.BorderLayout.CENTER);
            }
        }

        // --- Primer llenado del panel de archivos ---
        try {
            refreshArchivosMemoriaPanel();
        } catch (Exception ignore) {
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        btnAgregar = new javax.swing.JButton();
        tiempoLlegada = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        cpuBurst = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        quantum = new javax.swing.JTextField();
        tipoAlgoritmo = new javax.swing.JComboBox<>();
        simularRun = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        jScrollPane = new javax.swing.JScrollPane();
        tablaProcesos = new javax.swing.JTable();
        panelGantt = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jRepetir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        textAreaResultados = new javax.swing.JTextArea();
        jLabel8 = new javax.swing.JLabel();
        quantumActual = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jComboBoxAlgoritmos = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        jButtonSubirPaguina = new javax.swing.JButton();
        jButtonRUNRemplazo = new javax.swing.JButton();
        jComboBoxPaguina = new javax.swing.JComboBox<>();
        jLabel13 = new javax.swing.JLabel();
        jButtonLimpiarTodo = new javax.swing.JButton();
        jButtonInfoAlgorimo = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTablePaguinas = new javax.swing.JTable();
        Paguinas = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jPanelResultadosRemplazo = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPanelSimulacionRemplazo = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaConsola = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTreeArbol = new javax.swing.JTree();
        jTextFieldEntrada = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jPanelArchivosMemoria = new javax.swing.JPanel();
        crearAleatorio = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel3.setBackground(new java.awt.Color(0, 0, 0));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setText("Algoritmo");

        btnAgregar.setText("Agregar Proceso");
        btnAgregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarActionPerformed(evt);
            }
        });

        jLabel5.setText("Tiempo llegada");

        jLabel6.setText("Rafaga (burst time)");

        jLabel1.setText("Quantum");

        tipoAlgoritmo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "RR", "FCFS", "SJF", "SRTF" }));
        tipoAlgoritmo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tipoAlgoritmoActionPerformed(evt);
            }
        });

        simularRun.setBackground(new java.awt.Color(0, 0, 0));
        simularRun.setForeground(new java.awt.Color(255, 255, 255));
        simularRun.setText("RUN");
        simularRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                simularRunActionPerformed(evt);
            }
        });

        btnEliminar.setBackground(new java.awt.Color(204, 0, 51));
        btnEliminar.setForeground(new java.awt.Color(255, 255, 255));
        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnEliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(tiempoLlegada)
                                .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cpuBurst)
                                .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(quantum, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(btnAgregar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                                .addComponent(tipoAlgoritmo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(simularRun, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addContainerGap(28, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tipoAlgoritmo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addGap(7, 7, 7)
                .addComponent(tiempoLlegada, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel6)
                .addGap(7, 7, 7)
                .addComponent(cpuBurst, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(quantum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnAgregar)
                .addGap(18, 18, 18)
                .addComponent(btnEliminar)
                .addGap(18, 18, 18)
                .addComponent(simularRun)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tablaProcesos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "ID", "Llegada", "Ráfaga"
            }
        ));
        jScrollPane.setViewportView(tablaProcesos);

        javax.swing.GroupLayout panelGanttLayout = new javax.swing.GroupLayout(panelGantt);
        panelGantt.setLayout(panelGanttLayout);
        panelGanttLayout.setHorizontalGroup(
            panelGanttLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        panelGanttLayout.setVerticalGroup(
            panelGanttLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 213, Short.MAX_VALUE)
        );

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Tabla de procesos");

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Resultados");

        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Simulacion");

        jRepetir.setText("Repetir animacion");
        jRepetir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRepetirActionPerformed(evt);
            }
        });

        textAreaResultados.setEditable(false);
        textAreaResultados.setBackground(new java.awt.Color(220, 222, 227));
        textAreaResultados.setColumns(20);
        textAreaResultados.setRows(5);
        jScrollPane1.setViewportView(textAreaResultados);

        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Quantum:");

        quantumActual.setForeground(new java.awt.Color(255, 255, 255));
        quantumActual.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(quantumActual, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 635, Short.MAX_VALUE)
                                .addComponent(panelGantt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 112, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jRepetir, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 635, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addGap(3, 3, 3)
                .addComponent(jScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(quantumActual))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelGantt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jRepetir)
                .addGap(1, 1, 1)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE)
                .addGap(17, 17, 17))
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane4.addTab("Algoritmos de planificacion", jPanel3);

        jPanel4.setBackground(new java.awt.Color(0, 0, 0));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jComboBoxAlgoritmos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "FIFO", "LRU", "Optimal Page Replacement", "Clock", "Second Chance", "VMIN(Variable MIN)", "Working Set", "PFF" }));
        jComboBoxAlgoritmos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxAlgoritmosActionPerformed(evt);
            }
        });

        jLabel9.setForeground(new java.awt.Color(0, 0, 0));
        jLabel9.setText("Algoritmo");

        jButtonSubirPaguina.setBackground(new java.awt.Color(255, 0, 102));
        jButtonSubirPaguina.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButtonSubirPaguina.setForeground(new java.awt.Color(255, 255, 255));
        jButtonSubirPaguina.setText("Agregar Paguina");
        jButtonSubirPaguina.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSubirPaguinaActionPerformed(evt);
            }
        });

        jButtonRUNRemplazo.setBackground(new java.awt.Color(0, 0, 0));
        jButtonRUNRemplazo.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButtonRUNRemplazo.setForeground(new java.awt.Color(255, 255, 255));
        jButtonRUNRemplazo.setText("RUN");
        jButtonRUNRemplazo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRUNRemplazoActionPerformed(evt);
            }
        });

        jComboBoxPaguina.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jComboBoxPaguina.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "A", "B", "C", "D", "E" }));
        jComboBoxPaguina.setToolTipText("");

        jLabel13.setForeground(new java.awt.Color(0, 0, 0));
        jLabel13.setText("Paguinas");

        jButtonLimpiarTodo.setText("🗑️ Limpiar Todo ");
        jButtonLimpiarTodo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLimpiarTodoActionPerformed(evt);
            }
        });

        jButtonInfoAlgorimo.setBackground(new java.awt.Color(0, 102, 255));
        jButtonInfoAlgorimo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButtonInfoAlgorimo.setForeground(new java.awt.Color(255, 255, 255));
        jButtonInfoAlgorimo.setText("i");
        jButtonInfoAlgorimo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonInfoAlgorimoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jComboBoxAlgoritmos, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonSubirPaguina, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonRUNRemplazo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jComboBoxPaguina, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButtonInfoAlgorimo)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonLimpiarTodo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxAlgoritmos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jComboBoxPaguina, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonSubirPaguina)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonRUNRemplazo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonLimpiarTodo)
                    .addComponent(jButtonInfoAlgorimo))
                .addGap(18, 18, 18))
        );

        jTablePaguinas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jTablePaguinas.setEnabled(false);
        jScrollPane2.setViewportView(jTablePaguinas);

        Paguinas.setForeground(new java.awt.Color(255, 255, 255));
        Paguinas.setText("Secuencia de Paguinas");

        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Simulacion");

        javax.swing.GroupLayout jPanelResultadosRemplazoLayout = new javax.swing.GroupLayout(jPanelResultadosRemplazo);
        jPanelResultadosRemplazo.setLayout(jPanelResultadosRemplazoLayout);
        jPanelResultadosRemplazoLayout.setHorizontalGroup(
            jPanelResultadosRemplazoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelResultadosRemplazoLayout.setVerticalGroup(
            jPanelResultadosRemplazoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 115, Short.MAX_VALUE)
        );

        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Resultados");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Paguinas, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPanelSimulacionRemplazo)
                    .addComponent(jPanelResultadosRemplazo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 654, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Paguinas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPanelSimulacionRemplazo, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanelResultadosRemplazo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );

        jTabbedPane4.addTab("Algoritmos de reemplazo de pagina", jPanel4);

        jPanel5.setBackground(new java.awt.Color(0, 0, 0));
        jPanel5.setForeground(new java.awt.Color(51, 51, 51));

        jTextAreaConsola.setBackground(new java.awt.Color(0, 0, 0));
        jTextAreaConsola.setColumns(20);
        jTextAreaConsola.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jTextAreaConsola.setForeground(new java.awt.Color(153, 255, 102));
        jTextAreaConsola.setRows(5);
        jTextAreaConsola.setText("Consola principal");
        jTextAreaConsola.setEnabled(false);
        jScrollPane3.setViewportView(jTextAreaConsola);

        jTreeArbol.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jScrollPane4.setViewportView(jTreeArbol);

        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Ingresa el comando y presiona ENTER .....");

        javax.swing.GroupLayout jPanelArchivosMemoriaLayout = new javax.swing.GroupLayout(jPanelArchivosMemoria);
        jPanelArchivosMemoria.setLayout(jPanelArchivosMemoriaLayout);
        jPanelArchivosMemoriaLayout.setHorizontalGroup(
            jPanelArchivosMemoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 258, Short.MAX_VALUE)
        );
        jPanelArchivosMemoriaLayout.setVerticalGroup(
            jPanelArchivosMemoriaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 409, Short.MAX_VALUE)
        );

        crearAleatorio.setBackground(new java.awt.Color(51, 153, 0));
        crearAleatorio.setForeground(new java.awt.Color(255, 255, 255));
        crearAleatorio.setText("crear aleatorio");
        crearAleatorio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                crearAleatorioActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(crearAleatorio)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jTextFieldEntrada)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanelArchivosMemoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(65, 65, 65)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane3)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE))
                    .addComponent(jPanelArchivosMemoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jTextFieldEntrada, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(crearAleatorio)
                .addContainerGap(39, Short.MAX_VALUE))
        );

        jTabbedPane4.addTab("sistema de archivo", jPanel5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 856, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(8, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 659, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarActionPerformed(java.awt.event.ActionEvent evt) {
//GEN-FIRST:event_btnAgregarActionPerformed
        // TODO add your handling code here:
        try {
            String id = "P" + (modeloTabla.getRowCount() + 1);
            int llegada = Integer.parseInt(tiempoLlegada.getText().trim());
            int rafaga = Integer.parseInt(cpuBurst.getText().trim());
            if (llegada < 0 || rafaga <= 0) {
                javax.swing.JOptionPane.showMessageDialog(this, "Llegada ≥ 0 y Ráfaga > 0");
                return;
            }
            modeloTabla.addRow(new Object[]{id, llegada, rafaga});
        } catch (NumberFormatException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Ingresa números válidos en Llegada y Ráfaga.");
        }
    }//GEN-LAST:event_btnAgregarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        // TODO add your handling code here:
        int totalFilas = modeloTabla.getRowCount();
        if (totalFilas > 0) {
            modeloTabla.removeRow(totalFilas - 1); // Elimina la última fila
            // Reetiquetar IDs
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                modeloTabla.setValueAt("P" + (i + 1), i, 0);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No hay procesos para eliminar.");
        }
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void simularRunActionPerformed(java.awt.event.ActionEvent evt) {
//GEN-FIRST:event_simularRunActionPerformed
        // TODO add your handling code here:
        var procesos = leerProcesosDeTabla();
        if (procesos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Agrega al menos un proceso.");
            return;
        }

        String algoritmo = String.valueOf(tipoAlgoritmo.getSelectedItem());
        Planificador planificador;

        if ("RR".equals(algoritmo)) {
            try {
                int q = Integer.parseInt(quantum.getText().trim());
                if (q <= 0) {
                    throw new NumberFormatException();
                }
                planificador = new p1.schedule.RR(q);
                quantumActual.setText(String.valueOf(q));
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(this, "Quantum debe ser un número entero positivo.");
                return;
            }
        } else {
            planificador = crearPlanificador(); // FCFS, SJF, SRTF, etc.
        }

        ultimoResultado = planificador.simular(procesos);
        SimulationResult result = ultimoResultado;
        gantt.animarSegmentos(result.getTimeline());

        // Mostrar métricas en orden de ejecución
        StringBuilder sb = new StringBuilder("=== Métricas ===\n");
        double e = 0, r = 0, tt = 0;
        int n = 0;
        Set<String> mostrados = new HashSet<>();

        for (var seg : result.getTimeline()) {
            String pid = seg.getProcesoId();
            if ("IDLE".equals(pid) || mostrados.contains(pid)) {
                continue;
            }

            var m = result.getMetricsPorProceso().get(pid);
            sb.append(String.format("%s -> espera=%d, respuesta=%d, turnaround=%d%n",
                    pid, m.espera, m.respuesta, m.turnaround));
            e += m.espera;
            r += m.respuesta;
            tt += m.turnaround;
            n++;
            mostrados.add(pid);
        }

        if (n > 0) {
            sb.append(String.format("%nPromedios: espera=%.2f, respuesta=%.2f, turnaround=%.2f", e / n, r / n, tt / n));
        }

        textAreaResultados.setText(sb.toString());
        textAreaResultados.repaint();

    }//GEN-LAST:event_simularRunActionPerformed

    private void tipoAlgoritmoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tipoAlgoritmoActionPerformed
        // TODO add your handling code here:
        String seleccionado = String.valueOf(tipoAlgoritmo.getSelectedItem());

        if ("RR".equals(seleccionado)) {
            quantum.setEnabled(true);
        } else {
            quantum.setEnabled(false);
        }
    }//GEN-LAST:event_tipoAlgoritmoActionPerformed

    private void jRepetirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRepetirActionPerformed
        // TODO add your handling code here:

        if (ultimoResultado != null) {
            gantt.animarSegmentos(ultimoResultado.getTimeline());
        } else {
            JOptionPane.showMessageDialog(this, "Primero ejecuta una simulación.");
        }
    }//GEN-LAST:event_jRepetirActionPerformed

    private void jComboBoxAlgoritmosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxAlgoritmosActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxAlgoritmosActionPerformed

    private void jButtonSubirPaguinaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSubirPaguinaActionPerformed
        String page = (String) jComboBoxPaguina.getSelectedItem();
        if (page == null || page.isBlank()) {
            return;
        }

        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTablePaguinas.getModel();
        model.addRow(new Object[]{page});

        if (listaPaginas == null) {
            listaPaginas = new java.util.ArrayList<>();
        }
        listaPaginas.add(page.substring(0, 1).toUpperCase());
    }//GEN-LAST:event_jButtonSubirPaguinaActionPerformed

    private void jButtonRUNRemplazoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRUNRemplazoActionPerformed
        // TODO add your handling code here:       

        String algoritmo = String.valueOf(jComboBoxAlgoritmos.getSelectedItem());

        // Construye secuencia desde la tabla
        java.util.List<String> referencias = new java.util.ArrayList<>();
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTablePaguinas.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object val = model.getValueAt(i, 0);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) {
                    referencias.add(s.substring(0, 1).toUpperCase());
                }
            }
        }
        if (referencias.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Agrega al menos una Paguina (usa 'Agregar Paguina').",
                    "Atención", javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Pide marcos
        Integer marcos = pedirNumeroDeMarcos(this);
        if (marcos == null) {
            return;
        }

        try {
            FIFOPageReplacement.Result panelResult;
            String algoritmoUsado;

            if ("Clock".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "Clock";
                ClockPageReplacement clk = new ClockPageReplacement();
                ClockPageReplacement.Result clockFull = clk.simulate(referencias, marcos);
                panelResult = ClockPageReplacement.toFifoResult(clockFull);

                SimulationPanel panel = new SimulationPanel(panelResult, clockFull); // ✅ pasa objeto completo
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("Second Chance".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "Second Chance";
                SecondChancePageReplacement sc = new SecondChancePageReplacement();
                SecondChancePageReplacement.Result scFull = sc.simulate(referencias, marcos);
                panelResult = SecondChancePageReplacement.toFifoResult(scFull);

                SimulationPanel panel = new SimulationPanel(panelResult, scFull);   // ✅ pasa objeto completo
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("FIFO".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "FIFO";
                FIFOPageReplacement fifo = new FIFOPageReplacement();
                panelResult = fifo.simulate(referencias, marcos);
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("LRU".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "LRU";
                LRUPageReplacement lru = new LRUPageReplacement();
                panelResult = LRUPageReplacement.toFifoResult(lru.simulate(referencias, marcos));
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("Optimal Page Replacement".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "Óptimo";
                OptimoPageReplacement opt = new OptimoPageReplacement();
                panelResult = OptimoPageReplacement.toFifoResult(opt.simulate(referencias, marcos));
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("Working Set".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "Working Set";
                int windowSize = pedirWindowSize(this);
                WorkingSetPageReplacement ws = new WorkingSetPageReplacement();
                panelResult = WorkingSetPageReplacement.toFifoResult(ws.simulate(referencias, marcos, windowSize));
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("VMIN(Variable MIN)".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "VMIN";
                int horizon = pedirHorizon(this);
                VMINPageReplacement vmin = new VMINPageReplacement();
                panelResult = VMINPageReplacement.toFifoResult(vmin.simulate(referencias, marcos, horizon));
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else if ("PFF".equalsIgnoreCase(algoritmo)) {
                algoritmoUsado = "PFF";
                int upper = pedirUmbral("Umbral superior (p.ej. 2):");
                int lower = pedirUmbral("Umbral inferior (p.ej. 5):");
                PFFPageReplacement pff = new PFFPageReplacement();
                panelResult = PFFPageReplacement.toFifoResult(pff.simulate(referencias, marcos, upper, lower));
                SimulationPanel panel = new SimulationPanel(panelResult);
                mostrarSimulacion(panel, panelResult, algoritmoUsado, referencias, marcos);

            } else {
                javax.swing.JOptionPane.showMessageDialog(
                        this, "Algoritmo no implementado aún: " + algoritmo,
                        "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    this, "Error en la simulación: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void mostrarSimulacion(SimulationPanel panel,
            FIFOPageReplacement.Result panelResult,
            String algoritmoUsado,
            java.util.List<String> referencias,
            int marcos) {
        jScrollPanelSimulacionRemplazo.setViewportView(panel);
        jScrollPanelSimulacionRemplazo.revalidate();
        jScrollPanelSimulacionRemplazo.repaint();

        String finalFramesString = FIFOPageReplacement.Result.formatFrames(panelResult.finalFrames());
        String seq = String.join(", ", referencias);
        String resumen = String.format(
                "Algoritmo: %s%nSecuencia: %s%nMarcos: %d%n%nResultado final: %s%nFallas: %d%nHits: %d%nTasa de fallas: %.2f%%",
                algoritmoUsado, seq, marcos, finalFramesString,
                panelResult.totalFaults, panelResult.totalHits,
                (panelResult.totalFaults * 100.0) / Math.max(1, (panelResult.totalFaults + panelResult.totalHits))
        );
        resultadosRemplazoArea.setText(resumen);


    }//GEN-LAST:event_jButtonRUNRemplazoActionPerformed

    private void jButtonLimpiarTodoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLimpiarTodoActionPerformed
        // TODO add your handling code here:
        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "¿Seguro que quieres borrar la secuencia, simulación y resultados?",
                "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION);
        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
            // Borra la tabla
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTablePaguinas.getModel();
            model.setRowCount(0);
            if (listaPaginas != null) {
                listaPaginas.clear();
            }

            // Borra simulación
            jScrollPanelSimulacionRemplazo.setViewportView(null);

            // Borra resultados
            resultadosRemplazoArea.setText("");
        }
    }//GEN-LAST:event_jButtonLimpiarTodoActionPerformed

    private void jButtonInfoAlgorimoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonInfoAlgorimoActionPerformed
        // TODO add your handling code here:

        // Obtiene el algoritmo seleccionado en el combo
        String algoritmo = String.valueOf(jComboBoxAlgoritmos.getSelectedItem());

        // Texto explicativo según el algoritmo
        String mensaje;
        switch (algoritmo) {
            case "FIFO":
                mensaje = """
            FIFO (First-In, First-Out)
            --------------------------------
            • Estrategia simple: la primera página que entra es la primera en salir.
            • Si hay hueco, coloca la página.
            • Si no hay hueco, reemplaza la página más antigua.
            """;
                break;

            case "LRU":
                mensaje = """
            LRU (Least Recently Used)
            --------------------------------
            • Reemplaza la página que no se ha usado por más tiempo.
            • Mantiene registro del último uso de cada página.
            • Si hay hueco, coloca la página; si no, expulsa la menos recientemente usada.
            """;
                break;

            case "Optimal Page Replacement":
                mensaje = """
            Óptimo
            --------------------------------
            • Reemplaza la página que se usará más tarde en el futuro (o nunca).
            • Es teórico, requiere conocer las referencias futuras.
            """;
                break;

            case "Clock":
                mensaje = """
            Clock (Segunda oportunidad con puntero)
            --------------------------------
            • Cada marco tiene un bit de uso (u).
            • Si hay hueco, coloca la página y u=1.
            • Si no hay hueco:
              - El puntero recorre marcos:
                * Si u=0 → reemplaza ahí.
                * Si u=1 → pone u=0 y avanza.
            • Después de reemplazar, el puntero avanza.
            """;
                break;

            case "Second Chance":
                mensaje = """
            Second Chance (FIFO con bit de referencia)
            --------------------------------
            • Usa una cola FIFO y un bit de referencia (u).
            • Si hay hueco, coloca la página y u=1.
            • Si no hay hueco:
              - Toma el más antiguo:
                * Si u=0 → reemplaza.
                * Si u=1 → pone u=0 y lo reencola al final.
            """;
                break;

            case "VMIN(Variable MIN)":
                mensaje = """
            VMIN (Variable MIN)
            --------------------------------
            • Usa una ventana futura (horizon) para decidir la víctima.
            • Si una página no aparece en la ventana → candidata inmediata.
            • Si todas aparecen → expulsa la que aparece más tarde.
            """;
                break;

            case "Working Set":
                mensaje = """
            Working Set
            --------------------------------
            • Mantiene las páginas referenciadas en la ventana más reciente.
            • Elimina páginas fuera del conjunto antes de colocar la nueva.
            """;
                break;

            case "PFF":
                mensaje = """
            PFF (Page Fault Frequency)
            --------------------------------
            • Ajusta dinámicamente el número de marcos según la frecuencia de fallas.
            • Alta frecuencia → aumenta marcos.
            • Baja frecuencia → reduce marcos.
            """;
                break;

            default:
                mensaje = "Selecciona un algoritmo para ver su explicación.";
                break;
        }

        // Mostrar ventana emergente grande
        JTextArea textArea = new JTextArea(mensaje);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Información del algoritmo: " + algoritmo,
                JOptionPane.INFORMATION_MESSAGE
        );

    }//GEN-LAST:event_jButtonInfoAlgorimoActionPerformed

    private void crearAleatorioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_crearAleatorioActionPerformed
        // TODO add your handling code here:
        // Generador de datos aleatorios
        java.util.Random rnd = new java.util.Random();

        // Número de alumno (clave)
        String noAlumno = String.format("%04d", rnd.nextInt(9999));

        // Nombre y apellidos aleatorios
        String[] nombres = {"Juan", "Ana", "Luis", "María", "Pedro", "Sofía", "Carlos", "Lucía"};
        String[] apellidos = {"Pérez", "García", "López", "Martínez", "Hernández", "Ramírez"};
        String nombre = nombres[rnd.nextInt(nombres.length)];
        String apPat = apellidos[rnd.nextInt(apellidos.length)];
        String apMat = apellidos[rnd.nextInt(apellidos.length)];

        // Teléfono aleatorio
        String telefono = "55" + (10000000 + rnd.nextInt(89999999));

        // Calle y código postal
        String calle = "Calle " + (rnd.nextInt(200) + 1);
        String codigoPostal = String.format("%05d", rnd.nextInt(99999));

        // Crear registro
        p3.StudentRecord r = new p3.StudentRecord(noAlumno, nombre, apPat, apMat, telefono, calle, codigoPostal);

        // Insertar en organización actual
        fs.executeCommand(String.format(
                "insertalumno %s \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"",
                noAlumno, nombre, apPat, apMat, telefono, calle, codigoPostal
        ));

        // Crear archivo en el directorio actual (no en /alumnos)
        String fileName = "alumno_" + noAlumno + ".txt";
        String contenido = String.join("\n",
                "no=" + noAlumno,
                "nombre=" + nombre,
                "apellidoPaterno=" + apPat,
                "apellidoMaterno=" + apMat,
                "telefono=" + telefono,
                "calle=" + calle,
                "codigoPostal=" + codigoPostal
        );
        fs.echoToFile(contenido, fileName, false);

        // Refrescar árbol y panel
        updatingTree = true;
        p3.TreeAdapter.refreshJTree(jTreeArbol, fs.getRoot(), fs.getCurrent());
        updatingTree = false;
        refreshArchivosMemoriaPanel();

        // Mostrar mensaje en consola
        log("Alumno aleatorio creado: " + noAlumno + " (" + nombre + " " + apPat + ")");
        prompt();

    }//GEN-LAST:event_crearAleatorioActionPerformed

//1)----Algotimos de Planificaion----
    private List<Proceso> leerProcesosDeTabla() {
        List<Proceso> lista = new ArrayList<>();
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String id = String.valueOf(modeloTabla.getValueAt(i, 0));
            int llegada = Integer.parseInt(String.valueOf(modeloTabla.getValueAt(i, 1)));
            int rafaga = Integer.parseInt(String.valueOf(modeloTabla.getValueAt(i, 2)));
            lista.add(new Proceso(id, llegada, rafaga));
        }
        return lista;
    }

    private Planificador crearPlanificador() {
        String alg = String.valueOf(tipoAlgoritmo.getSelectedItem());
        switch (alg) {
            case "FCFS":
                return new FCFS();
            case "SJF":
                return new SJF();
            case "SRTF":
                return new SRTF();
            default:
                return new FCFS();
        }
    }
//2)----Algotimos de remplazo----

    private static Integer pedirNumeroDeMarcos(java.awt.Component parent) {
        while (true) {
            String input = javax.swing.JOptionPane.showInputDialog(parent,
                    "Número de marcos (p. ej. 3):",
                    "Configurar marcos", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return null; // cancelado
            }
            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }
            try {
                int n = Integer.parseInt(input);
                if (n > 0 && n <= 20) {
                    return n;
                } else {
                    javax.swing.JOptionPane.showMessageDialog(parent,
                            "Ingresa un entero entre 1 y 20.",
                            "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(parent,
                        "Ingresa un entero válido.",
                        "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static int pedirHorizon(java.awt.Component parent) {
        while (true) {
            String input = javax.swing.JOptionPane.showInputDialog(parent,
                    "Tamaño de ventana (horizon) para VMIN (p.ej. 5):",
                    "Configurar VMIN", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return 5; // valor por defecto si cancelan
            }
            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }
            try {
                int n = Integer.parseInt(input);
                if (n > 0 && n <= 50) {
                    return n;
                } else {
                    javax.swing.JOptionPane.showMessageDialog(parent,
                            "Ingresa un entero entre 1 y 50.",
                            "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(parent,
                        "Ingresa un entero válido.",
                        "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static int pedirWindowSize(java.awt.Component parent) {
        while (true) {
            String input = javax.swing.JOptionPane.showInputDialog(parent,
                    "Tamaño de ventana (Working Set) (p.ej. 4):",
                    "Configurar Working Set", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return 4; // valor por defecto
            }
            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }
            try {
                int n = Integer.parseInt(input);
                if (n > 0 && n <= 50) {
                    return n;
                } else {
                    javax.swing.JOptionPane.showMessageDialog(parent,
                            "Ingresa un entero entre 1 y 50.",
                            "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(parent,
                        "Ingresa un entero válido.",
                        "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static int pedirUmbral(String mensaje) {
        while (true) {
            String input = javax.swing.JOptionPane.showInputDialog(null, mensaje, "Configurar PFF", javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return 3; // valor por defecto
            }
            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }
            try {
                int n = Integer.parseInt(input);
                if (n > 0 && n <= 50) {
                    return n;
                } else {
                    javax.swing.JOptionPane.showMessageDialog(null, "Ingresa un entero entre 1 y 50.", "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException e) {
                javax.swing.JOptionPane.showMessageDialog(null, "Ingresa un entero válido.", "Valor inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    //3)----simulador de archivos---- 
    private void ejecutarComandoDesdeUI() {
        // 1) Leer y validar entrada
        String cmd = jTextFieldEntrada.getText().trim();
        if (cmd.isEmpty()) {
            return;
        }

        // 2) Comando especial: salir
        if ("exit".equalsIgnoreCase(cmd)) {
            System.exit(0);
        }

        // 3) Eco del comando en consola
        log("> " + cmd);

        try {
            // 4) Ejecutar en el motor (FileSystem)
            p3.FileSystem.CommandResult res = fs.executeCommand(cmd);

            // 5) Si el comando pidió limpiar la consola (clear)
            if (res.clearConsole) {
                jTextAreaConsola.setText("");
            }

            // 6) Mostrar la salida del comando (si trae texto)
            if (res.output != null && !res.output.isEmpty()) {
                log(res.output);
            }

            // 7) Manejo de reset vs refresco normal
            if (res.requestReset) {
                // Reinstanciar el motor
                fs = new p3.FileSystem();

                // Refrescar el árbol con flag anti-bucle
                updatingTree = true;
                p3.TreeAdapter.refreshJTree(jTreeArbol, fs.getRoot(), fs.getCurrent());
                updatingTree = false;

                // Prompt del nuevo estado
                prompt();

                // Refrescar panel de archivos
                try {
                    refreshArchivosMemoriaPanel();
                } catch (Exception ignore) {
                }
            } else {
                // Si hay que refrescar el árbol (mkdir/rm/cd/insertalumno que cambia FS)
                if (res.refreshTree) {
                    updatingTree = true;
                    p3.TreeAdapter.refreshJTree(jTreeArbol, fs.getRoot(), fs.getCurrent());
                    updatingTree = false;
                }

                // Prompt normal
                prompt();

                // Refrescar panel de archivos SIEMPRE (tras cualquier comando)
                try {
                    refreshArchivosMemoriaPanel();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ex) {
            // Captura cualquier falla inesperada para no romper la UI
            ex.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Error al ejecutar el comando:\n" + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        } finally {
            // 10) Limpiar la caja de entrada
            jTextFieldEntrada.setText("");
        }
    }


// Modo de vista (si quieres alternar FS/Organización; aquí asumimos organización)

private void refreshArchivosMemoriaPanel() {
    if (jTableArchivosMemoria == null) return;

    javax.swing.table.DefaultTableModel model =
        (javax.swing.table.DefaultTableModel) jTableArchivosMemoria.getModel();

    // NO cambies columnas aquí; ya están en initCustom()
    model.setRowCount(0);

    // Directorio actual y archivos
    p3.Directory dirActual = fs.getCurrent();
    java.util.List<p3.FileItem> archivos = new java.util.ArrayList<>(dirActual.getFiles().values());
    archivos.sort(java.util.Comparator.comparing(p3.FileItem::getName));

    boolean pobladoPorOrganizacion = false;

    if (modoArchivos == ModoArchivos.ORGANIZACION) {
        p3.FileOrganization org = fs.getCurrentOrg();
        if (org != null) {
            String showText;
            try { showText = org.show(); } catch (Exception ex) { showText = null; }

            java.util.List<String> ordenClaves = extraerClavesDeShow(showText);
            if (!ordenClaves.isEmpty()) {
                for (String clave : ordenClaves) {
                    p3.FileItem archivo = buscarArchivoPorClave(archivos, clave);
                    String nombreArchivo = (archivo != null) ? archivo.getName() : "(sin archivo)";
                    String contenido = (archivo != null && archivo.getContent() != null) ? archivo.getContent() : "";
                    String preview = contenido.replace("\n", " ");
                    if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
                    int size = contenido.length();

                    String orgInfo = etiquetaOrganizacion(org, clave, showText);
                    model.addRow(new Object[]{nombreArchivo, clave, orgInfo, size, preview});
                }
                pobladoPorOrganizacion = true;
            }
        }
    }

    // Fallback — si la organización no pudo poblar filas, muestra archivos del directorio actual
    if (!pobladoPorOrganizacion) {
        for (p3.FileItem f : archivos) {
            String contenido = f.getContent();
            String preview = (contenido == null) ? "" : contenido.replace("\n", " ");
            if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
            int size = (contenido == null) ? 0 : contenido.length();
            String clave = extraerClaveDesdeContenido(contenido);
            String orgInfo = (modoArchivos == ModoArchivos.ORGANIZACION) ? "(sin orden org)" : "";
            model.addRow(new Object[]{f.getName(), clave, orgInfo, size, preview});
        }
    }

    // Aplicar RowSorter para ver el orden en la UI según organización (cosmético)
    applyRowSorterPorOrganizacion();
}

private void applyRowSorterPorOrganizacion() {
    if (jTableArchivosMemoria == null) return;
    javax.swing.table.DefaultTableModel tm =
        (javax.swing.table.DefaultTableModel) jTableArchivosMemoria.getModel();

    javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel> sorter =
        new javax.swing.table.TableRowSorter<>(tm);

    jTableArchivosMemoria.setRowSorter(sorter);

    p3.FileOrganization org = fs.getCurrentOrg();
    if (org == null) {
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
        return;
    }
    String type = org.getClass().getSimpleName();

    if (type.contains("Hash")) {
        // Orden por bucket (columna 2 "Org Info": "Hash bkt=<n>")
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(2, javax.swing.SortOrder.ASCENDING)));
    } else if (type.contains("SecuencialIndexado")) {
        // Si orgInfo es "SecIndex pos=<n>", puedes extraer el número con un comparator custom;
        // de forma simple, ordena por Clave (col 1).
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.ASCENDING)));
    } else if (type.contains("Secuencial")) {
        // Orden por clave (col 1)
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(1, javax.swing.SortOrder.ASCENDING)));
    } else if (type.contains("Pile")) {
        // Pile: ya poblamos en orden de inserción, aquí no ordenamos (o por nombre de archivo)
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
    } else {
        // Indexado/otros: por nombre de archivo
        sorter.setSortKeys(java.util.List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
    }
}

// ===== Helpers de mapeo/parseo =====
private p3.FileItem buscarArchivoPorClave(java.util.List<p3.FileItem> archivos, String clave) {
    if (clave == null) return null;
    for (p3.FileItem f : archivos) {
        String c = f.getContent();
        if (c != null && c.contains("no=" + clave)) return f;
    }
    return null;
}

private String extraerClaveDesdeContenido(String contenido) {
    if (contenido == null) return null;
    int p = contenido.indexOf("no=");
    if (p >= 0) {
        int end = contenido.indexOf("\n", p);
        String line = (end > p) ? contenido.substring(p, end) : contenido.substring(p);
        int eq = line.indexOf('=');
        if (eq >= 0 && eq + 1 < line.length()) {
            return line.substring(eq + 1).trim();
        }
    }
    return null;
}

/** Extrae claves en el orden que muestra org.show() */
private java.util.List<String> extraerClavesDeShow(String showText) {
    java.util.List<String> out = new java.util.ArrayList<>();
    if (showText == null) return out;
    String[] lines = showText.split("\n");
    for (String ln : lines) {
        int p = ln.indexOf("no=");
        if (p >= 0) {
            int end = ln.indexOf(",", p);
            String segment = (end > p) ? ln.substring(p, end) : ln.substring(p);
            int eq = segment.indexOf('=');
            if (eq >= 0 && eq + 1 < segment.length()) {
                String clave = segment.substring(eq + 1).trim();
                if (!clave.isEmpty()) out.add(clave);
            }
        }
    }
    return out;
}

private String etiquetaOrganizacion(p3.FileOrganization org, String clave, String showText) {
    String type = (org == null) ? "" : org.getClass().getSimpleName();
    if (type.contains("Pile")) return "Pile (inserción)";
    if (type.contains("SecuencialIndexado")) {
        Integer pos = extraerPosicionDeIndice(showText, clave);
        return (pos != null) ? ("SecIndex pos=" + pos) : "SecIndex";
    }
    if (type.contains("Secuencial")) return "Secuencial (orden clave)";
    if (type.contains("Indexado")) return "Indexado (map)";
    if (type.contains("Hash")) {
        int M = 97;
        int bucket = (clave != null) ? Math.floorMod(clave.hashCode(), M) : -1;
        return (bucket >= 0) ? ("Hash bkt=" + bucket) : "Hash";
    }
    return "Org";
}

private Integer extraerPosicionDeIndice(String showText, String clave) {
    if (showText == null || clave == null) return null;
    int iStart = showText.indexOf("Índice");
    if (iStart < 0) return null;
    int braceStart = showText.indexOf("{", iStart);
    int braceEnd = showText.indexOf("}", iStart);
    if (braceStart >= 0 && braceEnd > braceStart) {
        String inside = showText.substring(braceStart + 1, braceEnd);
        String[] entries = inside.split(",");
        for (String e : entries) {
            String[] kv = e.trim().split("=");
            if (kv.length == 2) {
                String key = kv[0].trim();
                if (clave.equals(key)) {
                    try { return Integer.parseInt(kv[1].trim()); }
                    catch (NumberFormatException ignore) {}
                }
            }
        }
    }
    return null;
}



    private int indexOf(java.util.List<String> list, String key) {
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Integer> extraerPosicionesDeSecIndexShow(String showText) {
        // El show() de SecuencialIndexado muestra "Registros:\n ..." y "Índice (key->pos): { ... }"
        Map<String, Integer> pos = new java.util.HashMap<>();
        if (showText == null) {
            return pos;
        }
        int iStart = showText.indexOf("Índice");
        if (iStart < 0) {
            return pos;
        }
        int braceStart = showText.indexOf("{", iStart);
        int braceEnd = showText.indexOf("}", iStart);
        if (braceStart >= 0 && braceEnd > braceStart) {
            String inside = showText.substring(braceStart + 1, braceEnd);
            String[] entries = inside.split(",");
            for (String e : entries) {
                String[] kv = e.trim().split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    try {
                        Integer v = Integer.parseInt(kv[1].trim());
                        pos.put(key, v);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return pos;
    }

    private p3.FileOrganization getCurrentOrg() {
        // acceso a la organización actual; si no la tienes expuesta, puedes mantener referencia al cambiar setorg
        // en este ejemplo asumimos acceso por fs (puedes agregar un getter en FileSystem si no lo tienes)
        try {
            java.lang.reflect.Field f = p3.FileSystem.class.getDeclaredField("currentOrg");
            f.setAccessible(true);
            return (p3.FileOrganization) f.get(fs);
        } catch (Exception ex) {
            return null;
        }
    }



    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Paguinas;
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JTextField cpuBurst;
    private javax.swing.JButton crearAleatorio;
    private javax.swing.JButton jButtonInfoAlgorimo;
    private javax.swing.JButton jButtonLimpiarTodo;
    private javax.swing.JButton jButtonRUNRemplazo;
    private javax.swing.JButton jButtonSubirPaguina;
    private javax.swing.JComboBox<String> jComboBoxAlgoritmos;
    private javax.swing.JComboBox<String> jComboBoxPaguina;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelArchivosMemoria;
    private javax.swing.JPanel jPanelResultadosRemplazo;
    private javax.swing.JButton jRepetir;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPanelSimulacionRemplazo;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTable jTablePaguinas;
    private javax.swing.JTextArea jTextAreaConsola;
    private javax.swing.JTextField jTextFieldEntrada;
    private javax.swing.JTree jTreeArbol;
    private javax.swing.JPanel panelGantt;
    private javax.swing.JTextField quantum;
    private javax.swing.JLabel quantumActual;
    private javax.swing.JButton simularRun;
    private javax.swing.JTable tablaProcesos;
    private javax.swing.JTextArea textAreaResultados;
    private javax.swing.JTextField tiempoLlegada;
    private javax.swing.JComboBox<String> tipoAlgoritmo;
    // End of variables declaration//GEN-END:variables
}
