
./gradlew build -x test &&

if [ ! -f build/apps/kubectl ]; then
  echo "Downloading kubectl to build/apps/kubectl ..."
  mkdir -p "build/apps"
  # wget is installed on more systems than curl
  wget "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" -O build/apps/kubectl
fi

docker build -f src/main/docker/Dockerfile.jvm -t docker.dankito.net/dankito/k7s-jvm . &&

docker push docker.dankito.net/dankito/k7s-jvm