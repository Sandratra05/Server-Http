import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ServerGUI {
    private JFrame frame;
    private JTextArea logArea;
    private JTextField portField;
    private JTextField rootField;
    private Serveur server;
    private Thread serverThread;
    private Config config = new Config("config/server.conf");
    private int port;
    private String root;

    private static final int ICON_WIDTH = 50;
    private static final int ICON_HEIGHT = 50;

    public ServerGUI() {
        this.port = config.getIntProperty("port", 8000);
        this.root = config.getProperty("root", "www");

        frame = new JFrame("Serveur Binome");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Serveur", createServerPanel());
        tabbedPane.addTab("Configuration", createConfigPanel());

        frame.getContentPane().add(tabbedPane);
    }

    private JPanel createServerPanel() {
        JPanel serverPanel = new JPanel(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        JButton startButton = new JButton("Démarrer", resizeIcon(new ImageIcon("images/start_icon.png")));
        startButton.addActionListener(e -> startServer());

        JButton stopButton = new JButton("Arrêter", resizeIcon(new ImageIcon("images/stop_icon.png")));
        stopButton.addActionListener(e -> stopServer());

      
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        serverPanel.add(scrollPane, BorderLayout.CENTER);
        serverPanel.add(buttonPanel, BorderLayout.SOUTH);

        return serverPanel;
    }

    private JPanel createConfigPanel() {
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(new Color(244, 244, 249)); // Couleur de fond

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Conteneur principal pour les éléments de configuration
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(204, 204, 204), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Titre
        JLabel titleLabel = new JLabel("Configuration de Port");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(51, 51, 51));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        container.add(titleLabel, gbc);

        // Étiquette pour le port
        JLabel portLabel = new JLabel("Numéro du port :");
        portLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        portLabel.setForeground(new Color(85, 85, 85));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        container.add(portLabel, gbc);

        // Champ de saisie pour le port
        portField = new JTextField(String.valueOf(port), 10);
        portField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 1;
        container.add(portField, gbc);

        // Étiquette pour le root
        JLabel rootLabel = new JLabel("Chemin racine :");
        rootLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        rootLabel.setForeground(new Color(85, 85, 85));
        gbc.gridx = 0;
        gbc.gridy = 2;
        container.add(rootLabel, gbc);

        // Champ de saisie pour le root
        rootField = new JTextField(root, 20);
        rootField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 2;
          container.add(rootField, gbc);

        // Bouton d'enregistrement
        JButton saveConfigButton = new JButton("Enregistrer", resizeIcon(new ImageIcon("images/save_icon.jpeg")));
        saveConfigButton.setBackground(new Color(76, 175, 80));
        saveConfigButton.setForeground(Color.WHITE);
        saveConfigButton.setFont(new Font("Arial", Font.BOLD, 16));
        saveConfigButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        saveConfigButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveConfigButton.addActionListener(e -> saveConfiguration());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        container.add(saveConfigButton, gbc);

        // Ajouter le conteneur au panneau principal
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(container, gbc);

        return configPanel;
    }

    public void show() {
        frame.setVisible(true);
    }

    private void saveConfiguration() {
        try {
            port = Integer.parseInt(portField.getText().trim());
            root = rootField.getText().trim();
            System.out.println("Port == " + String.valueOf(port));

            config.setProperty("port", String.valueOf(port));
            config.setProperty("root", root);
            config.save();

            logArea.append("Configuration mise à jour :\n");
            logArea.append("Port : " + port + "\n");
            logArea.append("Chemin racine : " + root + "\n");
        } catch (NumberFormatException e) {
            logArea.append("Erreur : Le port doit être un nombre valide.\n");
        } catch (Exception e) {
            logArea.append("Erreur lors de la sauvegarde de la configuration : " + e.getMessage() + "\n");
        }
    }

    private void startServer() {
        logArea.append("Démarrage du serveur...\n");
        server = new Serveur(port, root);
        serverThread = new Thread(() -> {
            try {
                server.start();
                logArea.append("Serveur démarré sur le port " + port + "\n");
            } catch (Exception e) {
                logArea.append("Erreur lors du démarrage du serveur : " + e.getMessage() + "\n");
            }
        });
        serverThread.start();
    }

    private void stopServer() {
        logArea.append("Arrêt du serveur...\n");
        if (server != null) {
            try {
                server.stop();
                if (serverThread != null && serverThread.isAlive()) {
                    serverThread.interrupt();
                }
                logArea.append("Serveur arrêté.\n");
            } catch (Exception e) {
                logArea.append("Erreur lors de l'arrêt du serveur : " + e.getMessage() + "\n");
            }
        } else {
            logArea.append("Aucun serveur en cours d'exécution.\n");
        }
    }

  

    private ImageIcon resizeIcon(ImageIcon icon) {
        Image img = icon.getImage();
        Image resizedImage = img.getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH);
        return new ImageIcon(resizedImage);
    }
}
