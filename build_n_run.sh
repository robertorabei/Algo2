#!/bin/zsh

# On ne définit plus de valeur par défaut ici
LIMIT=$1
SRC_DIR="src"
BIN_DIR="bin"
DATA_DIR="data"

mkdir -p $BIN_DIR

echo "Compilation du code source dans $BIN_DIR..."
javac -d $BIN_DIR $SRC_DIR/*.java

if [ $? -eq 0 ]; then
    echo "Compilation réussie."

    # On prépare les arguments de base de la commande Java
    JAVA_ARGS=("-Xmx2g" "-cp" "$BIN_DIR" "DblpParsingDemo" "$DATA_DIR/dblp-2026-01-01.xml.gz" "$DATA_DIR/dblp.dtd")

    # Si LIMIT est donné (non vide), on ajoute l'argument --limit
    if [ -n "$LIMIT" ]; then
        echo "Lancement du parsing avec limite : $LIMIT"
        JAVA_ARGS+=("--limit=$LIMIT")
    else
        echo "Lancement du parsing complet (pas de limite)."
    fi

    # On lance la commande avec tous les arguments accumulés
    java "${JAVA_ARGS[@]}"
else
    echo "Erreur de compilation."
fi