FROM hbongen/openjfx-maven as builder
WORKDIR /app
COPY . .
RUN ./burst.sh compile

FROM openjdk:8-jdk-alpine
RUN apk update && apk upgrade && apk add --no-cache bash
WORKDIR /app
COPY --from=builder /app/burst.jar .
COPY html html
VOLUME ["/conf", "/db"]
COPY conf/brs.properties /conf/brs.properties
COPY conf/brs-default.properties /conf/brs-default.properties
COPY conf/logging-default.properties /conf/logging-default.properties
EXPOSE 8225 8223 8121
ENTRYPOINT ["java", "-classpath", "/app/burst.jar:/conf", "brs.Burst"]
