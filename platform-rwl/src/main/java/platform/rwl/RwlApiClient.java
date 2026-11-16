package platform.rwl;

import java.io.IOException;

public interface RwlApiClient {

    RwlFetchResponse fetch(RwlFetchRequest request) throws IOException;
}
