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
    new ProcessBuilder(new String[]{"/usr/bin/flock", "/tmp/lock", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/bin/su", "-c", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/sbin/runuser", "-u", "nobody", "--", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/doas", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/time", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/ionice", "-c", "3", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/taskset", "-c", "0", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/watch", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/script", "-c", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/unbuffer", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/bin/busybox", arg}).start(); // $ Alert[java/command-line-injection]
  }

  public static void launcherArguments(String arg) throws java.io.IOException {
    new ProcessBuilder(new String[]{"/usr/bin/env", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/env", "LANG=C", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/env", "--", "LANG=C", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/sudo", "-u", "nobody", "--", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/flock", "/tmp/lock", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/env", "sudo", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/timeout", "10", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/nice", "-n", "5", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/nohup", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/setsid", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/stdbuf", "-oL", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/xargs", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/sbin/chroot", "/tmp/root", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/sbin/runuser", "-u", "nobody", "--", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/doas", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/time", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/ionice", "-c", "3", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/taskset", "-c", "0", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/unbuffer", "/bin/cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/bin/busybox", "cp", arg + "", "/tmp/destination"}).start(); // GOOD
    new ProcessBuilder(new String[]{"/usr/bin/env", "/bin/sh", "-c", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/env", "sudo", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/env", "--", "LANG=C", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/flock", "/tmp/lock", "-c", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/env", "-S", arg}).start(); // $ Alert[java/command-line-injection]
    new ProcessBuilder(new String[]{"/usr/bin/sudo", "-s", "/bin/cp", arg}).start(); // $ Alert[java/command-line-injection]

    List<String> bad = new ArrayList<String>();
    bad.add("/usr/bin/flock");
    bad.add("/tmp/lock");
    bad.add(arg);
    new ProcessBuilder(bad).start(); // $ Alert[java/command-line-injection]

    List<String> shell = new ArrayList<String>();
    shell.add("/usr/bin/env");
    shell.add("/bin/sh");
    shell.add("-c");
    shell.add(arg);
    new ProcessBuilder(shell).start(); // $ Alert[java/command-line-injection]

    String[] badArray = new String[]{"/usr/bin/flock", "/tmp/lock", "<command>"};
    badArray[2] = arg;
    new ProcessBuilder(badArray).start(); // $ Alert[java/command-line-injection]

    String[] shellArray = new String[]{"/usr/bin/env", "/bin/sh", "-c", "<command>"};
    shellArray[3] = arg;
    new ProcessBuilder(shellArray).start(); // $ Alert[java/command-line-injection]

    String[] changedArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    changedArray[1] = "/bin/sh";
    changedArray[3] = arg;
    new ProcessBuilder(changedArray).start(); // $ Alert[java/command-line-injection]

    String[] changedLiteral = new String[]{"/usr/bin/env", "/bin/cp", "-c", arg};
    changedLiteral[1] = "/bin/sh";
    new ProcessBuilder(changedLiteral).start(); // $ Alert[java/command-line-injection]

    String[] unknownIndex = new String[]{"/usr/bin/env", "/bin/cp", "-c", arg};
    unknownIndex[arg.length()] = "/bin/sh";
    new ProcessBuilder(unknownIndex).start(); // $ Alert[java/command-line-injection]

    String[] conditionalArray = new String[]{"/usr/bin/env", "/bin/sh", "-c", "<command>"};
    if (arg.length() > 0) {
      conditionalArray[1] = "/bin/cp";
    }
    conditionalArray[3] = arg;
    new ProcessBuilder(conditionalArray).start(); // $ Alert[java/command-line-injection]

    String[] aliasedLiteral = new String[]{"/usr/bin/env", "/bin/cp", "-c", arg};
    String[] literalAlias = aliasedLiteral;
    literalAlias[1] = "/bin/sh";
    new ProcessBuilder(aliasedLiteral).start(); // $ Alert[java/command-line-injection]

    String[] aliasedArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    String[] arrayAlias = aliasedArray;
    arrayAlias[1] = "/bin/sh";
    aliasedArray[3] = arg;
    new ProcessBuilder(aliasedArray).start(); // $ Alert[java/command-line-injection]

    String[] allocatedArray = new String[4];
    allocatedArray[0] = "/usr/bin/env";
    allocatedArray[1] = "/bin/cp";
    allocatedArray[2] = "-c";
    allocatedArray[3] = arg;
    String[] allocatedAlias = allocatedArray;
    allocatedAlias[1] = "/bin/sh";
    new ProcessBuilder(allocatedArray).start(); // $ Alert[java/command-line-injection]

    String[] escapedArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", arg};
    mutateCommand(escapedArray);
    new ProcessBuilder(escapedArray).start(); // $ Alert[java/command-line-injection]

    String[] capturedArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", arg};
    Runnable mutation = () -> capturedArray[1] = "/bin/sh";
    mutation.run();
    new ProcessBuilder(capturedArray).start(); // $ Alert[java/command-line-injection]

    List<String> aliasedList = new ArrayList<String>();
    aliasedList.add("/usr/bin/env");
    aliasedList.add("/bin/cp");
    aliasedList.add("-c");
    aliasedList.add(arg);
    List<String> listAlias = aliasedList;
    listAlias.set(1, "/bin/sh");
    new ProcessBuilder(aliasedList).start(); // $ Alert[java/command-line-injection]

    String[] castArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    ((String[]) castArray)[1] = "/bin/sh";
    castArray[3] = arg + "";
    new ProcessBuilder(castArray).start(); // $ Alert[java/command-line-injection]

    String[] castOriginal = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    String[] castAlias = castOriginal;
    ((String[]) castOriginal)[1] = "/bin/sh";
    castAlias[3] = arg + "";
    new ProcessBuilder(castAlias).start(); // $ Alert[java/command-line-injection]

    String[] conditionalTarget = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    String[] otherTarget = new String[]{"/bin/echo", "<arg>"};
    (arg.length() > 0 ? conditionalTarget : otherTarget)[1] = "/bin/sh";
    conditionalTarget[3] = arg + "";
    new ProcessBuilder(conditionalTarget).start(); // $ Alert[java/command-line-injection]

    String[] storedArray = new String[]{"/usr/bin/env", "/bin/cp", "-c", "<command>"};
    String[][] storage = new String[][]{storedArray};
    storage[0][1] = "/bin/sh";
    storedArray[3] = arg + "";
    new ProcessBuilder(storedArray).start(); // $ Alert[java/command-line-injection]
  }

  public static void mutateCommand(String[] command) {
    command[1] = "/bin/sh";
  }

  public static void partiallyKnownCommand(String arg, String configuredCommand) throws java.io.IOException {
    String command = "/bin/cp";
    if (arg.length() > 0) {
      command = configuredCommand;
    }
    new ProcessBuilder(new String[]{"/usr/bin/env", command, "-c", arg}).start(); // $ Alert[java/command-line-injection]
  }

  public static void capturedLauncherCommands(String arg) {
    final String launcher = "/usr/bin/env";
    final String command = "/bin/cp";
    Runnable good = () -> new ProcessBuilder(new String[]{launcher, command, arg + "", "/tmp/destination"}); // GOOD
    good.run();

    Runnable nested = () -> {
      Runnable inner = () -> new ProcessBuilder(new String[]{launcher, command, arg + "", "/tmp/destination"}); // GOOD
      inner.run();
    };
    nested.run();

    final String shell = "/bin/sh";
    Runnable bad = () -> new ProcessBuilder(new String[]{launcher, shell, "-c", arg}); // $ Alert[java/command-line-injection]
    bad.run();
  }

  public static void fixedLauncherCommand(String arg) throws java.io.IOException {
    new ProcessBuilder(new String[]{"/usr/bin/env", "/bin/cp", arg, "/tmp/destination"}).start(); // GOOD
  }

  public static void fixedLauncherList(String arg) throws java.io.IOException {
    List<String> good = new ArrayList<String>();
    good.add("/usr/bin/env");
    good.add("/bin/cp");
    good.add(arg);
    good.add("/tmp/destination");
    new ProcessBuilder(good).start(); // GOOD
  }

  public static void fixedLauncherArray(String arg) throws java.io.IOException {
    String[] goodArray = new String[]{"/usr/bin/env", "/bin/cp", "<arg>", "/tmp/destination"};
    goodArray[2] = arg;
    new ProcessBuilder(goodArray).start(); // GOOD
  }

  public static void assignedLauncherArray(String arg) throws java.io.IOException {
    String[] assignedArray = new String[4];
    assignedArray[0] = "/usr/bin/env";
    assignedArray[1] = "/bin/cp";
    assignedArray[2] = arg;
    assignedArray[3] = "/tmp/destination";
    new ProcessBuilder(assignedArray).start(); // GOOD
  }

  public static void fixedAliasedLauncherArray(String arg) throws java.io.IOException {
    String[] original = new String[]{"/usr/bin/env", "/bin/cp", "<arg>", "/tmp/destination"};
    String[] alias = original;
    alias[2] = arg;
    new ProcessBuilder(alias).start(); // GOOD
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
      launcherArguments(arg);
      partiallyKnownCommand(arg, "/bin/sh");
      capturedLauncherCommands(arg);
      fixedLauncherCommand(arg);
      fixedLauncherList(arg);
      fixedLauncherArray(arg);
      assignedLauncherArray(arg);
      fixedAliasedLauncherArray(arg);
      relativeCommand();
  }
}
