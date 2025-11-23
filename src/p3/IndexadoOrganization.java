
// p3/IndexadoOrganization.java
package p3;

import java.util.*;

public class IndexadoOrganization implements FileOrganization {
    // Índice primario por clave
    private final Map<String, StudentRecord> primario = new HashMap<>();
    // Ejemplo de índice secundario por apellido paterno (opcional demo)
    private final Map<String, List<String>> idxApellido = new HashMap<>();

    @Override public void insert(StudentRecord r) {
        primario.put(r.key(), r);
        // actualiza índice secundario (demo)
        idxApellido.computeIfAbsent(r.apellidoPaterno.toLowerCase(), k -> new ArrayList<>()).add(r.key());
    }

    @Override public StudentRecord find(String key) {
        return primario.get(key);
    }

    @Override public String show() {
        return "Indexado (primario por noAlumno):\n" + primario +
               "\nIdxSecundario(apellidoPaterno -> [noAlumno]): " + idxApellido;
    }
}
