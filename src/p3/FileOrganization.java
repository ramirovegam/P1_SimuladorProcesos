
// p3/FileOrganization.java
package p3;

public interface FileOrganization {
    /** Inserta (si la clave existe, reemplaza o rechaza según la política) */
    void insert(StudentRecord r);

    /** Busca por clave (noAlumno) */
    StudentRecord find(String key);

    /** Muestra estructura interna y registros (para la CLI) */
    String show();
}
