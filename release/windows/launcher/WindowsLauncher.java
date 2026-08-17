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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class WindowsLauncher {
    private static final String PANEL_URL = "http://127.0.0.1:8080";
    private static final String HEALTH_URL = PANEL_URL + "/api/system/ai-provider";
    private static final String DEFAULT_MODEL = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning";

    private final JFrame frame = new JFrame("Budget AI - DIO Spring AI");
    private final JPasswordField nvidiaKeyField = new JPasswordField();
    private final JPasswordField openAiTtsKeyField = new JPasswordField();
    private final JTextField modelField = new JTextField(DEFAULT_MODEL);
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
                // O look-and-feel padrão do Java continua funcionando.
            }
            new WindowsLauncher().show();
        });
    }

    private WindowsLauncher() {
        String envNvidia = System.getenv("NVIDIA_API_KEY");
        if (envNvidia != null && !envNvidia.isBlank()) {
            nvidiaKeyField.setText(envNvidia);
        }

        String envTts = System.getenv("OPENAI_TTS_API_KEY");
        if (envTts != null && !envTts.isBlank()) {
            openAiTtsKeyField.setText(envTts);
        }

        modelField.setEditable(false);
        try {
            logFile = resolveDataDir().resolve("logs").resolve("budget-ai.log");
        } catch (IOException ignored) {
            // O caminho será recriado ao iniciar o backend.
        }
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 430));
        frame.setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(0, 1, 7, 7));
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 8, 18));

        form.add(new JLabel("NVIDIA NIM API Key (obrigatória) — texto, Tool Calling e áudio de entrada"));
        form.add(nvidiaKeyField);

        form.add(new JLabel("OpenAI API Key para TTS Spring AI / MP3 (opcional)"));
        form.add(openAiTtsKeyField);

        form.add(new JLabel("Modelo NVIDIA Omni"));
        form.add(modelField);

        form.add(new JLabel(
                "Sem a chave opcional de TTS, o painel usa a voz local do navegador. Nenhuma chave é gravada em arquivo."));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        actions.add(startButton);
        actions.add(openButton);
        actions.add(logButton);
        actions.add(stopButton);

        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(4, 18, 18, 18));
        status.add(statusLabel, BorderLayout.CENTER);

        frame.add(form, BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);
        frame.add(status, BorderLayout.NORTH);

        startButton.addActionListener(e -> startAsync());
        openButton.addActionListener(e -> openPanel());
        logButton.addActionListener(e -> openLog());
        stopButton.addActionListener(e -> stopAsync());

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
        Process current = serverProcess;
        if (current != null && current.isAlive()) {
            return;
        }

        String nvidiaKey = new String(nvidiaKeyField.getPassword()).trim();
        String openAiTtsKey = new String(openAiTtsKeyField.getPassword()).trim();

        if (nvidiaKey.isBlank()) {
            showWarning(
                    "NVIDIA API Key necessária",
                    "Informe sua NVIDIA API Key. Ela é usada para texto, áudio de entrada e Tool Calling.");
            return;
        }

        setStartingState();
        Thread worker = new Thread(
                () -> startServer(nvidiaKey, openAiTtsKey),
                "budget-ai-starter");
        worker.setDaemon(true);
        worker.start();
    }

    private void startServer(String nvidiaKey, String openAiTtsKey) {
        try {
            RuntimePaths paths = resolveRuntimePaths();
            Path dataDir = resolveDataDir();
            Path logsDir = dataDir.resolve("logs");
            Files.createDirectories(logsDir);
            Files.createDirectories(dataDir.resolve("data"));
            logFile = logsDir.resolve("budget-ai.log");

            ProcessBuilder builder = new ProcessBuilder(
                    paths.javaExe().toString(),
                    "-jar",
                    paths.appJar().toString());
            builder.directory(dataDir.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Map<String, String> env = builder.environment();
            env.put("NVIDIA_API_KEY", nvidiaKey);
            env.put("NVIDIA_MODEL", DEFAULT_MODEL);
            env.put("NVIDIA_BASE_URL", "https://integrate.api.nvidia.com");

            // Evita que uma OPENAI_API_KEY global altere acidentalmente o ChatModel NVIDIA.
            env.remove("OPENAI_API_KEY");

            if (openAiTtsKey.isBlank()) {
                env.remove("OPENAI_TTS_API_KEY");
                env.put("BUDGETAI_TTS_PROVIDER", "none");
            } else {
                env.put("OPENAI_TTS_API_KEY", openAiTtsKey);
                env.put("BUDGETAI_TTS_PROVIDER", "openai");
            }

            appendLauncherLog(
                    "Launcher iniciou o backend em " + LocalDateTime.now()
                            + " (TTS Spring AI: " + (openAiTtsKey.isBlank() ? "desativado" : "ativado") + ")");

            Process started = builder.start();
            serverProcess = started;

            if (!waitUntilReady(started, 75)) {
                if (!started.isAlive()) {
                    throw new BackendStartupException(readStartupDiagnostic());
                }
                throw new BackendStartupException(
                        "O backend iniciou, mas não respondeu em 75 segundos. Verifique se a porta 8080 está livre.");
            }

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(
                        "Rodando • NVIDIA Omni • TTS "
                                + (openAiTtsKey.isBlank() ? "local no navegador" : "Spring AI + fallback local")
                                + " • " + PANEL_URL);
                startButton.setEnabled(false);
                openButton.setEnabled(true);
                stopButton.setEnabled(true);
                nvidiaKeyField.setEnabled(false);
                openAiTtsKeyField.setEnabled(false);
            });

            openPanel();

            int exit = started.waitFor();
            if (exit != 0 && serverProcess == started) {
                appendLauncherLog("Backend encerrou inesperadamente com código " + exit);
            }

            SwingUtilities.invokeLater(() -> {
                if (serverProcess == started) {
                    statusLabel.setText("Backend encerrado (código " + exit + ")");
                    resetStoppedState();
                }
            });
        } catch (BackendStartupException ex) {
            terminateBackendAfterStartupFailure();
            reportFailure("Falha ao iniciar o backend", ex.getMessage(), ex);
        } catch (IOException ex) {
            terminateBackendAfterStartupFailure();
            reportFailure(
                    "Falha de leitura/gravação",
                    "O Budget AI não conseguiu acessar os arquivos necessários.\n" + safeMessage(ex),
                    ex);
        } catch (SecurityException ex) {
            terminateBackendAfterStartupFailure();
            reportFailure(
                    "Permissão negada",
                    "O Windows bloqueou o acesso a um arquivo ou processo necessário.\n" + safeMessage(ex),
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            terminateBackendAfterStartupFailure();
            reportFailure("Inicialização interrompida", "A inicialização do backend foi interrompida.", ex);
        } catch (Exception ex) {
            terminateBackendAfterStartupFailure();
            reportFailure(
                    "Erro inesperado no launcher",
                    "O launcher encontrou um erro não previsto. Consulte o log para detalhes.\n" + safeMessage(ex),
                    ex);
        }
    }

    private RuntimePaths resolveRuntimePaths() {
        Path imageRoot = Path.of(System.getProperty("java.home")).toAbsolutePath().getParent();
        if (imageRoot == null) {
            throw new BackendStartupException("Não foi possível localizar o runtime Java embutido.");
        }

        Path appJar = imageRoot.resolve("app").resolve("budget-ai.jar");
        Path javaExe = Path.of(System.getProperty("java.home"), "bin", "java.exe");

        if (!Files.isRegularFile(appJar)) {
            throw new BackendStartupException("Aplicação Spring Boot não encontrada: " + appJar);
        }
        if (!Files.isRegularFile(javaExe)) {
            throw new BackendStartupException("Java 21 embutido não encontrado: " + javaExe);
        }

        return new RuntimePaths(appJar, javaExe);
    }

    private boolean waitUntilReady(Process process, int timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);

        while (System.nanoTime() < deadline && process.isAlive()) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create(HEALTH_URL).toURL().openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    return true;
                }
            } catch (IOException ignored) {
                // Durante o boot é normal a porta ainda não aceitar conexão.
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    private String readStartupDiagnostic() {
        if (logFile == null || !Files.isRegularFile(logFile)) {
            return "O backend encerrou durante a inicialização e nenhum log foi encontrado.";
        }

        try {
            List<String> lines = Files.readAllLines(logFile);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);

                if (line.contains("OpenAI API key must be set")) {
                    return "A configuração do TTS Spring AI exige uma chave OpenAI válida. "
                            + "Deixe o campo de TTS vazio para usar somente a voz local do navegador.";
                }
                if (line.contains("Port 8080") && line.toLowerCase().contains("use")) {
                    return "A porta 8080 já está sendo usada por outro programa. "
                            + "Feche o processo que usa a porta e tente novamente.";
                }
                if (line.contains("BindException") || line.contains("Address already in use")) {
                    return "Não foi possível abrir a porta 8080 porque ela já está em uso.";
                }
                if (line.contains("JdbcSQLNonTransientConnectionException")
                        || line.contains("Database may be already in use")) {
                    return "O banco local está em uso por outra instância do Budget AI. "
                            + "Feche a outra instância e tente novamente.";
                }
                if (line.contains("Caused by:")) {
                    return "O Spring Boot falhou ao iniciar. Motivo encontrado no log:\n" + trim(line, 700);
                }
            }

            return "O backend encerrou durante a inicialização. Últimas linhas úteis:\n" + logTail(lines, 8);
        } catch (IOException ex) {
            return "O backend encerrou e o launcher não conseguiu ler o log: " + safeMessage(ex);
        }
    }

    private String logTail(List<String> lines, int count) {
        int start = Math.max(0, lines.size() - count);
        StringBuilder out = new StringBuilder();

        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isBlank()) {
                if (!out.isEmpty()) {
                    out.append('\n');
                }
                out.append(trim(line, 300));
            }
        }

        return out.toString();
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private void openPanel() {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException("Desktop API indisponível");
            }
            Desktop.getDesktop().browse(URI.create(PANEL_URL));
        } catch (Exception ex) {
            showInfo(
                    "Painel Budget AI",
                    "Não consegui abrir o navegador automaticamente.\nAbra manualmente: " + PANEL_URL);
        }
    }

    private void openLog() {
        try {
            if (logFile == null || !Files.isRegularFile(logFile)) {
                showInfo("Log Budget AI", "O arquivo de log ainda não existe.");
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                showInfo("Log Budget AI", "Log: " + logFile);
                return;
            }
            Desktop.getDesktop().open(logFile.toFile());
        } catch (Exception ex) {
            showError("Não foi possível abrir o log", "Log: " + logFile + "\n" + safeMessage(ex));
        }
    }

    private void stopAsync() {
        stopButton.setEnabled(false);
        statusLabel.setText("Encerrando backend...");

        Thread worker = new Thread(this::stopServer, "budget-ai-stopper");
        worker.setDaemon(true);
        worker.start();
    }

    private void stopServer() {
        Process process = serverProcess;
        if (process == null || !process.isAlive()) {
            SwingUtilities.invokeLater(() -> {
                resetStoppedState();
                statusLabel.setText("Parado");
            });
            return;
        }

        try {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            appendLauncherLog("Encerramento foi interrompido: " + safeMessage(ex));
        } catch (Exception ex) {
            appendLauncherLog("Falha ao encerrar backend: " + safeMessage(ex));
        } finally {
            if (serverProcess == process) {
                serverProcess = null;
            }
            SwingUtilities.invokeLater(() -> {
                resetStoppedState();
                statusLabel.setText("Parado");
            });
        }
    }

    private void terminateBackendAfterStartupFailure() {
        Process process = serverProcess;
        serverProcess = null;
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            } catch (Exception ex) {
                process.destroyForcibly();
            }
        }
    }

    private void reportFailure(String title, String message, Throwable ex) {
        appendLauncherLog(title + ": " + safeMessage(ex));

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Falha ao iniciar");
            resetStoppedState();
            showError(title, message + (logFile == null ? "" : "\n\nLog: " + logFile));
        });
    }

    private void setStartingState() {
        statusLabel.setText("Iniciando Spring Boot...");
        startButton.setEnabled(false);
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        nvidiaKeyField.setEnabled(false);
        openAiTtsKeyField.setEnabled(false);
    }

    private void resetStoppedState() {
        Process process = serverProcess;
        if (process == null || !process.isAlive()) {
            serverProcess = null;
        }
        startButton.setEnabled(true);
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        nvidiaKeyField.setEnabled(true);
        openAiTtsKeyField.setEnabled(true);
    }

    private Path resolveDataDir() throws IOException {
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
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                    logFile,
                    System.lineSeparator() + "[Launcher] " + line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Falha ao gravar log do launcher: " + ex.getMessage());
        }
    }

    private String safeMessage(Throwable ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex == null ? "erro desconhecido" : ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private record RuntimePaths(Path appJar, Path javaExe) {}

    private static final class BackendStartupException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private BackendStartupException(String message) {
            super(message);
        }
    }
}
