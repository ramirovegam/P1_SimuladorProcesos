package p3;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author RVega
 */

// FileItem.java
public class FileItem {
    private final String name;
    private String content;

    public FileItem(String name) { this.name = name; this.content = ""; }
    public String getName() { return name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}


