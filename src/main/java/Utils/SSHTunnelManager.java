package Utils;

import java.io.IOException;

/**
 * Manages a single SSH tunnel process for PostgreSQL access.
 * Windows-compatible implementation using the local ssh client.
 */
public class SSHTunnelManager {

    // Single shared tunnel process
    private static Process tunnelProcess;
    private static final Object LOCK = new Object();

    // SSH configuration for DEV server (password-based login via plink).
    // NOTE: This embeds the SSH password in code; keep this file private.
    private static final String SSH_USER = "dev";
    private static final String SSH_HOST = "100.126.146.20";
    private static final String SSH_PORT = "22";
    private static final String SSH_PASSWORD = "Etc@2026";

    // Local port 5432 -> remote localhost:5432
    private static final String LOCAL_PORT = "5432";
    private static final String REMOTE_PORT = "5432";

    /**
     * Starts the SSH tunnel if not already running.
     * Uses PuTTY plink with password, e.g.:
     *   plink -ssh -P 22 -L 5432:localhost:5432 -pw ****** dev@100.126.146.20 -N
     * Requires plink.exe to be installed and available on PATH.
     */
    public static void startTunnel() {
        synchronized (LOCK) {
            try {
                // Prevent multiple tunnels
                if (tunnelProcess != null && tunnelProcess.isAlive()) {
                    System.out.println("[SSH] Tunnel already running. Skipping start.");
                    return;
                }

                System.out.println("[SSH] =========================================");
                System.out.println("[SSH] Starting tunnel");
                System.out.println("[SSH] Host: " + SSH_HOST + " (user: " + SSH_USER + ", port: " + SSH_PORT + ")");
                System.out.println("[SSH] Local port: " + LOCAL_PORT + " -> Remote: localhost:" + REMOTE_PORT);
                System.out.println("[SSH] =========================================");

                // Build plink command with password so no interactive prompt is needed.
                String sshCommand = String.format(
                        "plink -ssh -P %s -L %s:localhost:%s -pw \"%s\" %s@%s -N",
                        SSH_PORT,
                        LOCAL_PORT,
                        REMOTE_PORT,
                        SSH_PASSWORD,
                        SSH_USER,
                        SSH_HOST
                );

                // On Windows, run via cmd.exe
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", sshCommand);

                // Merge stderr into stdout
                pb.redirectErrorStream(true);

                tunnelProcess = pb.start();

                // Give the tunnel some time to establish
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Basic health check
                if (!tunnelProcess.isAlive()) {
                    System.out.println("[SSH] ❌ Tunnel process exited immediately.");
                    throw new RuntimeException("[SSH] Failed to start SSH tunnel (process not alive).");
                }

                System.out.println("[SSH] ✅ Tunnel started successfully and is running.");
                System.out.println("[SSH] =========================================");

            } catch (IOException e) {
                System.out.println("[SSH] ❌ ERROR: Failed to start SSH tunnel.");
                System.out.println("[SSH] Reason: " + e.getMessage());
                throw new RuntimeException("Failed to start SSH tunnel", e);
            }
        }
    }

    /**
     * Stops the SSH tunnel if running.
     */
    public static void stopTunnel() {
        synchronized (LOCK) {
            System.out.println("[SSH] =========================================");
            System.out.println("[SSH] Stopping tunnel");
            if (tunnelProcess != null && tunnelProcess.isAlive()) {
                tunnelProcess.destroy();
                System.out.println("[SSH] ✅ Tunnel process destroyed.");
            } else {
                System.out.println("[SSH] No running tunnel process to stop.");
            }
            tunnelProcess = null;
            System.out.println("[SSH] =========================================");
        }
    }
}