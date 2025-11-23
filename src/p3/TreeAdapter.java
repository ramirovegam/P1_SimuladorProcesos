
package p3;

import javax.swing.JTree;
import javax.swing.tree.*;

public class TreeAdapter {

    public static void refreshJTree(JTree tree, Directory root, Directory current) {
        DefaultMutableTreeNode rootNode = buildNode(root);
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        tree.setModel(model);
        expandAll(tree);
        selectPath(tree, current.getPath());
    }

    private static DefaultMutableTreeNode buildNode(Directory dir) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir.getName());
        // subdirectorios
        for (Directory d : dir.getSubDirs().values()) {
            node.add(buildNode(d));
        }
        // archivos (hojas)
        for (FileItem f : dir.getFiles().values()) {
            node.add(new DefaultMutableTreeNode(f.getName()));
        }
        return node;
    }

    private static void expandAll(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    /** Seleccionar nodo del JTree por path (p.ej. /home/ramiro) */
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
            currentPath = findChildPath(currentPath, part);
            if (currentPath == null) break;
        }
        if (currentPath != null) tree.setSelectionPath(currentPath);
    }

    private static TreePath findChildPath(TreePath parentPath, String childName) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode c = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (childName.equals(c.getUserObject().toString())) {
                return parentPath.pathByAddingChild(c);
            }
        }
        return null;
    }
}
