package br.com.jaaschenbrenner.budgetai.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class WindowsLauncher {
    private static final String PANEL_URL = "http://127.0.0.1:8080";
    private static final String HEALTH_URL = PANEL_URL + "/api/system/ai-provider";
    private static final String DEFAULT_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning";

    private final JFrame frame = new JFrame("Budget AI - DIO Spring AI");
    private final JPasswordField nvidiaKeyField = new JPasswordField();
    private final JTextField modelField = new JTextField(DEFAULT_MODEL);
    private final JLabel statusLabel = new JLabel("Parado");
    private final JButton startButton = new JButton("Iniciar Budget AI");
    private final JButton openButton = new JButton("Abrir painel");
    private final JButton codexButton = new JButton("Login Codex");
    private final JButton stopButton = new JButton("Parar");

    private volatile Process serverProcess;
    private Path logFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new WindowsLauncher().show();
        });
    }

    private WindowsLauncher() {
        String envNvidia = System.getenv("NVIDIA_API_KEY");
        if (envNvidia != null && !envNvidia.isBlank()) {
            nvidiaKeyField.setText(envNvidia);
        }
        modelField.setEditable(false);
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 330));
        frame.setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 8, 18));
        form.add(new JLabel("NVIDIA NIM API Key - usada para texto, áudio e Tool Calling"));
        form.add(nvidiaKeyField);
        form.add(new JLabel("Modelo NVIDIA Omni (fixo)"));
        form.add(modelField);
        form.add(new JLabel("Uma única credencial NVIDIA. O launcher não grava a chave em arquivo."));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        actions.add(startButton);
        actions.add(openButton);
        actions.add(codexButton);
        actions.add(stopButton);

        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(4, 18, 18, 18));
        status.add(statusLabel, BorderLayout.CENTER);

        frame.add(form, BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        frame.add(status, BorderLayout.NORTH);

        startButton.addActionListener(e -> startAsync());
        openButton.addActionListener(e -> openPanel());
        codexButton.addActionListener(e -> launchCodex());
        stopButton.addActionListener(e -> stopServer());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServer();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    private void startAsync() {
        if (serverProcess != null && serverProcess.isAlive()) {
            return;
        }

        String nvidiaKey = new String(nvidiaKeyField.getPassword()).trim();
        if (nvidiaKey.isBlank()) {
            JOptionPane.showMessageDialog(frame,
                    "Informe sua NVIDIA API Key. A mesma chave será usada para texto, áudio e Tool Calling.",
                    "NVIDIA API Key necessária",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStartingState();
        Thread worker = new Thread(() -> startServer(nvidiaKey), "budget-ai-starter");
        worker.setDaemon(true);
        worker.start();
    }

    private void startServer(String nvidiaKey) {
        try {
            Path imageRoot = Path.of(System.getProperty("java.home")).toAbsolutePath().getParent();
            if (imageRoot == null) {
                throw new IllegalStateException("Não foi possível localizar o runtime embutido.");
            }

            Path appJar = imageRoot.resolve("app").resolve("budget-ai.jar");
            Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");
            if (!Files.isRegularFile(appJar)) {
                throw new IllegalStateException("Aplicação Spring Boot não encontrada: " + appJar);
            }
            if (!Files.isRegularFile(javaExe)) {
                throw new IllegalStateException("Java embutido não encontrado: " + javaExe);
            }

            Path dataDir = resolveDataDir();
            Path logsDir = dataDir.resolve("logs");
            Files.createDirectories(logsDir);
            Files.createDirectories(dataDir.resolve("data"));
            logFile = logsDir.resolve("budget-ai.log");

            ProcessBuilder builder = new ProcessBuilder(javaExe.toString(), "-jar", appJar.toString());
            builder.directory(dataDir.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Map<String, String> env = builder.environment();
            env.put("NVIDIA_API_KEY", nvidiaKey);
            env.put("NVIDIA_MODEL", DEFAULT_MODEL);
            env.put("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com");
            env.remove("OPENAI_API_KEY");

            serverProcess = builder.start();
            appendLauncherLog("Launcher iniciou o backend em " + LocalDateTime.now());

            if (!waitUntilReady(serverProcess, 75)) {
                if (!serverProcess.isAlive()) {
                    throw new IllegalStateException("O backend encerrou durante a inicialização. Consulte: " + logFile);
                }
                throw new IllegalStateException("O backend não respondeu em 75 segundos. Consulte: " + logFile);
            }

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Rodando • NVIDIA Omni • texto + áudio + tools • " + PANEL_URL);
                startButton.setEnabled(false);
                openButton.setEnabled(true);
                stopButton.setEnabled(true);
                nvidiaKeyField.setEnabled(false);
            });
            openPanel();

            int exit = serverProcess.waitFor();
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Backend encerrado (código " + exit + ")");
                resetStoppedState();
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Falha ao iniciar");
                resetStoppedState();
                JOptionPane.showMessageDialog(frame,
                        ex.getMessage() + (logFile == null ? "" : "\n\nLog: " + logFile),
                        "Falha ao iniciar Budget AI",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private boolean waitUntilReady(Process process, int timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline && process.isAlive()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                connection.disconnect();
                if (code >= 200 && code < 500) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void openPanel() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(PANEL_URL));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Abra manualmente: " + PANEL_URL,
                    "Painel Budget AI",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void launchCodex() {
        Thread worker = new Thread(() -> {
            try {
                String command =
                        "$ErrorActionPreference='Stop'; " +
                        "$native=Join-Path $env:LOCALAPPDATA 'Programs\\OpenAI\\Codex\\bin\\codex.exe'; " +
                        "$cmd=Get-Command codex -ErrorAction SilentlyContinue; " +
                        "if($cmd){ & $cmd.Source } " +
                        "elseif(Test-Path $native){ & $native } " +
                        "else { " +
                        "Write-Host 'Codex CLI não encontrado. Instalando pelo instalador oficial da OpenAI...' -ForegroundColor Cyan; " +
                        "irm https://chatgpt.com/codex/install.ps1 | iex; " +
                        "$cmd=Get-Command codex -ErrorAction SilentlyContinue; " +
                        "if($cmd){ & $cmd.Source } elseif(Test-Path $native){ & $native } " +
                        "else { throw 'Codex foi instalado, mas o executável não foi localizado.' } " +
                        "}";

                new ProcessBuilder(
                        "powershell.exe",
                        "-NoExit",
                        "-NoProfile",
                        "-ExecutionPolicy", "Bypass",
                        "-Command", command)
                        .start();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Não foi possível abrir/instalar o Codex: " + ex.getMessage(),
                        "Codex",
                        JOptionPane.ERROR_MESSAGE));
            }
        }, "codex-login");
        worker.setDaemon(true);
        worker.start();
    }

    private void stopServer() {
        Process process = serverProcess;
        if (process == null || !process.isAlive()) {
            resetStoppedState();
            return;
        }
        statusLabel.setText("Encerrando backend...");
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        resetStoppedState();
        statusLabel.setText("Parado");
    }

    private void setStartingState() {
        statusLabel.setText("Iniciando Spring Boot...");
        startButton.setEnabled(false);
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        nvidiaKeyField.setEnabled(false);
    }

    private void resetStoppedState() {
        serverProcess = null;
        startButton.setEnabled(true);
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        nvidiaKeyField.setEnabled(true);
    }

    private Path resolveDataDir() throws Exception {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dir = localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData, "BudgetAI")
                : Path.of(System.getProperty("user.home"), ".budgetai");
        Files.createDirectories(dir);
        return dir;
    }

    private void appendLauncherLog(String line) {
        if (logFile == null) {
            return;
        }
        try {
            Files.writeString(logFile, System.lineSeparator() + line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
}
