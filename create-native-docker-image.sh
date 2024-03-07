
./gradlew build -Dquarkus.package.type=native -x test &&

docker build -f src/main/docker/Dockerfile.native-micro -t docker.dankito.net/dankito/k7s . &&

docker push docker.dankito.net/dankito/k7s