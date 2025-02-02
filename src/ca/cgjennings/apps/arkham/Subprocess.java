package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.ScriptConsole;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JUtilities;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A {@code Subprocess} is a child process that has been started by, and is
 * being monitored by, the application. The subprocess runs concurrently with
 * the application. The output and error streams of the subprocess are connected
 * to the script console.
 *
 * <p>While {@code Subprocess} can be used to run any shell command, it has
 * {@linkplain #launch special support} for starting Java apps, and in
 * particular for starting Java apps included with this app, such as the script
 * debugger client.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class Subprocess {

    /**
     * Creates a new Subprocess that will execute the
     * specified command.
     *
     * @param command an array of command tokens to execute
     * @throws NullPointerException if {@code command} is {@code null}
     * @throws IllegalArgumentException if {@code command.length == 0}
     * @throws IllegalStateException if the application window has not been
     * created yet
     */
    public Subprocess(String... command) {
        Objects.requireNonNull(command, "command");
        if (command.length < 1) {
            throw new IllegalArgumentException("empty command");
        }
        if (StrangeEons.getWindow() == null) {
            throw new IllegalStateException("console not avilable yet");
        }

        pb = new ProcessBuilder(command.clone());

        if(StrangeEons.log.isLoggable(Level.INFO)) {
            StrangeEons.log.info("created subprocess " + String.join(" ", command));
        }
    }

    /**
     * Creates a new Subprocess that will execute the
     * specified command.
     *
     * @param command an list of command tokens to execute
     * @throws NullPointerException if {@code command} is {@code null}
     * @throws IllegalArgumentException if {@code command.length == 0}
     * @throws IllegalStateException if the application window has not been
     * created yet
     */
    public Subprocess(List<String> command) {
        this(command.toArray(new String[Objects.requireNonNull(command.size(), "command")]));
    }

    /**
     * Returns a new Subprocess that will launch an app that is part of this
     * app's main library or JAR file. For example, {@code launch("debugger")}
     * would launch the debugger client by starting a new runtime with the
     * same classpath as this app and passing it the {@link debugger} class.
     *
     * <p>If necessary, additional or replacement JVM arguments may be
     * specified <em>before</em> the class name (which is otherwise the
     * first argument).
     *
     * @param appClassNameAndArguments the name of the app, followed by any
     *   additional command line arguments to pass to the app
     * @return the new Subprocess controlling the launched app
     * @throws IllegalArgumentException if argument list is null or empty
     * @throws IllegalStateException if the application window has not been
     *   created yet
     * @since 3.2
     */
    public static Subprocess launch(String... appClassNameAndArguments) {
        if(appClassNameAndArguments == null || appClassNameAndArguments.length == 0) {
            throw new IllegalArgumentException("no app class specified");
        }
        return launch(Arrays.asList(appClassNameAndArguments));
    }

    /**
     * Returns a new Subprocess that will launch an app that is part of this
     * app's main library or JAR file. For example, {@code launch("debugger")}
     * would launch the debugger client by starting a new runtime with the
     * same classpath as this app and passing it the {@link debugger} class.
     *
     * <p>If necessary, additional or replacement JVM arguments may be
     * specified <em>before</em> the class name (which is otherwise the
     * first argument).
     *
     * @param appClassNameAndArguments the name of the app, followed by any
     *   additional command line arguments to pass to the app
     * @return the new Subprocess controlling the launched app
     * @throws IllegalArgumentException if argument list is null or empty
     * @throws IllegalStateException if the application window has not been
     *   created yet
     * @since 3.2
     */
    public static Subprocess launch(List<String> appClassNameAndArguments) {
        if(appClassNameAndArguments == null || appClassNameAndArguments.isEmpty()) {
            throw new IllegalArgumentException("no app class specified");
        }
        LinkedList<String> tokens = new LinkedList<>();
        tokens.add(getJavaRuntimeExecutable());
        tokens.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        tokens.add("-cp");
        tokens.add(getClasspath());
        tokens.addAll(appClassNameAndArguments);
        return new Subprocess(tokens);
    }

    private ProcessBuilder pb;
    private ExecutionManager em;
    private JButton killBtn;
    private boolean redirectStreams = true;
    private volatile int state = 0;
    private volatile boolean survives;
    private volatile boolean showRetVal = true;
    private volatile int retVal = -1;

    private static final int NOTSTARTED = 0, FINISHED = 1, RUNNING = 2;

    /**
     * Returns {@code true} if this subprocess has been started. Once a
     * subprocess has started, its configuration cannot be modified. Once the
     * subprocess starts, this method will always return {@code true} even
     * after the subprocess ends.
     *
     * @return {@code true} if the process has started
     * @see #isRunning()
     */
    public synchronized boolean isStarted() {
        return state > NOTSTARTED;
    }

    /**
     * Returns {@code true} if this subprocess has been started and it is
     * still running.
     *
     * @return {@code true} if the process has started but not finished
     * @see #isStarted()
     */
    public synchronized boolean isRunning() {
        return state > FINISHED;
    }

    /**
     * Sets whether the subprocess will be a survivor. If {@code true},
     * then the process will not be destroyed when the application terminates.
     *
     * @param survivor {@code true} if the subprocess will survive the
     * application
     * @throws IllegalStateException if the subprocess has already been started
     */
    public synchronized void setSurvivor(boolean survivor) {
        if (isStarted()) {
            throw new IllegalStateException("already started");
        }
        survives = survivor;
    }

    /**
     * Returns {@code true} if this subprocess will continue after the
     * application terminates.
     *
     * @return {@code true} if the subprocess will survive the application
     */
    public synchronized boolean isSurvivor() {
        return survives;
    }

    /**
     * Sets whether the subprocess will redirect the standard
     * I/O streams to the script console. The default is to redirect streams.
     *
     * @param redirect {@code true} if the streams will be redirected
     * @throws IllegalStateException if the subprocess has already been started
     * @since 3.2
     */
    public synchronized void setStreamIORedirected(boolean redirect) {
        if (isStarted()) {
            throw new IllegalStateException("already started");
        }
        redirectStreams = redirect;
    }

    /**
     * Returns {@code true} if this subprocess will redirect the standard
     * I/O streams to the script console.
     *
     * @return {@code true} if the streams will be redirected
     * @since 3.2
     */
    public synchronized boolean isStreamIORedirected() {
        return redirectStreams;
    }

    /**
     * Sets whether the exit code is written to the console when the process
     * ends. The default is {@code true}.
     *
     * @param show if {@code true}, print the exit code
     */
    public synchronized void setExitCodeShown(boolean show) {
        showRetVal = show;
    }

    /**
     * Returns {@code true} if the exit code will be written to the console
     * when the process ends.
     *
     * @return {@code true} if the exit code is printed
     */
    public synchronized boolean isExitCodeShown() {
        return showRetVal;
    }

    /**
     * Creates a control in the main application window that the user can use to
     * terminate the subprocess. The control will remove itself automatically
     * when the subprocess terminates.
     *
     * @param label label text to include on the button; if {@code null}
     * the command name will be used
     * @return a control that can be used to stop the subprocess
     * @throws IllegalStateException if the subprocess has already been started
     */
    public synchronized JComponent createStopButton(String label) {
        JUtilities.threadAssert();
        if (isStarted()) {
            throw new IllegalStateException("already started");
        }
        if (killBtn != null) {
            if (label != null) {
                killBtn.setText(label);
            }
            return killBtn;
        }

        if (label == null) {
            label = pb.command().get(0);
            int lastPart = label.lastIndexOf(File.separatorChar);
            if (lastPart >= 0) {
                label = label.substring(lastPart + 1);
            }
        }

        JButton btn;
        btn = new JButton(label);
        btn.setIcon(ResourceKit.getIcon("ui/controls/close0.png"));
        btn.setRolloverEnabled(true);
        btn.setIcon(ResourceKit.getIcon("ui/controls/close1.png"));
        btn.setSelectedIcon(ResourceKit.getIcon("ui/controls/close2.png"));
        btn.setContentAreaFilled(false);
        btn.setHorizontalTextPosition(SwingConstants.LEADING);
        btn.addActionListener((ActionEvent e) -> {
            stop();
            killBtn.setEnabled(false);
        });

        killBtn = btn;
        return btn;
    }

    /**
     * Starts the subprocess.
     *
     * @throws IllegalStateException if the subprocess has already been started
     */
    public synchronized void start() {
        if (isStarted()) {
            throw new IllegalStateException("already started");
        }

        state = RUNNING;
        em = new ExecutionManager();
        em.start();
    }

    /**
     * Stops the process if it is currently running. Otherwise, this method has
     * no effect. In most cases, this thread will wait for the subprocess to
     * finish executing before it returns, but if the subprocess does not exit
     * for some reason, it will give up and return after a timeout period.
     */
    public synchronized void stop() {
        if (state != RUNNING) {
            return;
        }
        em.interrupt();
        try {
            em.join(10_000);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Waits for the subprocess to end.
     *
     * @throws InterruptedException if this thread was interrupted while waiting
     * for the subprocess
     */
    public synchronized void waitFor() throws InterruptedException {
        if (state != RUNNING) {
            return;
        }
        em.join();
    }

    /**
     * Returns the process's exit code, or -1 if the process has not completed.
     *
     * @return the exit code when the process finished
     */
    public synchronized int getExitCode() {
        return retVal;
    }

    /**
     * The execution manager is a thread that starts the subprocess and then
     * waits for it to complete. It also starts an IOStreamManager thread to
     * print subprocess I/O to the script console. If this thread is
     * interrupted, it will signal the subprocess to exit and interrupt the
     * IOStreamManager to signal it to stop as well.
     */
    private class ExecutionManager extends Thread {

        public ExecutionManager() {
            super("Subprocess [" + pb.command().get(0) + ']');
        }

        @Override
        public void run() {
            ScriptConsole sc = ScriptMonkey.getSharedConsole();
            if (!survives) {
                beginMonitoring(this);
            }
            try {
                if (!redirectStreams) {
                    pb.inheritIO();
                }
                Process proc = pb.start();
                Thread io = redirectStreams ? new IOStreamManager(proc) : null;
                if (io != null) io.start();
                if (killBtn != null) {
                    EventQueue.invokeLater(() -> {
                        StrangeEons.getWindow().addCustomComponent(killBtn);
                    });
                }
                try {
                    proc.waitFor();
                    retVal = proc.exitValue();
                    if (showRetVal) {
                        sc.getErrorWriter().printf(string("retval", retVal));
                    }
                } catch (InterruptedException ex) {
                    proc.destroy();
                    if (!fastShutdown) {
                        sc.getErrorWriter().println(string("killps"));
                    }
                }
                if (io != null) io.interrupt();
            } catch (IOException e) {
                e.printStackTrace(sc.getErrorWriter());
            } finally {
                state = FINISHED;
                if (!fastShutdown) {
                    if (!survives) {
                        endMonitoring(this);
                    }
                    if (killBtn != null) {
                        EventQueue.invokeLater(() -> {
                            StrangeEons.getWindow().removeCustomComponent(killBtn);
                        });
                    }
                }
            }
        }
    }

    /**
     * Pipes output from the subprocess stdout and stderr streams to the script
     * console. When the subprocess exits, it will interrupt this thread, at
     * which point this thread waits up to ten more seconds for the streams to
     * close before it stops itself.
     */
    private static class IOStreamManager extends Thread {

        private final Process proc;

        public IOStreamManager(Process proc) {
            super("Subprocess I/O pipe");
            setDaemon(true);
            this.proc = proc;
        }

        @Override
        public void run() {
            ScriptConsole sc = ScriptMonkey.getSharedConsole();
            Reader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            Reader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            try {
                // will be set to current time when subprocess exits, then we'll
                // keep piping for a while or until the streams reach EOF
                boolean outEOF = false, errEOF = false;
                long countdown = Long.MIN_VALUE;
                while (countdown == Long.MIN_VALUE || (System.currentTimeMillis() - countdown) < 10_000) {
                    if (interrupted()) {
                        countdown = System.currentTimeMillis();
                    }
                    try {
                        boolean piped = false;
                        if (stdout.ready()) {
                            outEOF = pipe(stdout, sc.getWriter());
                            piped = true;
                        }
                        if (stderr.ready()) {
                            errEOF = pipe(stderr, sc.getErrorWriter());
                            piped = true;
                        }
                        if (outEOF && errEOF) {
                            break;
                        }
                        try {
                            Thread.sleep(piped ? 10 : 100);
                        } catch (InterruptedException ie) {
                            countdown = System.currentTimeMillis();
                        }
                    } catch (IOException e) {
                        e.printStackTrace(sc.getErrorWriter());
                    }
                }
            } finally {
                try {
                    stdout.close();
                } catch (IOException ioe) {
                }
                try {
                    stderr.close();
                } catch (IOException ioe) {
                }
            }
            StrangeEons.log.fine("Subprocess I/O pipe thread stopped");
        }

        private boolean pipe(Reader in, Writer out) throws IOException {
            if (buff == null) {
                buff = new char[8_192];
            }
            int len = in.read(buff);
            if (len > 0) {
                out.write(buff, 0, len);
            }
            return len < 0;
        }

        private char[] buff;
    }

    private static synchronized void beginMonitoring(ExecutionManager em) {
        if (activeProcesses == null) {
            Runnable killProcessesExitTask = new Runnable() {
                @Override
                public void run() {
                    synchronized (Subprocess.class) {
                        fastShutdown = true;
                        if (activeProcesses == null) {
                            return;
                        }
                        for (ExecutionManager em : activeProcesses) {
                            em.interrupt();
                            try {
                                em.join(10_000);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }

                @Override
                public String toString() {
                    return "stopping any Subprocesses";
                }
            };
            StrangeEons.getApplication().addExitTask(killProcessesExitTask);
            activeProcesses = new LinkedHashSet<>(4);
        }
        activeProcesses.add(em);
    }

    private static synchronized void endMonitoring(ExecutionManager em) {
        if (activeProcesses == null) {
            return;
        }
        activeProcesses.remove(em);
    }

    private static volatile boolean fastShutdown = false;
    private static LinkedHashSet<ExecutionManager> activeProcesses;

    /**
     * Returns the application's class path.
     *
     * @return a list of one or more paths separated by the platform path separator
     */
    public static String getClasspath() {
        String cp = System.getProperty("java.class.path");
        if(cp == null) {
            StrangeEons.log.warning("no classpath property, using fallback");
            cp = BundleInstaller.getApplicationLibrary().getAbsolutePath();
        }
        return cp;
    }

    /**
     * Returns a command that can be used to launch a Java runtime.
     * Where possible, this will be the full path to the specific Java runtime
     * used to start this app. The path is normally detected correctly
     * automatically, but if for some reason detection fails, the user setting
     * {@code invoke-java-cmd} can be set to a suitable path.
     *
     * <p>For example, on a Windows device on which the app itself was launched
     * with a version of AdoptOpenJDK 8, this might return:
     *
     * <pre>C:\Program Files\AdoptOpenJDK\jdk-8.0.292.10-openj9\jre\bin\java.exe</pre>
     *
     * @return path to an executable that is expected to launch a separate JVM process
     */
    public static String getJavaRuntimeExecutable() {
        String command = Settings.getUser().get("invoke-java-cmd");
        if (command == null || command.equals("java")) {
            command = "java";
            String home = System.getProperty("java.home");
            if (home != null) {
                StringBuilder path = new StringBuilder(72);
                path.append(home).append(File.separatorChar)
                        .append("bin").append(File.separatorChar)
                        .append("java");
                if (PlatformSupport.PLATFORM_IS_WINDOWS) {
                    path.append(".exe");
                }
                File exe = new File(path.toString());
                if (exe.exists()) {
                    command = exe.getAbsolutePath();
                    StrangeEons.log.log(Level.FINE, "using platform-specific path: {0}", command);
                } else {
                    StrangeEons.log.log(Level.WARNING, "platform-specific path is invalid: {0}", command);
                }
            } else {
                StrangeEons.log.warning("java.home property is null");
            }
        }
        return command;
    }
}
