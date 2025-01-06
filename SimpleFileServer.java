import java.io.*;
import java.net.*;
import java.util.Properties;

public class SimpleFileServer {
    private int port;
    private String broadcastAddress;
    private int udpPort;
    private int udpPort2;
    private int udpPort3;
    private String storageDir;
    private String storageDir2;
    private String storageDir3;

    public static void main(String[] args) {
        SimpleFileServer server = new SimpleFileServer();
        server.loadConfiguration("config.properties");
        server.startServer();
    }

    // Chargement de la configuration à partir du fichier config.properties
    public void loadConfiguration(String configFile) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);

            port = Integer.parseInt(properties.getProperty("serverPort"));
            broadcastAddress = properties.getProperty("broadcastAddress");
            udpPort = Integer.parseInt(properties.getProperty("udpPort"));
            storageDir = properties.getProperty("storageDir");
            udpPort2 = Integer.parseInt(properties.getProperty("udpPort2"));
            storageDir2 = properties.getProperty("storageDir2");
            udpPort3 = Integer.parseInt(properties.getProperty("udpPort3"));
            storageDir3 = properties.getProperty("storageDir3");

            System.out.println("Configuration chargée avec succès : " +
                "port=" + port + ", " +
                "broadcastAddress=" + broadcastAddress + ", " +
                "udpPorts=" + udpPort + ", " + udpPort2 + ", " + udpPort3);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration : " + e.getMessage());
        }
    }

    //Démarrage du serveur principal
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur prêt à recevoir les connexions sur le port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connexion établie avec : " + clientSocket.getInetAddress());

                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du serveur : " + e.getMessage());
        }
    }

    // Gestion des commandes envoyées par le client
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String command = reader.readLine();
            System.out.println("Commande reçue : " + command);

            switch (command) {
                case "UPLOAD":
                    forwardCommandToSubServers("UPLOAD", reader, writer);
                    break;
                case "DOWNLOAD":
                    forwardCommandToSubServers("DOWNLOAD", reader, writer);
                    break;
                case "LIST":
                    forwardCommandToSubServers("LIST", reader, writer);
                    break;
                case "DELETE":
                    forwardCommandToSubServers("DELETE", reader, writer);
                    break;
                case "CREATE":
                    forwardCommandToSubServers("CREATE", reader, writer);
                    break;
                default:
                    writer.write("Commande inconnue\n");
                    writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du traitement du client : " + e.getMessage());
        }
    }

    // Transmission de la commande aux sous-serveurs via UDP
    private void forwardCommandToSubServers(String command, BufferedReader reader, BufferedWriter writer) throws IOException {
        String fileName = reader.readLine();
        System.out.println("Commande " + command + " pour le fichier : " + fileName);

        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);

            // Envoi de la commande à chaque sous-serveur via UDP
            sendCommandToSubServer(socket, broadcastAddr, udpPort, storageDir, command, fileName);
            sendCommandToSubServer(socket, broadcastAddr, udpPort2, storageDir2, command, fileName);
            sendCommandToSubServer(socket, broadcastAddr, udpPort3, storageDir3, command, fileName);

            // Attente de la réponse des sous-serveurs
            String response = getResponseFromSubServers(socket);
            writer.write(response + "\n");
            writer.flush();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi de la commande au sous-serveur : " + e.getMessage());
            writer.write("Erreur lors de la communication avec les sous-serveurs.\n");
            writer.flush();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Envoi de la commande à un sous-serveur via UDP
    private void sendCommandToSubServer(DatagramSocket socket, InetAddress broadcastAddr, int port, String storageDir, String command, String fileName) throws IOException {
        String fullCommand = command + " " + fileName + " " + storageDir;
        byte[] commandBytes = fullCommand.getBytes();
        DatagramPacket commandPacket = new DatagramPacket(commandBytes, commandBytes.length, broadcastAddr, port);
        socket.send(commandPacket);
        System.out.println("Commande \"" + command + "\" envoyée au sous-serveur sur le port " + port);
    }

    // Récupération de la réponse des sous-serveurs
    private String getResponseFromSubServers(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[4096];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);
        return new String(responsePacket.getData(), 0, responsePacket.getLength());
    }
}
