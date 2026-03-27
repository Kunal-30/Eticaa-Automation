package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Manages a single SSH tunnel process for PostgreSQL access.
 * Windows-compatible implementation using PuTTY plink.
 */
public class SSHTunnelManager {

    // Single shared tunnel process
    private static Process tunnelProcess;
    private static final Object LOCK = new Object();

    // SSH configuration
    private static final String SSH_USER = "test";
    private static final String SSH_HOST = "100.74.70.82";
    private static final String SSH_PORT = "22";
    private static final String SSH_PASSWORD = "Etc@2026";

    // IMPORTANT: Local port changed to 5433 because 5432 is already used locally
    private static final String LOCAL_PORT = "5433";
    private static final String REMOTE_PORT = "5432";

    /**
     * Starts the SSH tunnel if not already running
     */
    public static void startTunnel() {

        synchronized (LOCK) {

            try {

                // Prevent multiple tunnels
                if (tunnelProcess != null && tunnelProcess.isAlive()) {
                    System.out.println("[SSH] Tunnel already running. Skipping start.");
                    return;
                }

                System.out.println();
                System.out.println("[SSH] =========================================");
                System.out.println("[SSH] Starting SSH Tunnel");
                System.out.println("[SSH] Host          : " + SSH_HOST);
                System.out.println("[SSH] User          : " + SSH_USER);
                System.out.println("[SSH] SSH Port      : " + SSH_PORT);
                System.out.println("[SSH] Local Port    : " + LOCAL_PORT);
                System.out.println("[SSH] Remote Target : localhost:" + REMOTE_PORT);
                System.out.println("[SSH] =========================================");

                // Build plink command
                String sshCommand = String.format(
                        "plink -ssh -P %s -L %s:localhost:%s -pw \"%s\" %s@%s -N",
                        SSH_PORT,
                        LOCAL_PORT,
                        REMOTE_PORT,
                        SSH_PASSWORD,
                        SSH_USER,
                        SSH_HOST
                );

                System.out.println("[SSH] Executing command:");
                System.out.println("[SSH] " + sshCommand);
                System.out.println("[SSH] =========================================");

                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", sshCommand);

                // Merge error stream
                pb.redirectErrorStream(true);

                tunnelProcess = pb.start();

                // Read plink output
                new Thread(() -> {

                    try (BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(tunnelProcess.getInputStream()))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            System.out.println("[SSH OUTPUT] " + line);
                        }

                    } catch (Exception e) {
                        System.out.println("[SSH] Error reading tunnel output: " + e.getMessage());
                    }

                }).start();

                // Wait for tunnel initialization
                System.out.println("[SSH] Waiting for tunnel to establish...");
                Thread.sleep(5000);

                if (tunnelProcess.isAlive()) {

                    System.out.println("[SSH] ✅ Tunnel started successfully.");
                    System.out.println("[SSH] You can now connect database using:");
                    System.out.println("[SSH] jdbc:postgresql://localhost:" + LOCAL_PORT + "/your_database");
                    System.out.println("[SSH] =========================================");

                } else {

                    System.out.println("[SSH] ❌ Tunnel process exited unexpectedly.");
                    throw new RuntimeException("SSH Tunnel failed to start.");

                }

            } catch (IOException e) {

                System.out.println("[SSH] ❌ ERROR: Failed to start SSH tunnel.");
                System.out.println("[SSH] Reason: " + e.getMessage());
                throw new RuntimeException("Failed to start SSH tunnel", e);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                System.out.println("[SSH] Tunnel startup interrupted.");

            }
        }
    }

    /**
     * Stops the SSH tunnel
     */
    public static void stopTunnel() {

        synchronized (LOCK) {

            System.out.println();
            System.out.println("[SSH] =========================================");
            System.out.println("[SSH] Stopping SSH Tunnel");

            if (tunnelProcess != null && tunnelProcess.isAlive()) {

                tunnelProcess.destroy();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (!tunnelProcess.isAlive()) {
                    System.out.println("[SSH] ✅ Tunnel stopped successfully.");
                } else {
                    System.out.println("[SSH] ⚠ Tunnel still running.");
                }

            } else {

                System.out.println("[SSH] No active tunnel found.");

            }

            tunnelProcess = null;

            System.out.println("[SSH] =========================================");

        }
    }

}