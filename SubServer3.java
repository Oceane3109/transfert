import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class SubServer3 {
    private static int udpPort3;
    private static String storageDir3;
    private static String broadcastAddress;

    public static void main(String[] args) {
        try {
            loadConfiguration("config.properties"); // Charger la configuration
            
            System.out.println("Sous-serveur démarré : UDP Port = " + udpPort3 + ", Répertoire = " + storageDir3);

            DatagramSocket socket = new DatagramSocket(udpPort3);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String request = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Requête reçue : " + request);

                String response = handleRequest(request);

                byte[] responseData = response.getBytes();
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("Erreur dans le sous-serveur : " + e.getMessage());
        }
    }

    private static void loadConfiguration(String configFile) {
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);

            udpPort3 = Integer.parseInt(properties.getProperty("udpPort3"));
            storageDir3 = properties.getProperty("storageDir3");
            broadcastAddress = properties.getProperty("broadcastAddress");

            // Créer le répertoire de stockage s'il n'existe pas
            Files.createDirectories(Paths.get(storageDir3));
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
            System.exit(1);
        }
    }

    private static String handleRequest(String request) {
        String[] parts = request.split(" ", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";

        try {
            switch (command) {
                case "ADD":
                    return addFile(argument);
                case "DELETE":
                    return deleteFile(argument);
                case "LIST":
                    return listFiles();
                default:
                    return "Commande inconnue : " + command;
            }
        } catch (IOException e) {
            return "Erreur lors de l'exécution de la commande : " + e.getMessage();
        }
    }

    private static String addFile(String fileName) throws IOException {
        Path filePath = Paths.get(storageDir3, fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("Fichier ajouté : " + fileName);
        }

        return "Fichier ajouté avec succès : " + fileName;
    }

    private static String deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(storageDir3, fileName);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return "Fichier supprimé avec succès : " + fileName;
        } else {
            return "Fichier introuvable : " + fileName;
        }
    }

    private static String listFiles() throws IOException {
        StringBuilder fileList = new StringBuilder("Fichiers disponibles :\n");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(storageDir3))) {
            for (Path entry : stream) {
                fileList.append(entry.getFileName().toString()).append("\n");
            }
        }

        return fileList.toString();
    }
}
