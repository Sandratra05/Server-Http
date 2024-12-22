import java.io.*;
import java.util.Properties;

public class Config {
    private Properties properties;
    private final String configFilePath;

    public Config(String configFilePath) {
        this.configFilePath = configFilePath;
        properties = new Properties();
        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
        }
    }

    // Lire une propriété
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    // Lire une propriété avec une valeur par défaut
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Lire une propriété entière
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Modifier une propriété
    public void setProperty(String key, String value) {
        System.out.println("setProperty() appelé avec key = " + key + ", value = " + value);
        properties.setProperty(key, value);
        System.out.println("Propriétés actuelles : " + properties);
    }

    // Sauvegarder les propriétés dans le fichier
    public void save() {
        try (OutputStream outputStream = new FileOutputStream(configFilePath)) {
            properties.store(outputStream, "Mise à jour du fichier de configuration");
            System.out.println("Fichier de configuration mis à jour avec succès: " + configFilePath);
            System.out.println("Contenu sauvegardé : " + properties);
            System.out.println("Chemin complet du fichier : " + new File(configFilePath).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde du fichier de configuration : " + e.getMessage());
        }
    }
}
