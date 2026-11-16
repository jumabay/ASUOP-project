package platform.rwl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Offline client: loads classpath fixtures. Never opens network sockets. */
public final class FixtureRwlApiClient implements RwlApiClient {

    private final ClassLoader classLoader;
    private final String classpathResource;

    public FixtureRwlApiClient(String classpathResource) {
        this.classLoader = FixtureRwlApiClient.class.getClassLoader();
        this.classpathResource = Objects.requireNonNull(classpathResource, "classpathResource");
    }

    @Override
    public RwlFetchResponse fetch(RwlFetchRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        try (InputStream in = classLoader.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("Missing fixture: " + classpathResource);
            }
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new RwlFetchResponse(200, body, "fixture://" + classpathResource);
        }
    }
}
