
package p3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor del simulador de sistema de archivos + organización de registros de alumnos.
 * Devuelve CommandResult con flags para que la UI (Swing) decida si refrescar árbol, limpiar consola o reiniciar.
 */
public class FileSystem {
    // --- Sistema de archivos básico ---
    private final Directory root = new Directory("/");
    private Directory current = root;

    // --- Organización de archivos para alumnos (por defecto: Pile) ---
    private FileOrganization currentOrg = new PileOrganization();

    public FileSystem() {
        // Sistema vacío por defecto (solo "/").
        // Si quieres iniciar con /home:
        // mkdirInternal(root, "home");
    }

    // === API del FS (usada por CLI) ===
    public Directory getRoot() { return root; }
    public Directory getCurrent() { return current; }
    public String pwd() { return current.getPath(); }

    public String ls() {
        StringBuilder sb = new StringBuilder();
        sb.append("Directorios: ").append(current.getSubDirs().keySet()).append("\n");
        sb.append("Archivos: ").append(current.getFiles().keySet());
        return sb.toString();
    }

    public boolean dirExists(String name) {
        return current.getSubDirs().containsKey(name);
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
        if (f == null) { // crea si no existe
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

    // === Resultado para la UI ===
    public static class CommandResult {
        public final String output;
        public final boolean refreshTree;
        public final boolean clearConsole;  // limpiar consola en UI
        public final boolean requestReset;  // reiniciar FS y consola en UI

        public CommandResult(String out, boolean refreshTree) {
            this(out, refreshTree, false, false);
        }
        public CommandResult(String out, boolean refreshTree, boolean clearConsole, boolean requestReset) {
            this.output = out;
            this.refreshTree = refreshTree;
            this.clearConsole = clearConsole;
            this.requestReset = requestReset;
        }
    }

    // === Comandos de la CLI ===
    public CommandResult executeCommand(String cmdLine) {
        // echo "texto" > archivo / echo "texto" >> archivo
        if (cmdLine.startsWith("echo ")) {
            return parseEcho(cmdLine);
        }

        String[] parts = cmdLine.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return new CommandResult("", false);
        }

        String cmd = parts[0];
        switch (cmd) {
            // --- Comandos FS ---
            case "pwd":
                return new CommandResult(pwd(), false);

            case "ls":
                return new CommandResult(ls(), false);

            case "mkdir":
                if (parts.length < 2) return new CommandResult("Uso: mkdir <nombre>", false);
                if (dirExists(parts[1])) return new CommandResult("ERROR: ya existe el directorio " + parts[1], false);
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
                boolean refresh = rmr.startsWith("OK");
                return new CommandResult(rmr, refresh);

            case "help":
                return new CommandResult(helpText(), false);

            // --- Limpieza y reinicio de la UI/FS ---
            case "clear":
                // Pide a la UI limpiar la consola; no toca el FS
                return new CommandResult("", false, true, false);

            case "reset":
                // Pide a la UI limpiar consola y reiniciar FS (nuevo objeto en UI)
                return new CommandResult("Simulador reiniciado (FS y consola).", true, true, true);

            // --- Organización de archivos (alumnos) ---
            case "setorg":
                if (parts.length < 2)
                    return new CommandResult("Uso: setorg <pile|sec|sec_index|indexado|hash>", false);
                switch (parts[1].toLowerCase()) {
                    case "pile":        currentOrg = new PileOrganization(); break;
                    case "sec":         currentOrg = new SecuencialOrganization(); break;
                    case "sec_index":   currentOrg = new SecuencialIndexadoOrganization(); break;
                    case "indexado":    currentOrg = new IndexadoOrganization(); break;
                    case "hash":        currentOrg = new HashOrganization(); break;
                    default: return new CommandResult("Organización no válida", false);
                }
                return new CommandResult("Organización cambiada a " + parts[1], false);

            case "insertalumno": {
                // Formato: insertalumno <no> "<nombre>" "<apPat>" "<apMat>" "<tel>" "<calle>" "<cp>"
                java.util.List<String> args = parseQuotedArgs(cmdLine);
                if (args.size() != 8) {
                    return new CommandResult(
                        "Uso: insertalumno <no> \"<nombre>\" \"<apPat>\" \"<apMat>\" \"<tel>\" \"<calle>\" \"<cp>\"",
                        false
                    );
                }
                StudentRecord r = new StudentRecord(
                    args.get(1), args.get(2), args.get(3), args.get(4),
                    args.get(5), args.get(6), args.get(7)
                );
                currentOrg.insert(r);
                return new CommandResult("Insertado alumno no=" + r.noAlumno, false);
            }

            case "findalumno":
                if (parts.length < 2) return new CommandResult("Uso: findalumno <no>", false);
                StudentRecord f = currentOrg.find(parts[1]);
                return new CommandResult(f != null ? f.toString() : "No encontrado", false);

            case "showalumnos":
                return new CommandResult(currentOrg.show(), false);

            default:
                return new CommandResult("Comando no reconocido. Usa 'help'.", false);
        }
    }

    // === Helpers internos ===

    private CommandResult parseEcho(String line) {
        String regex = "^echo\\s+\"(.*)\"\\s*(>>|>)\\s*(\\S+)$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(line);
        if (!m.matches()) {
            return new CommandResult("Uso: echo \"texto\" > archivo | echo \"texto\" >> archivo", false);
        }
        String text = m.group(1);
        boolean append = ">>".equals(m.group(2));
        String file = m.group(3);
        String r = echoToFile(text, file, append);
        return new CommandResult(r, false);
    }

    private String helpText() {
        return String.join("\n",
            "Comandos FS:",
            "  pwd                 - muestra directorio actual",
            "  ls                  - lista contenido",
            "  mkdir <nombre>      - crea directorio",
            "  touch <nombre>      - crea archivo vacío",
            "  cd <ruta>           - cambia directorio (., .., nombre, /absoluto)",
            "  echo \"texto\" > f    - escribe (sobrescribe) en archivo",
            "  echo \"texto\" >> f   - agrega al final",
            "  cat <archivo>       - muestra contenido",
            "  rm <archivo|dir>    - elimina (dir debe estar vacío)",
            "  clear               - limpia la consola (UI)",
            "  reset               - reinicia consola y sistema de archivos (UI + FS)",
            "",
            "Organización de archivos (alumnos):",
            "  setorg <pile|sec|sec_index|indexado|hash>",
            "  insertalumno <no> \"<nombre>\" \"<apPat>\" \"<apMat>\" \"<tel>\" \"<calle>\" \"<cp>\"",
            "  findalumno <no>",
            "  showalumnos",
            "",
            "  help                - esta ayuda",
            "  exit                - salir (lo maneja la UI)"
        );
    }

    /** Resolver rutas simples (absolutas/relativas), con . y .. */
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

    /** Parsea línea respetando argumentos entre comillas: insertalumno ... "Juan Carlos" ... */
    private java.util.List<String> parseQuotedArgs(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"([^\"]*)\"|(\\S+)")
                .matcher(line);
        while (m.find()) {
            if (m.group(1) != null) out.add(m.group(1));  // contenido entre comillas
            else out.add(m.group(2));                     // token normal
        }
        return out;
    }
}
