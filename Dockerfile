FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew clean build -x test -Pproduction

CMD ["sh", "-c", "java -jar build/libs/app.jar"]