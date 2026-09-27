package ee.markkuskoodi.c4generator.ingest;

/** A problem with the tool's inputs the user must fix; reported without a stack trace. */
public class IngestException extends RuntimeException {
    public IngestException(String message) {
        super(message);
    }

    public IngestException(String message, Throwable cause) {
        super(message, cause);
    }
}