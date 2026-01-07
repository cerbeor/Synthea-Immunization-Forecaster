FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace
COPY . .
RUN ./gradlew --no-daemon installDist -x test

FROM eclipse-temurin:17-jre-jammy

WORKDIR /opt/synthea
COPY --from=builder /workspace/build/install/synthea/ ./
COPY --from=builder /workspace/config ./config
COPY --from=builder /workspace/lib ./lib

ENV PATH="/opt/synthea/bin:${PATH}"
VOLUME ["/opt/synthea/output"]
ENTRYPOINT ["/opt/synthea/bin/synthea"]
