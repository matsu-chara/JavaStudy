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

`pack inspect docker.io/library/vehicle-api:0.0.1-SNAPSHOT` するとどのbuildpackで作られたか調べられる

`--bom` をつけると依存ライブラリなどもわかる。
jre, jvmkill, memory-calculatorなどが入る

## reabse

`pack rebase docker.io/library/vehicle-api:0.0.1-SNAPSHOT` するとrebaseできる（base imageの更新などが可能）

base imageを差し替える場合はrun.Dockerfileを用意して以下を実行する

```sh
docker build . -t run-image -f run.Dockerfile
pack rebase docker.io/library/vehicle-api:0.0.1-SNAPSHOT --run-image run-image

## こちらでも良い
./mvnw spring-boot:build-image \
 -Dmaven.test.skip=true \
 -Dspring-boot.build-image.runImage=run-image \
 -Dspring-boot.build-image.pullPolicy=NEVER ## remoteにイメージがない場合、この行を入れる

## imagemagickが入ったコンテナになった
docker run --entrypoint bash --rm docker.io/library/vehicle-api:0.0.1-SNAPSHOT -c 'convert --version'
```

基本的には対象のライブラリを追加するBuildpackを作成するほうがメンテナンスしやすいので自分でrun imageを作って更新し続けるのはさけた方が良い。

## buildpackの設定

java 16を使いたい場合はBP_JVM_VERSIONを指定
```
pack build vehicle-api:16 \
--builder paketobuildpacks/builder:base \
--env BP_JVM_VERSION=16
```

maven-pluginならpom.xmlを書き換え

```
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
     <image>
        <env>
           <BP_JVM_VERSION>16</BP_JVM_VERSION>
        </env>
     </image>
  </configuration>
</plugin>
```
### Memory Calculator

自動でメモリーが計算される

- Heap = Container Memory - Non Heap - Headroom
    - Headroom: コンテナ内でJava以外に使用されるメモリ(デフォルト 0)
- Non Heap = Metaspace + Reserved CodeCache + Direct Memory + Tread Stack
    - Reserved CodeCache=240MB
    - Direct Memory=10MB
    - Thread Stack=1MB * スレッド数
    - スレッド数=250 (Servletアプリの場合) or 50 (Spring WebFluxの場合)
    - Metaspace = ロードされるクラス数 (jarに含まれるクラスの* 0.35で推定)

アプリケーションを実行すると以下のような表示が出る
```
Calculated JVM Memory Configuration: -XX:MaxDirectMemorySize=10M -Xmx449112K -XX:MaxMetaspaceSize=87463K -XX:ReservedCodeCacheSize=240M -Xss1M (Total Memory: 1G, Thread Count: 250, Loaded Class Count: 13028, Headroom: 0%)
```

## debug

Paketo Debug Buildpackを使うとIDEでデバッグできる

```
pack build vehicle-api \
 --builder paketobuildpacks/builder:base \
 --env BP_DEBUG_ENABLED=true
docker run --rm \
 -p 8080:8080 \
 -p 8000:8000 \
 -m 1g \
 -e BPL_DEBUG_ENABLED=true \
 -e BPL_DEBUG_PORT="*:8000" \
 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/vehicle \
 vehicle-api 
 ```

## jmx

以下でjconsoleでlocalhost:5000でアクセスできるようになる

```
pack build vehicle-api \
 --builder paketobuildpacks/builder:base \
 --env BP_JMX_ENABLED=true

docker run --rm \
 -p 8080:8080 \
 -p 5000:5000 \
 -m 1g \
 -e BPL_JMX_ENABLED=true \
 -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/vehicle \
 vehicle-api
```

## binding

bindingという仕組みに準拠したファイルを設置し Spring Cloud Bindingsというライブラリを通すとspringがアクセスできるようなpropertyファイルにしてくれる。

以下のようにマウントしたあと SERVICE_BINDING_ROOT を設定する
```sh
docker run --rm \
 -p 8080:8080 \
 -m 1g \
 -e SERVICE_BINDING_ROOT=/bindings \
 -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="*" \
 -v ${PWD}/bindings:/bindings \
 docker.io/library/vehicle-api:0.0.1-SNAPSHOT
```

certsをgenerate-certs.shで生成し、 TLS対応のposgresを起動する

```
docker run --rm \
 -v ${PWD}/certs:/certs \
 paketobuildpacks/run:base-cnb \
 sh /certs/generate-certs.sh

docker run --rm \
-p 5432:5432 \
-e POSTGRES_DB=vehicle \
-e POSTGRES_USER=vehicle \
-e POSTGRES_PASSWORD=vehicle \
-e POSTGRESQL_ENABLE_TLS=yes \
-e POSTGRESQL_TLS_CERT_FILE=/certs/server.crt \
-e POSTGRESQL_TLS_KEY_FILE=/certs/server.key \
-e POSTGRESQL_TLS_CA_FILE=/certs/root.crt \
-e POSTGRESQL_PGHBA_REMOVE_FILTERS=hostssl \
-v ${PWD}/certs:/certs \
bitnami/postgresql:11.11.0-debian-10-r59
```

```
## 自己署名の証明書が信頼されてないのでエラーになる
docker run --rm \
 -p 8080:8080 \
 -m 1g \
 -e SPRING_DATASOURCE_HIKARI_DATASOURCEPROPERTIES_SSLMODE=verify-full \
 -e SPRING_DATASOURCE_HIKARI_DATASOURCEPROPERTIES_SSLFACTORY=org.postgresql.ssl.DefaultJavaSSLFactory \
 -e SERVICE_BINDING_ROOT=/bindings \
 -v ${PWD}/bindings:/bindings \
 docker.io/library/vehicle-api:0.0.1-SNAPSHOT

## bindingを生成してから再実行すると通る
echo ca-certificates > bindings/trusted-certs/type
cp certs/root.crt bindings/trusted-certs/
```

環境変数SSL_CERT_DIRに追加したいCA証明書のディレクトリを指定すると、そのディレクトリ内の証明書が起動時にJavaのTruststoreに追加される。
Buildpackはtypeがca-certificatesなBindingがある場合に、その値を環境変数SSL_CERT_DIRに追加してくれる。

bindingsの中にファイルを入れておけばコンテナ起動するときにtypeごとに環境変数を追加する必要はない。（勝手に環境変数周りを変更してくれる）

## 別のbuildpack

以下のようにbuildpackを指定することで別のjreを使ったりできる

```
pack build vehicle-api:adopt-openjdk \
 --builder paketobuildpacks/builder:base \
 --buildpack paketo-buildpacks/ca-certificates \
 --buildpack paketo-buildpacks/adopt-openjdk \
 --buildpack paketo-buildpacks/maven \
 --buildpack paketo-buildpacks/executable-jar \
 --buildpack paketo-buildpacks/spring-boot
 ```
