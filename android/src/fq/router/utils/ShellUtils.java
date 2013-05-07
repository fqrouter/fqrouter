package fq.router.utils;

import android.util.Log;

import java.io.BufferedReader;
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

    public static String sudo(boolean returnsOutput, String... command) throws Exception {
        Log.d("fqrouter", "sudo: " + Arrays.toString(command));
        Process process = new ProcessBuilder()
                .command("su")
                .redirectErrorStream(true)
                .start();
        OutputStreamWriter stdin = new OutputStreamWriter(process.getOutputStream());
        try {
            stdin.write("echo going to run some command\n");
            for (String c : command) {
                stdin.write(c);
                stdin.write(" ");
            }
            stdin.write("\n");
        } finally {
            stdin.close();
        }
        if (returnsOutput) {
            return waitFor(Arrays.toString(command), process);
        } else {
            process.waitFor();
            int exitValue = process.exitValue();
            if (0 != exitValue) {
                throw new Exception("failed to execute: " + Arrays.toString(command) + ", exit value: " + exitValue);
            }
            return "";
        }
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
}
