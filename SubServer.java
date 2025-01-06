import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class SubServer {
    private int port;
    private String stockage;

    public static void main(String[] args) {
        SubServer subServer = new SubServer();
        subServer.loadConfiguration("config.properties");
        subServer.startServer();
    }

    // Chargement de la configuration à partir du fichier config.properties
    public void loadConfiguration(String configFile) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);

            port = Integer.parseInt(properties.getProperty("udpPort"));
            stockage = properties.getProperty("storageDir");
            System.out.println("Sous-serveur démarré sur le port " + port + " avec le répertoire de stockage " + stockage);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
        }
    }

    // Démarrage du sous-serveur
    public void startServer() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Sous-serveur prêt à recevoir des commandes sur le port " + port + "...");

            while (true) {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String command = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Commande reçue : " + command);

                String[] commandParts = command.split(" ");
                String action = commandParts[0];
                String fileName = commandParts[1];
                String dir = commandParts[2]; // répertoire spécifique pour chaque sous-serveur

                // Gestion des commandes
                String response = handleCommand(action, fileName, dir);
                byte[] responseBytes = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de l'exécution du sous-serveur : " + e.getMessage());
        }
    }

    // Gestion des commandes reçues
    private String handleCommand(String action, String fileName, String dir) {
        switch (action) {
            case "UPLOAD":
                return uploadFile(fileName, dir);
            case "DOWNLOAD":
                return downloadFile(fileName, dir);
            case "LIST":
                return listFiles(dir);
            case "DELETE":
                return deleteFile(fileName, dir);
            case "CREATE":
                return createFile(fileName, dir);
            default:
                return "Commande inconnue";
        }
    }

    // Implémentation des commandes (upload, download, etc.) utilisant 'dir' comme répertoire de stockage
    private String uploadFile(String fileName, String dir) {
        Path filePath = Paths.get(dir, fileName);
        try {
            Files.createFile(filePath);
            return "Fichier " + fileName + " téléchargé avec succès.";
        } catch (IOException e) {
            return "Erreur lors du téléchargement du fichier " + fileName;
        }
    }

    private String downloadFile(String fileName, String dir) {
        Path filePath = Paths.get(dir, fileName);
        if (Files.exists(filePath)) {
            return "Fichier " + fileName + " trouvé.";
        } else {
            return "Fichier " + fileName + " introuvable.";
        }
    }

    private String listFiles(String dir) 
    {
        Path directoryPath = Paths.get(dir);
        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            StringBuilder fileList = new StringBuilder();
            try (Stream<Path> paths = Files.walk(directoryPath)) {
                paths.filter(Files::isRegularFile)
                    .forEach(file -> fileList.append(file.getFileName()).append("\n"));
            } catch (IOException e) {
                return "Erreur lors de la lecture du répertoire : " + e.getMessage();
            }
            return fileList.toString();
        } else {
            return "Répertoire non trouvé.";
        }
    }


    private String deleteFile(String fileName, String dir) {
        Path filePath = Paths.get(dir, fileName);
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
                return "Fichier " + fileName + " supprimé avec succès.";
            } catch (IOException e) {
                return "Erreur lors de la suppression du fichier " + fileName;
            }
        } else {
            return "Fichier " + fileName + " introuvable.";
        }
    }

    private String createFile(String fileName, String dir) {
        Path filePath = Paths.get(dir, fileName);
        if (Files.exists(filePath)) {
            return "Le fichier " + fileName + " existe déjà.";
        } else {
            try {
                Files.createFile(filePath);
                return "Fichier " + fileName + " créé avec succès.";
            } catch (IOException e) {
                return "Erreur lors de la création du fichier " + fileName;
            }
        }
    }
}
