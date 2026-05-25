FROM eclipse-temurin:21
WORKDIR /usr/app
COPY ./static-content ./static-content
COPY ./build/libs ./libs
CMD ["java", "-jar", "./libs/2526-2-common.jar"]