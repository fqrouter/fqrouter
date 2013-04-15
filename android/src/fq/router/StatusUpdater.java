package fq.router;

public interface StatusUpdater {
    void updateStatus(String status);
    void appendLog(String log);
    void activateAndClickManageButton();
    void reportError(String msg, Exception e);
}
