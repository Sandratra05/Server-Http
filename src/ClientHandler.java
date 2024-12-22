import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String root;
    public ClientHandler(Socket clientSocket, String root) {
        this.clientSocket = clientSocket;
        this.root = root;
    }
    
    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Lire la première ligne de la requête HTTP
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }
            // Décomposer la requête HTTP
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }
            String method = parts[0]; // Méthode HTTP (GET, POST, etc.)
            String fullPath = parts[1]; // Chemin de la ressource demandée
            // Identifier le chemin du fichier demandé
            String filePath = fullPath.split("\\?")[0];
            File requestedFile = new File(root, filePath);
            // Gérer les requêtes en fonction de la méthode HTTP
            if ("GET".equalsIgnoreCase(method)) {
                handleGetRequest(requestedFile, out, clientSocket.getOutputStream());
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePostRequest(requestedFile, in, out);
            } else {
                sendErrorResponse(out, 501, "Not Implemented");
            }
        } catch (IOException e) {
            System.err.println("Erreur de traitement du client : " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture du socket client : " + e.getMessage());
            }
        }
    }

    
    private void sendHttpResponse(PrintWriter out, int statusCode, String contentType, String content) {
        out.println("HTTP/1.1 " + statusCode + " OK");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + content.length());
        out.println();
        out.print(content);
    }    

    private void sendFileResponse(File file, PrintWriter headerWriter, OutputStream dataOut) throws IOException {
        // Déterminer le type MIME en fonction de l'extension du fichier
        String contentType = getImageMimeType(file);
        if (contentType == null) {
            contentType = Files.probeContentType(file.toPath()); // Détecter automatiquement si ce n'est pas une image
        }
        if (contentType == null) {
            contentType = "application/octet-stream"; // Type générique si le MIME est inconnu
        }

        // Envoyer les en-têtes HTTP
        headerWriter.println("HTTP/1.1 200 OK");
        headerWriter.println("Content-Type: " + contentType);
        headerWriter.println("Content-Length: " + file.length());
        headerWriter.println("Connection: close"); // Ajout pour une compatibilité accrue
        headerWriter.println();
        headerWriter.flush();

        // Envoyer le contenu du fichier
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; // Buffer pour lire le fichier par morceaux
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            dataOut.flush();
        }
    }

    private String getImageMimeType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (fileName.endsWith(".ico")) {
            return "image/x-icon";
        }
        return null; // Retourne null si ce n'est pas une image reconnue
    }

    private void handleGetRequest(File file, PrintWriter headerWriter, OutputStream dataOut) {
        try {
            if (!file.exists()) {
                sendErrorResponse(headerWriter, 404, "Not Found");
                return;
            }

            if (file.isDirectory()) {
                sendDirectoryListing(file, headerWriter, dataOut); // Si répertoire, lister
            } else if (file.getName().endsWith(".php")) {
                // Exécution du fichier PHP
                String response = executePhpFile(file.getAbsolutePath(), null);
                if (response != null) {
                    sendHttpResponse(headerWriter, 200, "text/html", response);
                } else {
                    sendErrorResponse(headerWriter, 500, "Erreur lors de l'ex&eacute;cution du fichier PHP.");
                }
            } else if (file.getName().endsWith(".c")) {
                handleCFile(file, headerWriter);
            }
            else {
                sendFileResponse(file, headerWriter, dataOut); // Gérer tous les fichiers
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la gestion de la requête GET : " + e.getMessage());
            sendErrorResponse(headerWriter, 500, "Internal Server Error");
        }
    }
    
    private void handlePostRequest(File file, BufferedReader in, PrintWriter out) {
        StringBuilder postData = new StringBuilder();
        try {
            // Lire le corps de la requête POST
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                postData.append(line).append("\n");
            }
    
            if (file.getName().endsWith(".php")) {
                // Exécution du fichier PHP avec les données POST
                String response = executePhpFile(file.getAbsolutePath(), postData.toString());
                if (response != null) {
                    sendHttpResponse(out, 200, "text/html", response);
                } else {
                    sendErrorResponse(out, 500, "Erreur lors de l'exécution du fichier PHP.");
                }
            } else {
                sendErrorResponse(out, 405, "Method Not Allowed");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la gestion de la requête POST : " + e.getMessage());
            sendErrorResponse(out, 500, "Internal Server Error");
        }
    }
    
    
    private void sendErrorResponse(PrintWriter out, int statusCode, String message) {
        out.println("HTTP/1.1 " + statusCode + " " + message);
        out.println("Content-Type: text/html");
        out.println();
        out.println("<html><body><h1>" + statusCode + " - " + message + "</h1></body></html>");
    }

    private void sendResponse(OutputStream out, String status, String contentType, byte[] content)
            throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        writer.println("HTTP/1.1 " + status);
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + content.length);
        writer.println();
        out.write(content);
        out.flush();
    }

    private void sendDirectoryListing(File directory, PrintWriter out, OutputStream dataOut) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            sendResponse(dataOut, "500 Internal Server Error", "text/html", "<h1>Erreur serveur</h1>".getBytes());
            return;
        }

        StringBuilder responseContent = new StringBuilder();
        responseContent.append("<html><body><h1>Index de ").append(directory.getName()).append("</h1><ul>");

        // Lien vers le répertoire parent
        File parent = directory.getParentFile();
        if (parent != null) {
            responseContent.append("<li><a href=\"../\">../</a></li>");
        }

        for (File file : files) {
            String fileName = file.getName();
            String href = file.isDirectory() ? fileName + "/" : fileName;

            if (file.isDirectory()) {
                // Ajouter l'image "dossier.png" à côté des répertoires
                responseContent
                        .append("<img src=\"/dossier.png\" alt=\"dossier\" style=\"width:10px;height:10px;\"> ");
            } else {
                responseContent.append("<li>");
            }

            responseContent.append("<a href=\"").append(href).append("\">").append(fileName).append("</a></li>");
        }

        responseContent.append("</ul></body></html>");
        sendResponse(dataOut, "200 OK", "text/html", responseContent.toString().getBytes());
    
    }
    // private void sendPhpResponse(File phpFile, PrintWriter out) {
    //     sendPhpResponse(phpFile, null, out);
    // }
    
    private void sendPhpResponse(File phpFile, String postData, PrintWriter out) {
        try {
            // Construction de la commande pour exécuter le script PHP
            ProcessBuilder processBuilder = new ProcessBuilder("php", phpFile.getAbsolutePath());
            Process process = processBuilder.start();
    
            // Si des données POST sont présentes, les envoyer en entrée du script PHP
            if (postData != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(postData.getBytes());
                    os.flush();
                }
            }
    
            // Lire la sortie du script PHP
            BufferedReader phpOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = phpOutput.readLine()) != null) {
                response.append(line).append("\n");
            }
    
            // Attendre que le processus se termine
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Envoyer une réponse HTTP avec le contenu retourné par le script PHP
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println();
                out.print(response.toString());
            } else {
                // En cas d'erreur, renvoyer une réponse 500
                sendErrorResponse(out, 500, "Erreur lors de l'exécution du script PHP.");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de l'exécution du fichier PHP : " + e.getMessage());
            sendErrorResponse(out, 500, "Erreur interne du serveur.");
        }
    }
    
    private String executePhpFile(String phpFilePath, String postData) {
        try {
            // Commande pour exécuter le fichier PHP
            ProcessBuilder processBuilder = new ProcessBuilder("php", phpFilePath);
            Process process = processBuilder.start();
    
            // Si des données POST existent, les envoyer en entrée standard
            if (postData != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(postData.getBytes());
                    os.flush();
                }
            }
    
            // Lire la sortie du script PHP
            BufferedReader phpOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = phpOutput.readLine()) != null) {
                response.append(line).append("\n");
            }
    
            // Attendre que le processus se termine
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return response.toString(); // Retourner la réponse en cas de succès
            } else {
                System.err.println("Erreur lors de l'exécution du fichier PHP. Code de sortie : " + exitCode);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de l'exécution du fichier PHP : " + e.getMessage());
            return null;
        }
    }
    
    
    private void handleCFile(File cFile, PrintWriter out) {
        try {
            // Étape 1 : Compiler le fichier C
            String executableName = cFile.getName().replace(".c", ""); // Nom de l'exécutable
            File parentDirectory = cFile.getParentFile();
            File executableFile = new File(parentDirectory, executableName); // Exécutable dans le même répertoire
    
            Process compileProcess = new ProcessBuilder("gcc", cFile.getAbsolutePath(), "-o", executableFile.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
    
            // Lire les erreurs de compilation, s'il y en a
            StringBuilder compileOutput = new StringBuilder();
            try (BufferedReader compileReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
                String line;
                while ((line = compileReader.readLine()) != null) {
                    compileOutput.append(line).append("\n");
                }
            }
    
            int compileStatus = compileProcess.waitFor();
            if (compileStatus != 0) {
                sendErrorResponse(out, 500, "Erreur de compilation :\n" + compileOutput);
                return;
            }
    
            // Vérification que l'exécutable a bien été créé
            if (!executableFile.exists()) {
                sendErrorResponse(out, 500, "Erreur : L'exécutable n'a pas été créé.");
                return;
            }
    
            // Étape 2 : Exécuter l'exécutable
            Process executionProcess = new ProcessBuilder(executableFile.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
    
            // Lire la sortie de l'exécution
            StringBuilder executionOutput = new StringBuilder();
            try (BufferedReader executionReader = new BufferedReader(new InputStreamReader(executionProcess.getInputStream()))) {
                String line;
                while ((line = executionReader.readLine()) != null) {
                    executionOutput.append(line).append("\n");
                }
            }
    
            int executionStatus = executionProcess.waitFor();
            if (executionStatus != 0) {
                sendErrorResponse(out, 500, "Erreur lors de l'exécution du fichier compilé :\n" + executionOutput);
                return;
            }
    
            // Étape 3 : Envoyer la sortie comme réponse HTTP
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println(executionOutput);
    
            // Nettoyer l'exécutable après l'exécution
            if (!executableFile.delete()) {
                System.err.println("Erreur : Impossible de supprimer l'exécutable " + executableFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la gestion du fichier C : " + e.getMessage());
            sendErrorResponse(out, 500, "Erreur interne du serveur.");
        }
    }    
}