/**
 * Definitions for reasoning about lists and arrays that are to be used as arguments to an external process.
 */
overlay[local?]
module;

import java
import semmle.code.java.dataflow.SSA
import semmle.code.java.Collections

/**
 * Holds if `ex` is used safely as an argument to a command;
 * i.e. it's not in the first position and the command does not interpret arguments as commands.
 */
predicate isSafeCommandArgument(Expr ex) {
  exists(ArrayInit ai, int i |
    ex = ai.getInit(i) and
    i > 0 and
    not isShell(ai.getInit(0)) and
    (
      not isDelegatingLauncher(ai.getInit(0))
      or
      TLiteralArguments(ai).(CommandArguments).hasSafeArgument(i) and
      not exists(SsaExplicitWrite array, ArrayAccess access |
        arrayInitializerOf(array.getDefiningExpr()) = ai and
        access.getArray() = array.getARead() and
        arrayVarWrite(access) and
        (
          not access.getIndexExpr() instanceof CompileTimeConstantExpr
          or
          access.getIndexExpr().(CompileTimeConstantExpr).getIntValue() < i
        )
      )
    )
  )
  or
  exists(CommandArgumentList cal |
    not cal.isShell() and
    ex = cal.getASubsequentAdd().getArgument(0) and
    (
      not isDelegatingLauncher(cal.getAFirstAdd().getArgument(0))
      or
      exists(int i |
        ex = TListArguments(cal).(CommandArguments).getArgument(i) and
        TListArguments(cal).(CommandArguments).hasSafeArgument(i)
      )
    )
  )
  or
  exists(CommandArgArrayImmutableFirst caa |
    not caa.isShell() and
    exists(int i |
      i > 0 and
      ex = caa.getAWrite(i) and
      (
        not isDelegatingLauncher(caa.getFirstElement())
        or
        TMutableArguments(caa).(CommandArguments).hasSafeArgument(i)
      )
    )
  )
}

/**
 * Holds if the given expression is the name of a shell command such as bash or python
 */
private predicate isShell(Expr ex) {
  exists(string cmd | cmd = ex.(StringLiteral).getValue() |
    cmd.regexpMatch(".*(sh|javac?|python[23]?|osascript|cmd)(\\.exe)?$")
  )
  or
  exists(SsaDefinition ssa, SsaExplicitWrite def |
    ex = ssa.getARead() and
    isShell(def.getDefiningExpr())
  |
    ssa.getAnUltimateDefinition() = def
    or
    ssa.(SsaCapturedDefinition).getAnUltimateCapturedDefinition() = def
  )
  or
  isShell(ex.(Assignment).getRhs())
  or
  isShell(ex.(LocalVariableDeclExpr).getInit())
}

/**
 * Holds if the given expression is the name of a launcher that delegates to a command supplied in
 * one of its arguments, rather than interpreting that command itself like a shell.
 */
private predicate isDelegatingLauncher(Expr ex) {
  commandName(ex)
      .regexpMatch("(.*/)?(busybox|chroot|doas|env|flock|ionice|nice|nohup|runuser|script|setsid|stdbuf|su|sudo|taskset|time|timeout|unbuffer|watch|xargs)$")
}

private string commandName(Expr ex) {
  result = ex.(StringLiteral).getValue()
  or
  exists(SsaDefinition ssa, SsaExplicitWrite def |
    ex = ssa.getARead() and
    result = commandName(def.getDefiningExpr())
  |
    ssa.getAnUltimateDefinition() = def
    or
    ssa.(SsaCapturedDefinition).getAnUltimateCapturedDefinition() = def
  )
  or
  result = commandName(ex.(Assignment).getRhs())
  or
  result = commandName(ex.(LocalVariableDeclExpr).getInit())
}

private newtype TCommandArguments =
  TLiteralArguments(ArrayInit init) or
  TListArguments(CommandArgumentList list) or
  TMutableArguments(CommandArgArrayImmutableFirst array)

/** An ordered argument sequence whose fixed command prefixes can be inspected. */
private class CommandArguments extends TCommandArguments {
  string toString() { result = this.getArgument(0).toString() }

  Expr getArgument(int index) {
    exists(ArrayInit init | this = TLiteralArguments(init) | result = init.getInit(index))
    or
    exists(CommandArgumentList list |
      this = TListArguments(list) and
      forall(MethodCall add | add = list.getAnAdd() |
        add.getMethod().hasName("add") and add.getNumArgument() = 1
      ) and
      result = list.getAnAddAtIndex(index).getArgument(0)
    )
    or
    exists(CommandArgArrayImmutableFirst array |
      this = TMutableArguments(array) and
      (
        index = 0 and result = array.getFirstElement()
        or
        index > 0 and
        (
          result = array.getAWrite(index)
          or
          result = arrayInitializerOf(array.getDefiningExpr()).getInit(index)
        )
      )
    )
  }

  /** Gets a constant shared by every possible value at this position. */
  private string getFixedArgument(int index) {
    result = commandName(this.getArgument(index)) and
    forall(Expr value | value = this.getArgument(index) |
      commandName(value) = result and
      not exists(string other | commandName(value) = other and other != result)
    )
  }

  /**
   * Gets a command operand after skipping only understood options and required operands.
   * Shell-interpreting forms (such as `su -c`, `watch`, and `script -c`) are not skipped.
   */
  private int commandOperand(string launcher, int index, boolean options) {
    launcher = this.getFixedArgument(_).regexpCapture("(.*/)?([^/]+)", 2) and
    options = [true, false] and
    exists(Expr arg | arg = this.getArgument(index) |
      (options = false or not this.getFixedArgument(index).matches("-%")) and
      not (launcher = "env" and this.getFixedArgument(index).matches("%=%")) and
      result = index
      or
      options = true and
      this.getFixedArgument(index) = "--" and
      result = this.commandOperand(launcher, index + 1, false)
      or
      options = true and
      exists(int width |
        launcherOption(launcher, this.getFixedArgument(index), width) and
        (width = 1 or exists(this.getArgument(index + 1))) and
        result = this.commandOperand(launcher, index + width, true)
      )
      or
      launcher = "env" and
      this.getFixedArgument(index).matches("%=%") and
      result = this.commandOperand(launcher, index + 1, options)
    )
  }

  private int delegatedCommand(int index) {
    exists(string launcher |
      launcher = this.getFixedArgument(index).regexpCapture("(.*/)?([^/]+)", 2) and
      isDelegatingLauncher(this.getArgument(index)) and
      (
        exists(int start |
          launcher =
            [
              "busybox", "doas", "env", "ionice", "nice", "nohup", "setsid", "stdbuf", "sudo",
              "time", "unbuffer", "xargs"
            ] and
          start = index + 1
          or
          launcher = "runuser" and
          this.getFixedArgument(index + 1) = ["-u", "--user"] and
          start = index + 3
        |
          result = this.commandOperand(launcher, start, true)
        )
        or
        launcher = ["chroot", "flock", "taskset", "timeout"] and
        // Locate the required root, lock file, CPU mask, or duration first.
        result = this.commandOperand(launcher, index + 1, true) + 1 and
        not this.getFixedArgument(result).matches("-%")
      )
    )
  }

  private predicate safeAfter(int command, int argument) {
    command < argument and
    exists(this.getArgument(argument)) and
    (
      exists(this.getFixedArgument(command)) and
      not isShell(this.getArgument(command)) and
      not isDelegatingLauncher(this.getArgument(command))
      or
      this.safeAfter(this.delegatedCommand(command), argument)
    )
  }

  predicate hasSafeArgument(int index) { this.safeAfter(0, index) }
}

private ArrayInit arrayInitializerOf(Expr ex) {
  result = ex.(ArrayInit)
  or
  result = ex.(ArrayCreationExpr).getInit()
  or
  result = arrayInitializerOf(ex.(LocalVariableDeclExpr).getInit())
  or
  result = arrayInitializerOf(ex.(Assignment).getRhs())
}

/** The width of an understood launcher option, including a separate option operand. */
bindingset[launcher, option]
private predicate launcherOption(string launcher, string option, int width) {
  width = 1 and
  (
    launcher = "env" and option = ["-i", "--ignore-environment", "-0", "--null"]
    or
    launcher = "nice" and option.regexpMatch("-[0-9]+")
    or
    launcher = "setsid" and option = ["-c", "-f", "-w", "--ctty", "--fork", "--wait"]
    or
    launcher = "stdbuf" and option.regexpMatch("-[ioe].+")
    or
    launcher = "sudo" and option = ["-n", "-E", "-H", "--non-interactive"]
    or
    launcher = "doas" and option = ["-n", "-L"]
    or
    launcher = "time" and option = ["-p", "-v", "--portability", "--verbose"]
    or
    launcher = "taskset" and option = ["-a", "-c", "--all-tasks", "--cpu-list"]
    or
    launcher = "flock" and
    option = ["-s", "-x", "-n", "-o", "--shared", "--exclusive", "--nonblock", "--close"]
    or
    launcher = "timeout" and option = ["--foreground", "--preserve-status", "-v", "--verbose"]
    or
    launcher = "xargs" and option = ["-0", "-r", "-t", "--null", "--no-run-if-empty", "--verbose"]
  )
  or
  width = 2 and
  (
    launcher = "env" and option = ["-u", "--unset", "-C", "--chdir"]
    or
    launcher = "nice" and option = ["-n", "--adjustment"]
    or
    launcher = "ionice" and option = ["-c", "-n", "--class", "--classdata"]
    or
    launcher = "sudo" and option = ["-u", "-g", "--user", "--group"]
    or
    launcher = "doas" and option = ["-u"]
    or
    launcher = "time" and option = ["-o", "-f", "--output", "--format"]
    or
    launcher = "flock" and option = ["-w", "-E", "--timeout", "--conflict-exit-code"]
    or
    launcher = "timeout" and option = ["-s", "-k", "--signal", "--kill-after"]
    or
    launcher = "xargs" and
    option = ["-n", "-P", "-I", "-d", "--max-args", "--max-procs", "--replace", "--delimiter"]
  )
}

/**
 * A type that could be a list of strings. Includes raw `List` types.
 */
private class ListOfStringType extends CollectionType {
  ListOfStringType() {
    this.getSourceDeclaration().getASourceSupertype*().hasQualifiedName("java.util", "List") and
    this.getElementType().getADescendant() instanceof TypeString
  }
}

/**
 * A variable that could be used as a list of arguments to a command.
 */
private class CommandArgumentList extends SsaExplicitWrite {
  CommandArgumentList() {
    this.getSourceVariable().getType() instanceof ListOfStringType and
    forex(CollectionMutation ma | ma.getQualifier() = this.getARead() |
      ma.getMethod().getName().matches("add%")
    )
  }

  /** Gets a use of the variable for which the list could be empty. */
  private VarRead getAUseBeforeFirstAdd() {
    result = ssaGetAFirstUse(this)
    or
    exists(VarRead mid |
      mid = this.getAUseBeforeFirstAdd() and
      adjacentUseUse(mid, result) and
      not exists(MethodCall ma |
        mid = ma.getQualifier() and
        ma.getMethod().hasName("add")
      )
    )
  }

  /**
   * Gets an addition to this list, i.e. a call to an `add` or `addAll` method.
   */
  MethodCall getAnAdd() {
    result.getQualifier() = this.getARead() and
    result.getMethod().getName().matches("add%")
  }

  /** Gets an addition to this list which could be its first element. */
  MethodCall getAFirstAdd() {
    result = this.getAnAdd() and
    result.getQualifier() = this.getAUseBeforeFirstAdd()
  }

  /** Gets an addition to this list which is not the first element. */
  MethodCall getASubsequentAdd() {
    result = this.getAnAdd() and
    not result = this.getAFirstAdd()
  }

  /** Gets a single-element append in a straight-line prefix of additions. */
  MethodCall getAnAddAtIndex(int index) {
    result = this.getAnAdd() and
    result.getMethod().hasName("add") and
    result.getNumArgument() = 1 and
    index in [0 .. count(this.getAnAdd())] and
    (
      index = 0 and
      result = this.getAFirstAdd() and
      forall(MethodCall first | first = this.getAFirstAdd() | first = result)
      or
      exists(MethodCall previous |
        previous = this.getAnAddAtIndex(index - 1) and
        adjacentUseUse(previous.getQualifier(), result.getQualifier()) and
        forall(VarRead use | adjacentUseUse(use, result.getQualifier()) |
          use = previous.getQualifier()
        )
      )
    )
  }

  /** Holds if the first element of this list is a shell command. */
  predicate isShell() {
    exists(MethodCall ma | ma = this.getAFirstAdd() and isShell(ma.getArgument(0)))
  }
}

/**
 * The type `String[]`.
 */
private class ArrayOfStringType extends Array {
  ArrayOfStringType() { this.getElementType() instanceof TypeString }
}

private predicate arrayVarWrite(ArrayAccess acc) { exists(Assignment a | a.getDest() = acc) }

/**
 * A variable that could be an array of arguments to a command.
 */
private class CommandArgumentArray extends SsaExplicitWrite {
  CommandArgumentArray() {
    this.getSourceVariable().getType() instanceof ArrayOfStringType and
    forall(ArrayAccess a | a.getArray() = this.getARead() and arrayVarWrite(a) |
      a.getIndexExpr() instanceof CompileTimeConstantExpr
    )
  }

  /** Gets an expression that is written to the given index of this array at the given use. */
  Expr getAWrite(int index, VarRead use) {
    exists(Assignment a, ArrayAccess acc |
      acc.getArray() = use and
      use = this.getARead() and
      index = acc.getIndexExpr().(CompileTimeConstantExpr).getIntValue() and
      acc = a.getDest() and
      result = a.getRhs()
    )
  }

  /** Gets an expression that is written to the given index of this array. */
  Expr getAWrite(int index) { result = this.getAWrite(index, _) }
}

/**
 * A `CommandArgArray` whose element at index 0 is never written to, except possibly once to initialise it.
 */
private class CommandArgArrayImmutableFirst extends CommandArgumentArray {
  CommandArgArrayImmutableFirst() {
    (exists(this.getAWrite(0)) or exists(firstElementOf(this.getDefiningExpr()))) and
    forall(VarRead use | exists(this.getAWrite(0, use)) | use = ssaGetAFirstUse(this))
  }

  /** Gets the first element of this array. */
  Expr getFirstElement() {
    result = this.getAWrite(0)
    or
    not exists(this.getAWrite(0)) and
    result = firstElementOf(this.getDefiningExpr())
  }

  /** Holds if the first element of this array is a shell command. */
  predicate isShell() { isShell(this.getFirstElement()) }
}

/** Gets the first element of an imutable array of strings */
private Expr firstElementOf(Expr arr) {
  arr.getType() instanceof ArrayOfStringType and
  (
    result = firstElementOf(arr.(Assignment).getRhs())
    or
    result = firstElementOf(arr.(LocalVariableDeclExpr).getInit())
    or
    exists(CommandArgArrayImmutableFirst caa | arr = caa.getARead() |
      result = caa.getFirstElement()
    )
    or
    exists(MethodCall ma, Method m |
      arr = ma and
      ma.getMethod() = m and
      m.getDeclaringType().hasQualifiedName("java.util", "Arrays") and
      m.hasName("copyOf") and
      result = firstElementOf(ma.getArgument(0))
    )
    or
    exists(Field f |
      f.isStatic() and
      arr.(FieldRead).getField() = f and
      result = firstElementOf(f.getInitializer())
    )
    or
    result = arr.(ArrayInit).getInit(0)
    or
    result = arr.(ArrayCreationExpr).getInit().getInit(0)
  )
}
