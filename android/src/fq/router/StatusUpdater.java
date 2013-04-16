package fq.router;

public interface StatusUpdater {
    void updateStatus(String status);
    void appendLog(String log);
    void activateManageButton();
    void reportError(String msg, Exception e);
}
