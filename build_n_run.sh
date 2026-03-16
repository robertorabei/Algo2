#!/bin/zsh

SRC_DIR="src"
BIN_DIR="bin"
DATA_DIR="data"

mkdir -p $BIN_DIR

echo "Compilation du code source dans $BIN_DIR..."
javac -d $BIN_DIR $SRC_DIR/*.java

if [ $? -eq 0 ]; then
    echo "Compilation réussie."

    echo "Lancement du parsing..."
    java -Xmx2g -cp $BIN_DIR DblpParsingDemo \
        $DATA_DIR/dblp-2026-01-01.xml.gz \
        $DATA_DIR/dblp.dtd \
        --limit=1000
else
    echo "Erreur de compilation."
fi