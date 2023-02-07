/* 
 * Common utilities 
*/
package modder;

import java.io.IOException;

public class Util {

    public static boolean DoesCommandExist(String command) {
        // TODO: add log warning when exception
        // - this way of checking is quite dangerous
        // - because it actually try to execute it
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(command + " --help");

            try {
                // wait for program to exit
                // this is needed, because exec will run on
                // different thread
                // https://stackoverflow.com/questions/25979336/handling-the-illegalthreadstateexception
                process.waitFor();
            } catch (InterruptedException interruptedException) {
                // System.out.println("false, interrupted exception");
                return false;

            }
            int exitCode = process.exitValue();

            // if program is able to return a ret code
            // then it exist
            // System.out.println("exit code" + exitCode);
            return true;
        }

        catch (IOException e) {
            // System.out.println("false, IO exception");
            return false;
        }

    }

}
