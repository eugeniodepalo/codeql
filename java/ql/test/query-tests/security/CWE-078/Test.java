import java.lang.ProcessBuilder;
import java.util.List;
import java.util.ArrayList;

class Test {
  public static void shellCommand(String arg) throws java.io.IOException {
    ProcessBuilder pb = new ProcessBuilder("/bin/bash -c echo " + arg); // $ Alert[java/concatenated-command-line] Alert[java/command-line-injection]
    pb.start();

    pb = new ProcessBuilder(new String[]{"/bin/bash", "-c", "echo " + arg}); // $ Alert[java/command-line-injection]
    pb.start();

    List<String> cmd = new ArrayList<String>();
    cmd.add("/bin/bash");
    cmd.add("-c");
    cmd.add("echo " + arg);

    pb = new ProcessBuilder(cmd); // $ Alert[java/command-line-injection]
    pb.start();

    String[] cmd1 = new String[]{"/bin/bash", "-c", "<cmd>"};
    cmd1[1] = "echo " + arg;

    pb = new ProcessBuilder(cmd1); // $ Alert[java/command-line-injection]
    pb.start();
  }

  public static void nonShellCommand(String arg) throws java.io.IOException {
    ProcessBuilder pb = new ProcessBuilder("./customTool " + arg); // $ Alert[java/concatenated-command-line] Alert[java/command-line-injection]
    pb.start();

    pb = new ProcessBuilder(new String[]{"./customTool", arg});
    pb.start();

    List<String> cmd = new ArrayList<String>();
    cmd.add("./customTool");
    cmd.add(arg);

    pb = new ProcessBuilder(cmd);
    pb.start();

    String[] cmd1 = new String[]{"./customTool", "<arg>"};
    cmd1[1] = arg;

    pb = new ProcessBuilder(cmd1);
    pb.start();

    pb = new ProcessBuilder(new String[]{"cp", arg, "/tmp/destination"}); // $ Alert[java/relative-path-command]
    pb.start();
  }

  public static void delegatingLaunchers(String arg) throws java.io.IOException {
    new ProcessBuilder(new String[]{"chroot", "/tmp/root", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"env", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"nice", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"nohup", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"setsid", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"stdbuf", "-oL", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"sudo", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"timeout", "10", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
    new ProcessBuilder(new String[]{"xargs", arg}).start(); // $ Alert[java/command-line-injection] Alert[java/relative-path-command]
  }

  public static void relativeCommand() throws java.io.IOException {
      ProcessBuilder pb = new ProcessBuilder("ls"); // $ Alert[java/relative-path-command]
      pb.start();

      pb = new ProcessBuilder("/bin/ls");
      pb.start();
  }

  public static void main(String[] args) throws java.io.IOException { // $ Source[java/command-line-injection]
      String arg = args.length > 1 ? args[1] : "default";

      shellCommand(arg);
      nonShellCommand(arg);
      delegatingLaunchers(arg);
      relativeCommand();
  }
}
