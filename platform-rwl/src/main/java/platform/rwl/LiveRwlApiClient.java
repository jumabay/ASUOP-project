package platform.rwl;

import java.io.IOException;

/**
 * Placeholder for a future live client. Any call fails fast without opening sockets.
 */
public final class LiveRwlApiClient implements RwlApiClient {

    @Override
    public RwlFetchResponse fetch(RwlFetchRequest request) throws IOException {
        throw new LiveRwlDisabledException();
    }
}
