package tn.softsys.buanderie.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " introuvable avec l'id: " + id);
    }
    public ResourceNotFoundException(String entity, String field, String value) {
        super(entity + " introuvable avec " + field + ": " + value);
    }

    public ResourceNotFoundException(String service, String id, Long serviceId) {
    }
}
