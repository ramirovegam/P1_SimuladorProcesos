
package p3;

import java.util.LinkedHashMap;
import java.util.Map;

public class Directory {
    private final String name;
    private Directory parent;
    private final Map<String, Directory> subDirs = new LinkedHashMap<>();
    private final Map<String, FileItem> files = new LinkedHashMap<>();

    public Directory(String name) { this.name = name; }

    public String getName() { return name; }
    public Directory getParent() { return parent; }
    public void setParent(Directory p) { this.parent = p; }
    public Map<String, Directory> getSubDirs() { return subDirs; }
    public Map<String, FileItem> getFiles() { return files; }

    public String getPath() {
        if (parent == null) return "/";
        String pp = parent.getPath();
        return (pp.endsWith("/") ? pp : pp + "/") + name;
    }
}
