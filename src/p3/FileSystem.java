/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package p3;

/**
 *
 * @author RVega
 */

// FileSystem.java
import java.util.*;

public class FileSystem {
    private final Directory root = new Directory("/");
    private Directory current = root;

    public FileSystem() {
        // ejemplo: crear /home/ramiro
        Directory home = mkdirInternal(root, "home");
        Directory ramiro = mkdirInternal(home, "ramiro");
    }

    public Directory getRoot() { return root; }
    public Directory getCurrent() { return current; }

    public String pwd() { return current.getPath(); }

    public String ls() {
        StringBuilder sb = new StringBuilder();
        sb.append("Directorios: ").append(current.getSubDirs().keySet()).append("\n");
        sb.append("Archivos: ").append(current.getFiles().keySet());
        return sb.toString();
    }

    public boolean mkdir(String name) {
        if (name == null || name.isBlank()) return false;
        if (current.getSubDirs().containsKey(name)) return false;
        mkdirInternal(current, name);
        return true;
    }
    private Directory mkdirInternal(Directory parent, String name) {
        Directory d = new Directory(name);
        d.setParent(parent);
        parent.getSubDirs().put(name, d);
        return d;
    }

    public boolean touch(String name) {
        if (name == null || name.isBlank()) return false;
        if (current.getFiles().containsKey(name)) return true; // ya existe
        current.getFiles().put(name, new FileItem(name));
        return true;
    }

    public String cd(String path) {
        Directory target = resolveDir(path);
        if (target == null) return "ERROR: directorio no encontrado: " + path;
        current = target;
        return "OK: cwd -> " + current.getPath();
    }

    public String cat(String name) {
        FileItem f = current.getFiles().get(name);
        if (f == null) return "ERROR: archivo no encontrado: " + name;
        return f.getContent();
    }

    public String echoToFile(String text, String name, boolean append) {
        FileItem f = current.getFiles().get(name);
        if (f == null) { // si no existe, lo creamos rápido
            f = new FileItem(name);
            current.getFiles().put(name, f);
        }
        if (append) f.setContent(f.getContent() + text);
        else f.setContent(text);
        return "OK: escrito en " + name;
    }

    public String rm(String name) {
        if (current.getFiles().remove(name) != null) {
            return "OK: archivo eliminado " + name;
        }
        Directory d = current.getSubDirs().get(name);
        if (d != null && d.getSubDirs().isEmpty() && d.getFiles().isEmpty()) {
            current.getSubDirs().remove(name);
            return "OK: directorio eliminado " + name;
        }
        if (d != null) return "ERROR: directorio no vacío";
        return "ERROR: no existe " + name;
    }

    // --- Parser central de comandos ---
    public CommandResult executeCommand(String cmdLine) {
        // manejamos echo "texto" > archivo   y   echo "texto" >> archivo
        if (cmdLine.startsWith("echo ")) {
            return parseEcho(cmdLine);
        }
        String[] parts = cmdLine.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank())
            return new CommandResult("", false);

        String cmd = parts[0];
        switch (cmd) {
            case "pwd":
                return new CommandResult(pwd(), false);
            case "ls":
                return new CommandResult(ls(), false);
            case "mkdir":
                if (parts.length < 2) return new CommandResult("Uso: mkdir <nombre>", false);
                boolean ok = mkdir(parts[1]);
                return new CommandResult(ok ? "OK: directorio creado " + parts[1] : "ERROR: no se pudo crear", true);
            case "touch":
                if (parts.length < 2) return new CommandResult("Uso: touch <nombre>", false);
                boolean okf = touch(parts[1]);
                return new CommandResult(okf ? "OK: archivo creado " + parts[1] : "ERROR: no se pudo crear", true);
            case "cd":
                if (parts.length < 2) return new CommandResult("Uso: cd <ruta>", false);
                String res = cd(parts[1]);
                return new CommandResult(res, true);
            case "cat":
                if (parts.length < 2) return new CommandResult("Uso: cat <archivo>", false);
                return new CommandResult(cat(parts[1]), false);
            case "rm":
                if (parts.length < 2) return new CommandResult("Uso: rm <archivo|directorio>", false);
                String rmr = rm(parts[1]);
                // refrescamos árbol si se elimina algo
                boolean refresh = rmr.startsWith("OK");
                return new CommandResult(rmr, refresh);
            case "help":
                return new CommandResult(helpText(), false);
            default:
                return new CommandResult("Comando no reconocido. Usa 'help'.", false);
        }
    }

    private CommandResult parseEcho(String line) {
        // Formatos soportados:
        // echo "Hola" > notas.txt
        // echo "Hola" >> notas.txt
        String regex = "^echo\\s+\"(.*)\"\\s*(>>|>)\\s*(\\S+)$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher m = p.matcher(line);
        if (!m.matches()) return new CommandResult("Uso: echo \"texto\" > archivo | echo \"texto\" >> archivo", false);
        String text = m.group(1);
        boolean append = ">>".equals(m.group(2));
        String file = m.group(3);
        String r = echoToFile(text, file, append);
        return new CommandResult(r, false);
    }

    private String helpText() {
        return String.join("\n",
            "Comandos:",
            "  pwd                 - muestra directorio actual",
            "  ls                  - lista contenido",
            "  mkdir <nombre>      - crea directorio",
            "  touch <nombre>      - crea archivo vacío",
            "  cd <ruta>           - cambia directorio (., .., nombre)",
            "  echo \"texto\" > f    - escribe (sobrescribe) en archivo",
            "  echo \"texto\" >> f   - agrega al final",
            "  cat <archivo>       - muestra contenido",
            "  rm <archivo|dir>    - elimina (dir debe estar vacío)",
            "  help                - esta ayuda",
            "  exit                - salir (lo maneja la UI)"
        );
    }

    // Resuelve rutas sencillas (absolutas / relativas), con . y ..
    private Directory resolveDir(String path) {
        if (path == null || path.isBlank()) return current;
        Directory start = path.startsWith("/") ? root : current;
        String[] parts = path.split("/");
        Directory cur = start;
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (cur.getParent() != null) cur = cur.getParent();
                continue;
            }
            Directory next = cur.getSubDirs().get(part);
            if (next == null) return null;
            cur = next;
        }
        return cur;
    }

    // Pequeño wrapper para devolver texto y si hay que refrescar JTree
    public static class CommandResult {
        public final String output;
        public final boolean refreshTree;
        public CommandResult(String out, boolean refresh) {
            this.output = out; this.refreshTree = refresh;
        }
    }
}
