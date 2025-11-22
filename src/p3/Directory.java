package p3;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author RVega
 */

// Directory.java
import java.util.*;
import p3.FileItem;

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
