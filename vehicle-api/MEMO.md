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
## pack cliでもmaven-pluginでも同じ物ができるので好きな方を使う
brew install buildpacks/tap/pack
pack build vehicle-api \
 --path ./target/vehicle-api-0.0.1-SNAPSHOT.jar \
 --builder paketobuildpacks/builder:base

## buildからやる場合 (コンテナ上でソースがビルドされる。二回目以降はキャッシュが効く)
pack build vehicle-api \
 --builder paketobuildpacks/builder:base

docker run --rm \
 -p 8080:8080 \
 -m 1g \
 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/vehicle \
 vehicle-api
```
builderの構成要素
- buildpacks ソースからイメージを作成する作業単位 (builderには複数のbuildpackが含まれる)
- stack ベースとなるOSイメージ（ビルド用とランタイム用がある)
- lifecycle 実際のビルド処理を行うAPI

ビルダーによってイメージが違うしサポート言語も違う
下記コマンドでサジェストしてくれる

```
$ pack builder suggest
Suggested builders:
	Google:                gcr.io/buildpacks/builder:v1      Ubuntu 18 base image with buildpacks for .NET, Go, Java, Node.js, and Python
	Heroku:                heroku/buildpacks:18              Base builder for Heroku-18 stack, based on ubuntu:18.04 base image
	Heroku:                heroku/buildpacks:20              Base builder for Heroku-20 stack, based on ubuntu:20.04 base image
	Paketo Buildpacks:     paketobuildpacks/builder:base     Ubuntu bionic base image with buildpacks for Java, .NET Core, NodeJS, Go, Ruby, NGINX and Procfile
	Paketo Buildpacks:     paketobuildpacks/builder:full     Ubuntu bionic base image with buildpacks for Java, .NET Core, NodeJS, Go, PHP, Ruby, Apache HTTPD, NGINX and Procfile
	Paketo Buildpacks:     paketobuildpacks/builder:tiny     Tiny base image (bionic build image, distroless-like run image) with buildpacks for Java Native Image and Go

Tip: Learn more about a specific builder with:
	pack builder inspect <builder-image>
```

