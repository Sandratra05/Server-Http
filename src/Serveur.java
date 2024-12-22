import java.io.*;
import java.net.*;

public class Serveur {
    private final int port;
    private final String root;
    private ServerSocket serverSocket;
    private boolean running;

    public Serveur(int port, String root) {
        this.port = port;
        this.root = root;
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server HTTP démarré sur http://localhost:" + port);

            while (running) {
                try {
                    // Attendre une connexion client
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Connexion acceptée : " + clientSocket.getInetAddress());

                    // Créer un nouveau thread pour gérer la requête du client
                    ClientHandler client = new ClientHandler(clientSocket, root);
                    new Thread(client).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Erreur du Serveur : " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur du Serveur : " + e.getMessage());
        }
    }

    public void stop() {
        try {
            running = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Serveur arrêté.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du serveur : " + e.getMessage());
        }
    }
}
