package platform.rwl;

/** Subset of documented RWL request_type values used by the offline client. */
public enum RwlRequestType {
    VIEW_VAGON("view_vagon"),
    GET_USER_VAGONS("get_user_vagons"),
    VIEW_VAGON_HISTORY("view_vagon_history"),
    SYNC_VAGONS("sync_vagons");

    private final String wireName;

    RwlRequestType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
