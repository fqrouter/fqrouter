package fq.router.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class ShellUtils {

    public static String execute(String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        return waitFor(Arrays.toString(command), process);
    }

    public static String sudo(String... command) throws Exception {
        return sudo(true, command);
    }

    public static String sudo(boolean logsOutput, String... command) throws Exception {
        Log.d("fqrouter", "sudo: " + Arrays.toString(command));
        Process process = new ProcessBuilder()
                .command("su")
                .redirectErrorStream(true)
                .start();
        OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
        try {
            for (String c : command) {
                stdin.write(c);
                stdin.write(" ");
            }
            stdin.write("\n");
        } finally {
            stdin.close();
        }
        return waitFor(Arrays.toString(command), process, logsOutput);
    }

    public static String waitFor(String command, Process process) throws Exception {
        return waitFor(command, process, true);
    }

    public static String waitFor(String command, Process process, boolean logsOutput) throws Exception {

        BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        try {
            String line;
            while (null != (line = stdout.readLine())) {
                if (logsOutput) {
                    Log.d("fqrouter", "shell: " + line);
                }
                output.append(line);
                output.append("\n");
            }
        } finally {
            stdout.close();
        }
        process.waitFor();
        if (0 != process.exitValue()) {
            throw new Exception("failed to execute: " + command);
        }
        return output.toString();
    }

    public static String getPath(String command) {
        if (new File("/system/bin/" + command).exists()) {
            return "/system/bin/" + command;
        }
        if (new File("/system/xbin/" + command).exists()) {
            return "/system/xbin/" + command;
        }
        throw new RuntimeException("can not find path of command: " + command);
    }
}
