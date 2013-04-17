package fq.router;

public interface StatusUpdater {
    void updateStatus(String status);
    void appendLog(String log);
    void onStarted();
    void reportError(String msg, Exception e);
    String getMyVersion();
    void notifyNewerVersion(String latestVersion, String upgradeUrl);
}
