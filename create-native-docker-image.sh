
./gradlew build -Dquarkus.package.type=native -x test &&

if [ ! -f build/apps/kubectl ]; then
  mkdir -p "build/apps"
  echo "Downloading kubectl to build/apps/kubectl ..."
  # wget is installed on more systems than curl
  wget "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" -O build/apps/kubectl
fi

docker build -f src/main/docker/Dockerfile.native-micro -t docker.dankito.net/dankito/k7s . &&

docker push docker.dankito.net/dankito/k7s