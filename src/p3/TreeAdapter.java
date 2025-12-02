
package p3;

import javax.swing.JTree;
import javax.swing.tree.*;
import java.util.*;

/**
 * Adaptador para construir y manejar el JTree del sistema de archivos simulado.
 * - Distingue directorios y archivos mediante FsNode.
 * - Ordena directorios y archivos alfabéticamente (cascada).
 * - Selecciona el path actual (cwd) correctamente.
 */
public class TreeAdapter {

    /**
     * Objeto del nodo (userObject) que indica si es directorio o archivo.
     */
    public static class FsNode {
        public final String name;
        public final boolean isDirectory;

        public FsNode(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }

        @Override
        public String toString() {
            return name; // texto que muestra el árbol
        }
    }

    public static void refreshJTree(JTree tree, Directory root, Directory current) {
        DefaultMutableTreeNode rootNode = buildNode(root);
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.setModel(model);
        expandAll(tree);
        selectPath(tree, current.getPath());
    }

    /**
     * Construye el árbol recursivamente con orden por cascada:
     * - Primero subdirectorios ordenados por nombre.
     * - Luego archivos ordenados por nombre.
     */
    private static DefaultMutableTreeNode buildNode(Directory dir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FsNode(dir.getName(), true));

        // --- Ordenar subdirectorios ---
        List<Directory> subDirs = new ArrayList<>(dir.getSubDirs().values());
        subDirs.sort(Comparator.comparing(Directory::getName));

        for (Directory d : subDirs) {
            node.add(buildNode(d));
        }

        // --- Ordenar archivos ---
        List<FileItem> files = new ArrayList<>(dir.getFiles().values());
        files.sort(Comparator.comparing(FileItem::getName));

        for (FileItem f : files) {
            node.add(new DefaultMutableTreeNode(new FsNode(f.getName(), false)));
        }
        return node;
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    /**
     * Selecciona el path actual (p.ej. "/ramiro/lucas").
     * Busca solo directorios (FsNode.isDirectory == true).
     */
    private static void selectPath(JTree tree, String path) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        TreePath tp = new TreePath(root);
        if (path.equals("/")) {
            tree.setSelectionPath(tp);
            return;
        }
        String[] parts = path.split("/");
        TreePath currentPath = tp;
        for (String part : parts) {
            if (part.isBlank() || part.equals("/")) continue;
            currentPath = findChildDirPath(currentPath, part);
            if (currentPath == null) break;
        }
        if (currentPath != null) tree.setSelectionPath(currentPath);
    }

    /**
     * Encuentra un hijo cuyo userObject sea FsNode de directorio con nombre 'childName'.
     */
    private static TreePath findChildDirPath(TreePath parentPath, String childName) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode c = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object uo = c.getUserObject();
            if (uo instanceof FsNode) {
                FsNode fn = (FsNode) uo;
                if (fn.isDirectory && childName.equals(fn.name)) {
                    return parentPath.pathByAddingChild(c);
                }
            }
        }
        return null;
    }
}
