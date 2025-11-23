
package p3;

public class FileItem {
    private final String name;
    private String content;

    public FileItem(String name) { this.name = name; this.content = ""; }

    public String getName() { return name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
