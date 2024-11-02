plugins {
	java
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
	all{
		// was tomcat 제외
		exclude(module = "spring-boot-starter-tomcat") // Tomcat 제외
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.5"){
		exclude(module = "spring-boot-starter-tomcat") // Tomcat 제외
	}
	implementation("org.springframework.boot:spring-boot-starter-undertow:3.3.5")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-data-rest")
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web

	implementation("org.springframework.boot:spring-boot-starter-hateoas")
//	implementation("org.springframework.boot:spring-boot-starter-web") {
//		exclude(module = "spring-boot-starter-tomcat") // Tomcat 제외
//	}	// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-undertow

	// https://mvnrepository.com/artifact/org.postgresql/postgresql
// https://mvnrepository.com/artifact/org.postgresql/postgresql
	implementation("org.postgresql:postgresql:42.7.4")
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-amqp
	implementation("org.springframework.boot:spring-boot-starter-amqp:3.3.5")
// https://mvnrepository.com/artifact/org.redisson/redisson
	implementation("org.redisson:redisson:3.38.0")

	compileOnly("org.projectlombok:lombok")
//	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
//	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.withType<Test> {
	useJUnitPlatform()
}
