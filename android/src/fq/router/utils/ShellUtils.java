package fq.router.utils;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ShellUtils {

    private static final String[] BINARY_PLACES = {"/data/bin/", "/system/bin/", "/system/xbin/", "/sbin/",
            "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/",
            "/data/local/"};
    private static boolean NO_SUDO = false;

    public static String execute(String... command) throws Exception {
        Process process = executeNoWait(new HashMap<String, String>(), command);
        return waitFor(Arrays.toString(command), process);
    }

    public static Process executeNoWait(Map<String, String> env, String... command) throws IOException {
        LogUtils.i("command: " + Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(env);
        return processBuilder
                .redirectErrorStream(true)
                .start();
    }


    public static String sudo(String... command) throws Exception {
        if (NO_SUDO) {
            return execute(command);
        }
        Process process = sudoNoWait(new HashMap<String, String>(), command);
        return waitFor(Arrays.toString(command), process);
    }


    public static Process sudoNoWait(Map<String, String> env, String... command) throws Exception {
        if (NO_SUDO) {
            return executeNoWait(env, command);
        }
        LogUtils.i("sudo: " + Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.environment().putAll(env);
        Process process = processBuilder
                .command(findCommand("su"))
                .redirectErrorStream(true)
                .start();
        OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
        try {
            stdin.write("echo going to run some command\n");
            for (String c : command) {
                stdin.write(c);
                stdin.write(" ");
            }
            stdin.write("\nexit\n");
        } finally {
            stdin.close();
        }
        return process;
    }

    public static String findCommand(String command) {
        for (String binaryPlace : BINARY_PLACES) {
            String path = binaryPlace + command;
            if (new File(path).exists()) {
                return path;
            }
        }
        return command;
    }

    public static String waitFor(String command, Process process) throws Exception {

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        try {
            String line;
            while (null != (line = stdout.readLine())) {
                output.append(line);
                output.append("\n");
            }
        } finally {
            stdout.close();
        }
        process.waitFor();
        int exitValue = process.exitValue();
        if (0 != exitValue) {
            throw new Exception("failed to execute: " + command + ", exit value: " + exitValue + ", output: " + output);
        }
        return output.toString();
    }

    public static boolean isRooted() {
        NO_SUDO = false;
        boolean isRooted;
        try {
            isRooted = sudo("echo", "hello").contains("hello");
        } catch (Exception e) {
            isRooted = false;
        }
        NO_SUDO = !isRooted;
        return isRooted;
    }
}
