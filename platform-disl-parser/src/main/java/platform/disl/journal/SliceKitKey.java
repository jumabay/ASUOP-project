package platform.disl.journal;

import java.util.Objects;

/**
 * Idempotency key of a kit: file name + slice type (as {@code IsImportedFile(filename, type)}).
 */
public final class SliceKitKey {

    private final String fileName;
    private final int type;

    public SliceKitKey(String fileName, int type) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must be non-blank");
        }
        this.fileName = fileName;
        this.type = type;
    }

    public String fileName() {
        return fileName;
    }

    public int type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SliceKitKey that)) {
            return false;
        }
        return type == that.type && fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, type);
    }

    @Override
    public String toString() {
        return fileName + "#" + type;
    }
}
