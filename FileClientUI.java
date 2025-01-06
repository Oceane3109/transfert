// FileClientUI.java mise à jour pour gérer les nouvelles fonctionnalités
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.table.DefaultTableModel;

public class FileClientUI {
    private JFrame frame;
    private JTextField serverAddressField;
    private JTextField portField;
    private JTextArea logArea;
    private JTable remoteFileTable;
    public FileClientUI() {
        frame = new JFrame("Transfert de Fichiers");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Création du logo et redimensionnement
        ImageIcon logoIcon = new ImageIcon("data-transfer_2091563.png");
        Image logoImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH); 
        JLabel logoLabel = new JLabel(new ImageIcon(logoImage));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        logoPanel.add(logoLabel); 

        JPanel inputPanel = new JPanel(new BorderLayout(10, 10)); 
        inputPanel.setBorder(BorderFactory.createTitledBorder("Connexion au Serveur"));

        inputPanel.add(logoPanel, BorderLayout.WEST);

        JPanel fieldsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        JLabel serverLabel = new JLabel("Adresse du Serveur:");
        serverAddressField = new JTextField("172.10.186.182");
        fieldsPanel.add(serverLabel);
        fieldsPanel.add(serverAddressField);

        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("1435");
        fieldsPanel.add(portLabel);
        fieldsPanel.add(portField);

        JButton connectButton = new JButton("Connecter");
        connectButton.addActionListener(e -> listFiles());
        fieldsPanel.add(connectButton);
        
        inputPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Configuration de la zone des logs
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logArea.setBackground(new Color(240, 248, 255)); // Bleu clair
        logArea.setForeground(new Color(0, 10, 255));   // Bleu foncé
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));

        // Boutons d'action
        JButton deleteButton = new JButton("Supprimer Fichier");
        deleteButton.addActionListener(e -> deleteFile());

        JButton downloadButton = new JButton("Télécharger Fichier");
        downloadButton.addActionListener(e -> downloadFile());

        JButton createButton = new JButton("Créer Fichier Vide");
        createButton.addActionListener(e -> createFile());

        JButton uploadButton = new JButton("Envoyer un Fichier");
        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                sendFile(fileChooser.getSelectedFile());
            }
        });

        // Panel des actions
        JPanel actionPanel = new JPanel();
        actionPanel.add(deleteButton);
        actionPanel.add(downloadButton);
        actionPanel.add(createButton);
        actionPanel.add(uploadButton);

        // Table des fichiers distants
        remoteFileTable = new JTable(new DefaultTableModel(new String[]{"Nom du Fichier"}, 0));
        JScrollPane remoteTableScrollPane = new JScrollPane(remoteFileTable);
        remoteTableScrollPane.setBorder(BorderFactory.createTitledBorder("Fichiers Distants"));

        // Disposition de la fenêtre
        frame.setLayout(new BorderLayout());
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(remoteTableScrollPane, BorderLayout.CENTER);
        frame.add(actionPanel, BorderLayout.SOUTH);
        frame.add(logScrollPane, BorderLayout.EAST);

        frame.setVisible(true);
    }
    private void listFiles() {
        String serverAddress = serverAddressField.getText();
        int port = Integer.parseInt(portField.getText());
    
        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
    
            writer.println("LIST");
    
            DefaultTableModel model = (DefaultTableModel) remoteFileTable.getModel();
            model.setRowCount(0);  // Réinitialiser la table avant de la remplir
    
            String fileName;
            while ((fileName = reader.readLine()) != null) {
                model.addRow(new Object[]{fileName});  // Ajouter chaque fichier à la table
            }
    
            logArea.append("Liste des fichiers récupérée avec succès.\n");
        } catch (IOException e) {
            logArea.append("Erreur lors de la récupération de la liste des fichiers : " + e.getMessage() + "\n");
        }
    }

    private void deleteFile() {
        String serverAddress = serverAddressField.getText();
        int port = Integer.parseInt(portField.getText());

        int selectedRow = remoteFileTable.getSelectedRow();
        if (selectedRow == -1) {
            logArea.append("Aucun fichier sélectionné pour suppression.\n");
            return;
        }

        String fileName = (String) remoteFileTable.getValueAt(selectedRow, 0);

        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("DELETE");
            writer.println(fileName);

            String response = reader.readLine();
            logArea.append(response + "\n");

            if (response.contains("succès")) {
                listFiles();
            }
        } catch (IOException e) {
            logArea.append("Erreur lors de la suppression du fichier: " + e.getMessage() + "\n");
        }
    }

    private void downloadFile() {
        String serverAddress = serverAddressField.getText();
        int port = Integer.parseInt(portField.getText());

        int selectedRow = remoteFileTable.getSelectedRow();
        if (selectedRow == -1) {
            logArea.append("Aucun fichier sélectionné pour téléchargement.\n");
            return;
        }

        String fileName = (String) remoteFileTable.getValueAt(selectedRow, 0);

        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             InputStream inputStream = socket.getInputStream()) {

            writer.println("DOWNLOAD");
            writer.println(fileName);

            File outputFile = new File(fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            logArea.append("Fichier téléchargé avec succès: " + fileName + "\n");
        } catch (IOException e) {
            logArea.append("Erreur lors du téléchargement du fichier: " + e.getMessage() + "\n");
        }
    }

    private void createFile() {
        String serverAddress = serverAddressField.getText();
        int port = Integer.parseInt(portField.getText());

        String fileName = JOptionPane.showInputDialog(frame, "Nom du fichier à créer:");
        if (fileName == null || fileName.trim().isEmpty()) {
            logArea.append("Nom de fichier invalide.\n");
            return;
        }

        try (Socket socket = new Socket(serverAddress, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("CREATE");
            writer.println(fileName);

            String response = reader.readLine();
            logArea.append(response + "\n");

            if (response.contains("succès")) {
                listFiles();
            }
        } catch (IOException e) {
            logArea.append("Erreur lors de la création du fichier: " + e.getMessage() + "\n");
        }
    }

    private void sendFile(File fileToUpload) {
        String serverAddress = serverAddressField.getText();
        int port;

        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            logArea.append("Erreur : Port invalide.\n");
            return;
        }

        if (fileToUpload == null || !fileToUpload.exists()) {
            logArea.append("Erreur : Aucun fichier valide à envoyer.\n");
            return;
        }

        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, port);
                 FileInputStream fileInputStream = new FileInputStream(fileToUpload);
                 OutputStream outputStream = socket.getOutputStream();
                 PrintWriter writer = new PrintWriter(outputStream, true)) {

                logArea.append("Connexion au serveur " + serverAddress + ":" + port + "\n");

                // Envoi de la commande "UPLOAD"
                writer.println("UPLOAD");
                writer.println(fileToUpload.getName());

                long fileSize = fileToUpload.length();
                long bytesSent = 0;
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                    bytesSent += bytesRead;
                    int progress = (int) ((bytesSent * 100) / fileSize);
                    SwingUtilities.invokeLater(() -> logArea.append("Progression : " + progress + "%\n"));
                }

                logArea.append("Fichier " + fileToUpload.getName() + " envoyé avec succès.\n");

                // Mettre à jour la liste des fichiers distants
                listFiles();

            } catch (IOException e) {
                logArea.append("Erreur lors de l'envoi du fichier : " + e.getMessage() + "\n");
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileClientUI::new);
    }
}
