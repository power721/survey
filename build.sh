#!/bin/bash
set -e

echo "Building project with Maven..."
mvn clean package -DskipTests

echo "Done. Backend JAR built in backend/target/"
