<?php 
// Vérifiez si la clé 'nom' existe dans $_POST
var_dump($_POST['nom']);
$nom = isset($_POST['nom']) ? htmlspecialchars($_POST['nom']) : 'Nom non fourni';
$prenom = isset($_POST['prenom']) ? htmlspecialchars($_POST['prenom']) : 'Prénom non fourni';

echo $nom;
echo $prenom;

?>
