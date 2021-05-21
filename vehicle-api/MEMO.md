```sh
# postgres DB
docker run --rm \
 -p 5432:5432 \
 -e POSTGRES_DB=vehicle \
 -e POSTGRES_USER=vehicle \
 -e POSTGRES_PASSWORD=vehicle \
 bitnami/postgresql:11.11.0-debian-10-r59

# build jar & run
./mvnw clean package -Dmaven.test.skip=true
java -jar target/vehicle-api-0.0.1-SNAPSHOT.jar

# build OCI image & run
## イメージ名は -Dspring-boot.build-image.imageName=**** で変更可能
./mvnw spring-boot:build-image -Dmaven.test.skip=true
docker run --rm \
 -p 8080:8080 \
 -m 1g \
 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/vehicle \
 docker.io/library/vehicle-api:0.0.1-SNAPSHOT

# cloudnative pack CLI
brew install buildpacks/tap/pack
```
