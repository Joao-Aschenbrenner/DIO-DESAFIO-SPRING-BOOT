package br.com.jaaschenbrenner.budgetai.launcher;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class WindowsLauncherDelivery {
    private static final String PANEL_URL = "http://127.0.0.1:8080";
    private static final String HEALTH_URL = PANEL_URL + "/api/system/ai-provider";
    private static final String DEFAULT_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning";

    private final JFrame frame = new JFrame("Budget AI - DIO Spring AI");
    private final JPasswordField nvidiaKeyField = new JPasswordField();
    private final JPasswordField ttsKeyField = new JPasswordField();
    private final JCheckBox showAdvanced = new JCheckBox("Configurar voz MP3 via Spring AI (opcional)");
    private final JPanel ttsPanel = new JPanel();
    private final JLabel statusLabel = new JLabel("Pronto para iniciar");
    private final JButton startButton = new JButton("Iniciar Budget AI");
    private final JButton openButton = new JButton("Abrir painel");
    private final JButton logButton = new JButton("Abrir log");
    private final JButton stopButton = new JButton("Parar");

    private volatile Process serverProcess;
    private Path logFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // O look-and-feel padrão continua disponível.
            }
            new WindowsLauncherDelivery().show();
        });
    }

    private WindowsLauncherDelivery() {
        String envNvidia = System.getenv("NVIDIA_API_KEY");
        if (envNvidia != null && !envNvidia.isBlank()) {
            nvidiaKeyField.setText(envNvidia);
        }
        String envTts = System.getenv("BUDGETAI_TTS_API_KEY");
        if (envTts != null && !envTts.isBlank()) {
            ttsKeyField.setText(envTts);
            showAdvanced.setSelected(true);
        }
        try {
            logFile = resolveDataDir().resolve("logs").resolve("budget-ai.log");
        } catch (IOException ignored) {
            // Recriado ao iniciar.
        }
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(700, 430));
        frame.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 20));

        JLabel title = new JLabel("Budget AI - Assistente Financeiro com Spring AI");
        title.setFont(title.getFont().deriveFont(16f));
        content.add(title);
        content.add(spacer());
        content.add(new JLabel("NVIDIA API Key (obrigatória para IA, Tool Calling e áudio de entrada)"));
        nvidiaKeyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        content.add(nvidiaKeyField);
        content.add(spacer());
        content.add(new JLabel("Modelo: " + DEFAULT_MODEL));
        content.add(spacer());
        content.add(showAdvanced);

        ttsPanel.setLayout(new BoxLayout(ttsPanel, BoxLayout.Y_AXIS));
        ttsPanel.setBorder(BorderFactory.createEmptyBorder(6, 16, 0, 0));
        ttsPanel.add(new JLabel("Chave OpenAI para TextToSpeechModel/MP3 (opcional)"));
        ttsKeyField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        ttsPanel.add(ttsKeyField);
        ttsPanel.add(new JLabel("Se ficar vazio, o painel usa a voz local do navegador como fallback."));
        ttsPanel.setVisible(showAdvanced.isSelected());
        content.add(ttsPanel);
        content.add(spacer());
        content.add(new JLabel("As chaves ficam somente na memória dos processos e não são gravadas pelo launcher."));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        actions.add(startButton);
        actions.add(openButton);
        actions.add(logButton);
        actions.add(stopButton);

        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(8, 20, 16, 20));
        status.add(statusLabel, BorderLayout.CENTER);

        frame.add(content, BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        frame.add(status, BorderLayout.NORTH);

        showAdvanced.addActionListener(e -> {
            ttsPanel.setVisible(showAdvanced.isSelected());
            frame.pack();
        });
        startButton.addActionListener(e -> startAsync());
        openButton.addActionListener(e -> openPanel());
        logButton.addActionListener(e -> openLog());
        stopButton.addActionListener(e -> stopServer());
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                stopServer();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel spacer() {
        JPanel panel = new JPanel();
        panel.setMaximumSize(new Dimension(1, 10));
        panel.setPreferredSize(new Dimension(1, 10));
        return panel;
    }

    private void startAsync() {
        if (serverProcess != null && serverProcess.isAlive()) {
            return;
        }
        String nvidiaKey = new String(nvidiaKeyField.getPassword()).trim();
        String ttsKey = showAdvanced.isSelected() ? new String(ttsKeyField.getPassword()).trim() : "";
        if (nvidiaKey.isBlank()) {
            JOptionPane.showMessageDialog(frame,
                    "Informe sua NVIDIA API Key para usar texto, áudio e Tool Calling.",
                    "NVIDIA API Key necessária", JOptionPane.WARNING_MESSAGE);
            return;
        }

        startButton.setEnabled(false);
        nvidiaKeyField.setEnabled(false);
        ttsKeyField.setEnabled(false);
        showAdvanced.setEnabled(false);
        statusLabel.setText("Iniciando backend Spring Boot…");

        Thread worker = new Thread(() -> startServer(nvidiaKey, ttsKey), "budget-ai-starter");
        worker.setDaemon(true);
        worker.start();
    }

    private void startServer(String nvidiaKey, String ttsKey) {
        try {
            RuntimePaths paths = resolveRuntimePaths();
            Path dataDir = resolveDataDir();
            Path logsDir = dataDir.resolve("logs");
            Files.createDirectories(logsDir);
            Files.createDirectories(dataDir.resolve("data"));
            logFile = logsDir.resolve("budget-ai.log");

            ProcessBuilder builder = new ProcessBuilder(paths.javaExe().toString(), "-jar", paths.appJar().toString());
            builder.directory(dataDir.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Map<String, String> env = builder.environment();
            env.put("NVIDIA_API_KEY", nvidiaKey);
            env.put("NVIDIA_MODEL", DEFAULT_MODEL);
            env.put("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com");
            if (!ttsKey.isBlank()) {
                env.put("BUDGETAI_TTS_API_KEY", ttsKey);
            } else {
                env.remove("BUDGETAI_TTS_API_KEY");
            }

            appendLauncherLog("Launcher v0.3.5 iniciou o backend em " + LocalDateTime.now());
            serverProcess = builder.start();

            if (!waitUntilReady(serverProcess, 75)) {
                if (!serverProcess.isAlive()) {
                    throw new BackendStartupException(readStartupDiagnostic());
                }
                throw new BackendStartupException("O backend não respondeu em 75 segundos. Verifique a porta 8080 e o log.");
            }

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Rodando • painel disponível em " + PANEL_URL);
                openButton.setEnabled(true);
                stopButton.setEnabled(true);
            });
            openPanel();

            int exit = serverProcess.waitFor();
            appendLauncherLog("Backend encerrado com código " + exit);
            SwingUtilities.invokeLater(() -> resetStoppedState("Backend encerrado (código " + exit + ")"));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            reportFailure("Inicialização interrompida", "A inicialização foi interrompida.", ex);
        } catch (Exception ex) {
            reportFailure("Falha ao iniciar o Budget AI", safeMessage(ex), ex);
        }
    }

    private RuntimePaths resolveRuntimePaths() {
        Path javaHome = Path.of(System.getProperty("java.home")).toAbsolutePath();
        Path imageRoot = javaHome.getParent();
        if (imageRoot == null) {
            throw new BackendStartupException("Não foi possível localizar o runtime Java embutido.");
        }
        Path javaExe = javaHome.resolve("bin").resolve("java.exe");
        Path appJar = imageRoot.resolve("app").resolve("budget-ai.jar");
        if (!Files.isRegularFile(javaExe)) throw new BackendStartupException("Java embutido não encontrado: " + javaExe);
        if (!Files.isRegularFile(appJar)) throw new BackendStartupException("Aplicação Spring Boot não encontrada: " + appJar);
        return new RuntimePaths(appJar, javaExe);
    }

    private Path resolveDataDir() throws IOException {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                : Path.of(localAppData);
        Path dir = base.resolve("BudgetAI");
        Files.createDirectories(dir);
        return dir;
    }

    private boolean waitUntilReady(Process process, int timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline && process.isAlive()) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code >= 200 && code < 500) return true;
            } catch (IOException ignored) {
                // Normal durante o boot.
            } finally {
                if (connection != null) connection.disconnect();
            }
            try {
                Thread.sleep(900);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void openPanel() {
        try {
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop API indisponível.");
            Desktop.getDesktop().browse(URI.create(PANEL_URL));
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Abra manualmente no navegador:\n" + PANEL_URL,
                    "Painel Budget AI", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    private void openLog() {
        try {
            if (logFile == null) logFile = resolveDataDir().resolve("logs").resolve("budget-ai.log");
            Files.createDirectories(logFile.getParent());
            if (!Files.exists(logFile)) Files.writeString(logFile, "Log ainda não criado.\n", StandardOpenOption.CREATE);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(logFile.toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Não foi possível abrir o log:\n" + safeMessage(ex),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        Process process = serverProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(8, TimeUnit.SECONDS)) process.destroyForcibly();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        serverProcess = null;
        resetStoppedState("Parado");
    }

    private void resetStoppedState(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            startButton.setEnabled(true);
            openButton.setEnabled(false);
            stopButton.setEnabled(false);
            nvidiaKeyField.setEnabled(true);
            ttsKeyField.setEnabled(true);
            showAdvanced.setEnabled(true);
        });
    }

    private void reportFailure(String title, String message, Exception ex) {
        appendLauncherLog(title + ": " + ex.getClass().getSimpleName() + " - " + safeMessage(ex));
        SwingUtilities.invokeLater(() -> {
            resetStoppedState("Falha ao iniciar");
            JOptionPane.showMessageDialog(frame,
                    message + "\n\nUse 'Abrir log' para ver os detalhes técnicos.",
                    title, JOptionPane.ERROR_MESSAGE);
        });
    }

    private String readStartupDiagnostic() {
        if (logFile == null || !Files.isRegularFile(logFile)) return "O backend encerrou e nenhum log foi encontrado.";
        try {
            List<String> lines = Files.readAllLines(logFile);
            int start = Math.max(0, lines.size() - 30);
            return String.join("\n", lines.subList(start, lines.size()));
        } catch (IOException ex) {
            return "Não foi possível ler o log de inicialização: " + safeMessage(ex);
        }
    }

    private void appendLauncherLog(String line) {
        try {
            if (logFile == null) logFile = resolveDataDir().resolve("logs").resolve("budget-ai.log");
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, "[launcher] " + line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Nunca impede o usuário de iniciar o aplicativo por falha de log.
        }
    }

    private String safeMessage(Throwable ex) {
        String message = ex == null ? null : ex.getMessage();
        return message == null || message.isBlank() ? "Erro sem mensagem adicional." : message;
    }

    private record RuntimePaths(Path appJar, Path javaExe) {}

    private static final class BackendStartupException extends RuntimeException {
        private BackendStartupException(String message) { super(message); }
    }
}
