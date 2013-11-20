package fq.router2.life_cycle;

import android.content.Context;
import android.os.Build;
import fq.router2.R;
import fq.router2.feedback.HandleAlertIntent;
import fq.router2.utils.IOUtils;
import fq.router2.utils.LogUtils;
import fq.router2.utils.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class Deployer {

    public static File PAYLOAD_ZIP = new File(ShellUtils.DATA_DIR, "payload.zip");
    public static File PAYLOAD_CHECKSUM = new File(ShellUtils.DATA_DIR, "payload.checksum");
    public static File PYTHON_DIR = new File(ShellUtils.DATA_DIR, "python");
    public static File PYTHON_LAUNCHER = new File(PYTHON_DIR, "bin/python");
    public static File WIFI_TOOLS_DIR = new File(ShellUtils.DATA_DIR, "wifi-tools");
    public static File PROXY_TOOLS_DIR = new File(ShellUtils.DATA_DIR, "proxy-tools");
    public static File MANAGER_DIR = new File(ShellUtils.DATA_DIR, "manager");
    public static File MANAGER_MAIN_PY = new File(MANAGER_DIR, "main.py");
    public static File MANAGER_VPN_PY = new File(MANAGER_DIR, "vpn.py");
    private final Context context;

    public Deployer(Context context) {
        this.context = context;

    }

    public String deploy() {
        LogUtils.i("Deploying payload");
        try {
            copyBusybox();
            makeExecutable(ShellUtils.BUSYBOX_FILE);
        } catch (Exception e) {
            return logFatalError("failed to copy busybox", e);
        }
        boolean foundPayloadUpdate;
        try {
            foundPayloadUpdate = shouldDeployPayload();
        } catch (Exception e) {
            return logFatalError("failed to check update", e);
        }
        if (foundPayloadUpdate) {
            context.sendBroadcast(new LaunchingIntent(_(R.string.status_clear_old_files), 30));
            try {
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    LogUtils.e("failed to kill manager before redeploy", e);
                    // ignore and continue
                }
                clearDataDirectory();
            } catch (Exception e) {
                return logFatalError("failed to clear data directory", e);
            }
        }
        context.sendBroadcast(new LaunchingIntent(_(R.string.status_copy_files), 35));
        try {
            copyBusybox();
            makeExecutable(ShellUtils.BUSYBOX_FILE);
            copyPayloadZip();
        } catch (Exception e) {
            return logFatalError("failed to copy payload.zip", e);
        }
        context.sendBroadcast(new LaunchingIntent(_(R.string.status_unzip_files), 40));
        try {
            unzipPayloadZip();
        } catch (Exception e) {
            String msg = logFatalError("failed to unzip payload.zip", e);
            try {
                if (ShellUtils.execute("getprop", "ro.product.cpu.abi").trim().equals("x86")) {
                    return _(R.string.x86_is_not_supported);
                }
            } catch (Exception e2) {
                // ignore
            }
            return msg;
        }
        try {
            linkLibs();
        } catch (Exception e) {
            LogUtils.e("failed to link libs", e);
        }
        try {
            makePayloadExecutable();
        } catch (Exception e) {
            return logFatalError("failed to make payload executable", e);
        }
        LogUtils.i("Deployed payload");
        return "";
    }

    private String _(int id) {
        return context.getResources().getString(id);
    }


    private boolean shouldDeployPayload() throws Exception {
        if (!PAYLOAD_CHECKSUM.exists()) {
            LogUtils.i("no checksum, assume it is old");
            return true;
        }
        if (!isPayloadComplete()) {
            LogUtils.i("payload is corrupted");
            return true;
        }
        String oldChecksum = IOUtils.readFromFile(PAYLOAD_CHECKSUM);
        InputStream inputStream = context.getAssets().open("payload.zip");
        try {
            String newChecksum = IOUtils.copy(inputStream, null);
            if (oldChecksum.equals(newChecksum)) {
                LogUtils.i("no payload update found");
                return false;
            } else {
                LogUtils.i("found payload update");
                return true;
            }
        } finally {
            inputStream.close();
        }
    }

    private boolean isPayloadComplete() {
        return MANAGER_VPN_PY.exists() && PYTHON_LAUNCHER.exists() && WIFI_TOOLS_DIR.exists();
    }

    private void clearDataDirectory() throws Exception {
        if (ShellUtils.DATA_DIR.exists()) {
            LogUtils.i("clear data dir");
            deleteDirectory(ShellUtils.DATA_DIR + "/python");
            deleteDirectory(ShellUtils.DATA_DIR + "/wifi-tools");
            deleteDirectory(ShellUtils.DATA_DIR + "/proxy-tools");
            deleteDirectory(ShellUtils.DATA_DIR + "/manager");
            deleteDirectory(ShellUtils.DATA_DIR + "/payload.zip");
            new File("/data/data/fq.router2/busybox").delete();
        }
    }

    private void deleteDirectory(String path) throws Exception {
        if (new File(path).exists()) {
            try {
                ShellUtils.execute("/data/data/fq.router2/busybox", "rm", "-rf", path);
            } catch (Exception e) {
                LogUtils.e("failed to delete " + path, e);
            }
        }
        if (new File(path).exists()) {
            LogUtils.e("failed to delete " + path);
        }
    }

    private void copyPayloadZip() throws Exception {
        if (PAYLOAD_ZIP.exists()) {
            LogUtils.i("skip copy payload.zip as it already exists");
            return;
        }
        if (PYTHON_DIR.exists()) {
            LogUtils.i("skip copy payload.zip as it has already been unzipped");
            return;
        }
        LogUtils.i("copying payload.zip to data directory");
        InputStream inputStream = context.getAssets().open("payload.zip");
        try {
            OutputStream outputStream = new FileOutputStream(PAYLOAD_ZIP);
            try {
                String checksum = IOUtils.copy(inputStream, outputStream);
                IOUtils.writeToFile(PAYLOAD_CHECKSUM, checksum);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
        LogUtils.i("successfully copied payload.zip");
    }

    private void copyBusybox() throws Exception {
        if (ShellUtils.BUSYBOX_FILE.exists()) {
            LogUtils.i("skip copy busybox as it already exists");
            return;
        }
        LogUtils.i("copying busybox to data directory");
        InputStream inputStream = context.getAssets().open("busybox");
        try {
            OutputStream outputStream = new FileOutputStream(ShellUtils.BUSYBOX_FILE);
            try {
                IOUtils.copy(inputStream, outputStream);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
        LogUtils.i("successfully copied busybox");
    }

    private void unzipPayloadZip() throws Exception {
        if (PYTHON_DIR.exists()) {
            LogUtils.i("skip unzip payload.zip as it has already been unzipped");
            return;
        }
        LogUtils.i("unzipping payload.zip");
        Process process = Runtime.getRuntime().exec(
                ShellUtils.BUSYBOX_FILE + " unzip -o -q payload.zip", new String[0], ShellUtils.DATA_DIR);
        try {
            ShellUtils.waitFor("unzip", process);
        } catch (Exception e) {
            LogUtils.e("unzip failed", e);
            Thread.sleep(3000);
            process = Runtime.getRuntime().exec(
                    ShellUtils.BUSYBOX_FILE + " unzip -o -q payload.zip", new String[0], ShellUtils.DATA_DIR);
            ShellUtils.waitFor("unzip", process);
        }
        if (!new File("/data/data/fq.router2/payload.zip").delete()) {
            LogUtils.i("failed to delete payload.zip after unzip");
        }
        for (int i = 0; i < 10; i++) {
            LogUtils.i("sleep 2 seconds");
            Thread.sleep(2000); // wait for the files written out
            if (isPayloadComplete()) {
                LogUtils.i("payload is complete");
                break;
            }
        }
        LogUtils.i("successfully unzipped payload.zip");
    }

    private void linkLibs() throws Exception {
        if (Build.VERSION.SDK_INT >= 14) {
            return; // try narrow the effect range
        }
        if (isXperia()) {
            LogUtils.i("skip link libs for xperia"); // will cause reboot
            return;
        }
        if (!shouldLinkLibs()) {
            return;
        }
        ShellUtils.sudo("/data/data/fq.router2/busybox", "mount", "-o", "remount,rw", "/system");
        try {
            File[] files = new File(PYTHON_DIR, "lib").listFiles();
            if (files == null) {
                throw new Exception(new File(PYTHON_DIR, "lib") + " not found");
            } else {
                for (File file : files) {
                    String targetPath = "/system/lib/" + file.getName();
                    if (!new File(targetPath).exists()) {
                        try {
                            LogUtils.i("rm " + targetPath);
                            ShellUtils.sudo("rm " + targetPath);
                        } catch (Exception e) {
                            // ignore
                        }
                        ShellUtils.sudo(ShellUtils.BUSYBOX_FILE.getCanonicalPath(), "ln", "-s", file.getCanonicalPath(), targetPath);
                    }
                }
            }
        } finally {
            ShellUtils.sudo("/data/data/fq.router2/busybox", "mount", "-o", "remount,ro", "/system");
        }
    }

    private boolean isXperia() {
        try {
            return new File("/sbin/ric").exists();
        } catch (Exception e) {
            LogUtils.e("failed to tell if it is xperia", e);
            return true;
        }
    }

    private boolean shouldLinkLibs() throws Exception {
        File[] files = new File(PYTHON_DIR, "lib").listFiles();
        if (files == null) {
            throw new Exception(new File(PYTHON_DIR, "lib") + " not found");
        } else {
            for (File file : files) {
                String targetPath = "/system/lib/" + file.getName();
                if (!new File(targetPath).exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void makePayloadExecutable() throws Exception {
        File[] files = new File(PYTHON_DIR, "bin").listFiles();
        if (files == null) {
            throw new Exception(new File(PYTHON_DIR, "bin") + " not found");
        } else {
            for (File file : files) {
                makeExecutable(file);
            }
        }
        try {
            files = WIFI_TOOLS_DIR.listFiles();
            if (files == null) {
                throw new Exception(WIFI_TOOLS_DIR + " not found");
            } else {
                for (File file : files) {
                    makeExecutable(file);
                }
            }
        } catch (Exception e) {
            LogUtils.e("failed to make wifi tools executable", e);
        }
//        files = PROXY_TOOLS_DIR.listFiles();
//        if (files == null) {
//            throw new Exception(PROXY_TOOLS_DIR + " not found");
//        } else {
//            for (File file : files) {
//                makeExecutable(file);
//            }
//        }
    }

    private void makeExecutable(File file) throws Exception {
        try {
            Method setExecutableMethod = File.class.getMethod("setExecutable", boolean.class, boolean.class);
            if ((Boolean) setExecutableMethod.invoke(file, true, true)) {
                LogUtils.i("successfully made " + file.getName() + " executable");
            } else {
                LogUtils.i("failed to make " + file.getName() + " executable");
                ShellUtils.sudo(ShellUtils.findCommand("chmod"), "0700", file.getCanonicalPath());
            }
        } catch (NoSuchMethodException e) {
            ShellUtils.execute("/data/data/fq.router2/busybox", "chmod", "0700", file.getAbsolutePath());
            LogUtils.i("successfully made " + file.getName() + " executable");
        }
    }

    private String logFatalError(String message, Exception e) {
        LogUtils.e(message, e);
        return message;
    }
}
