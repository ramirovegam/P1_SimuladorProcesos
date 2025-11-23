
// p3/StudentRecord.java
package p3;

import java.util.Objects;

public class StudentRecord {
    public final String noAlumno;        // clave primaria
    public final String nombre;
    public final String apellidoPaterno;
    public final String apellidoMaterno;
    public final String telefono;
    public final String calle;
    public final String codigoPostal;

    public StudentRecord(String noAlumno, String nombre, String apellidoPaterno,
                         String apellidoMaterno, String telefono, String calle,
                         String codigoPostal) {
        this.noAlumno = noAlumno;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.telefono = telefono;
        this.calle = calle;
        this.codigoPostal = codigoPostal;
    }

    public String key() { return noAlumno; }

    @Override public String toString() {
        return String.format(
            "{no=%s, nombre=%s, apPat=%s, apMat=%s, tel=%s, calle=%s, cp=%s}",
            noAlumno, nombre, apellidoPaterno, apellidoMaterno, telefono, calle, codigoPostal
        );
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentRecord)) return false;
        StudentRecord that = (StudentRecord) o;
        return Objects.equals(noAlumno, that.noAlumno);
    }
    @Override public int hashCode() { return Objects.hash(noAlumno); }
}
