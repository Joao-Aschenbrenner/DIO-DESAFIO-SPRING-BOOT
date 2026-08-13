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
import java.net.URL;
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
    private final JTextField modelField = new JTextField(DEFAULT_MODEL);
    private final JLabel statusLabel = new JLabel("Parado");
    private final JButton startButton = new JButton("Iniciar Budget AI");
    private final JButton openButton = new JButton("Abrir painel");
    private final JButton codexButton = new JButton("Login Codex");
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
        modelField.setEditable(false);
        try {
            logFile = resolveDataDir().resolve("logs").resolve("budget-ai.log");
        } catch (IOException ignored) {
            // O caminho será recriado ao iniciar o backend.
        }
    }

    private void show() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(790, 350));
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
        codexButton.addActionListener(e -> launchCodex());
        logButton.addActionListener(e -> openLog());
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
            showWarning("NVIDIA API Key necessária",
                    "Informe sua NVIDIA API Key. A mesma chave será usada para texto, áudio e Tool Calling.");
            return;
        }

        setStartingState();
        Thread worker = new Thread(() -> startServer(nvidiaKey), "budget-ai-starter");
        worker.setDaemon(true);
        worker.start();
    }

    private void startServer(String nvidiaKey) {
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
            env.remove("OPENAI_API_KEY");

            appendLauncherLog("Launcher iniciou o backend em " + LocalDateTime.now());
            serverProcess = builder.start();

            if (!waitUntilReady(serverProcess, 75)) {
                if (!serverProcess.isAlive()) {
                    throw new BackendStartupException(readStartupDiagnostic());
                }
                throw new BackendStartupException(
                        "O backend iniciou, mas não respondeu em 75 segundos. Verifique se a porta 8080 está livre.");
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
            if (exit != 0) {
                appendLauncherLog("Backend encerrou inesperadamente com código " + exit);
            }
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Backend encerrado (código " + exit + ")");
                resetStoppedState();
            });
        } catch (BackendStartupException ex) {
            reportFailure("Falha ao iniciar o backend", ex.getMessage(), ex);
        } catch (IOException ex) {
            reportFailure("Falha de leitura/gravação",
                    "O Budget AI não conseguiu acessar os arquivos necessários.\n" + safeMessage(ex), ex);
        } catch (SecurityException ex) {
            reportFailure("Permissão negada",
                    "O Windows bloqueou o acesso a um arquivo ou processo necessário.\n" + safeMessage(ex), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            reportFailure("Inicialização interrompida", "A inicialização do backend foi interrompida.", ex);
        } catch (Exception ex) {
            reportFailure("Erro inesperado no launcher",
                    "O launcher encontrou um erro não previsto. Consulte o log para detalhes.\n" + safeMessage(ex), ex);
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
                connection = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code >= 200 && code < 500) {
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
                    return "Configuração inválida do Spring AI: um módulo OpenAI não utilizado tentou iniciar sem credencial. Este build deveria manter esses módulos desativados.";
                }
                if (line.contains("Port 8080") && line.toLowerCase().contains("use")) {
                    return "A porta 8080 já está sendo usada por outro programa. Feche o processo que usa a porta e tente novamente.";
                }
                if (line.contains("BindException") || line.contains("Address already in use")) {
                    return "Não foi possível abrir a porta 8080 porque ela já está em uso.";
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
                if (!out.isEmpty()) out.append('\n');
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
            showInfo("Painel Budget AI", "Não consegui abrir o navegador automaticamente.\nAbra manualmente: " + PANEL_URL);
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
                        "Write-Host 'Codex CLI não encontrado. Instalando pelo instalador oficial...' -ForegroundColor Cyan; " +
                        "irm https://chatgpt.com/codex/install.ps1 | iex; " +
                        "$cmd=Get-Command codex -ErrorAction SilentlyContinue; " +
                        "if($cmd){ & $cmd.Source } elseif(Test-Path $native){ & $native } " +
                        "else { throw 'Codex foi instalado, mas o executável não foi localizado.' } " +
                        "}";

                new ProcessBuilder("powershell.exe", "-NoExit", "-NoProfile",
                        "-ExecutionPolicy", "Bypass", "-Command", command).start();
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> showError("Falha ao abrir Codex",
                        "O PowerShell/Codex não pôde ser iniciado.\n" + safeMessage(ex)));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError("Erro no Codex",
                        "Não foi possível abrir ou instalar o Codex.\n" + safeMessage(ex)));
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
            resetStoppedState();
            statusLabel.setText("Parado");
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
    }

    private void resetStoppedState() {
        serverProcess = null;
        startButton.setEnabled(true);
        openButton.setEnabled(false);
        stopButton.setEnabled(false);
        nvidiaKeyField.setEnabled(true);
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
        if (logFile == null) return;
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, System.lineSeparator() + "[Launcher] " + line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
        private BackendStartupException(String message) {
            super(message);
        }
    }
}
