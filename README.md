# Product Catalog Synchronizer

A Spring Boot application that ingests product data from Google Product Data Feeds (CSV/TSV format), validates the data, stores it in a database, and generates detailed synchronization reports.

## Technology Stack

- **Java 21** (or Java 17+)
- **Spring Boot 4.1.0** with Spring Data JPA
- **H2 Database** (in-memory for tests, configurable for production)
- **Hibernate 7.4.1** for ORM
- **Apache Commons CSV 1.10.0** for CSV parsing
- **JUnit 5** and Mockito for testing
- **Maven** for build management
- **Lombok** for reducing boilerplate code

## Building the Project

### Prerequisites

- Java 17 or later
- Maven 3.6+ (or use the included Maven wrapper `./mvnw`)

### Build Steps

```bash
# Navigate to project directory
cd product-catalog-synchronizer

# Clean and compile
./mvnw clean compile

# Run all tests
./mvnw test

# Build JAR package
./mvnw clean package

# Run the application (Spring Boot)
./mvnw spring-boot:run
```

## Running the Synchronizer

### Method 1: Using Unit Tests 

The project includes a comprehensive integration test that demonstrates how to synchronize products from the provided data files:

```bash
# Run the integration test that syncs the three provided files
./mvnw test -Dtest=ProductCatalogSynchronizerIntegrationTest

# Or run a specific test class
./mvnw test -Dtest=ProductSynchronizerServiceTest
```



## Validation Rules
https://support.google.com/merchants/answer/7052112?hl=en#zippy=%2Cother-requirements%2Cformatting-your-product-data


### Example Report Output

see report-example.md


## Database Schema

http://localhost:8080/h2-console/

The application creates a single `products` table:

```sql
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    availability VARCHAR(50) NOT NULL,
    condition VARCHAR(50) NOT NULL,
    price VARCHAR(100) NOT NULL,
    sale_price VARCHAR(100),
    link VARCHAR(500) NOT NULL,
    brand VARCHAR(255) NOT NULL,
    image_link VARCHAR(500),
    age_group VARCHAR(100),
    google_product_category VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    last_updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_product_id ON products(product_id);
```

## Testing

### Unit Tests

All components are thoroughly tested:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ProductValidatorTest
./mvnw test -Dtest=CsvParserServiceTest
./mvnw test -Dtest=ProductSynchronizerServiceTest

# Run with coverage (if Maven-Surefire is configured)
./mvnw test -Dtest=ProductCatalogSynchronizerIntegrationTest
```

## Configuring for Production

### Using MySQL Instead of H2

Update `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/product_catalog
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
  
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: validate  # or update for production with care
```

Add MySQL driver to `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```



# 1. Build the project
cd -product-catalog-synchronizer/product-catalog-synchronizer
mvn clean package -DskipTests

# 2. Run it
java -jar target/product-catalog-synchronizer-0.0.1-SNAPSHOT.jar

# 3. You'll be prompted:
#    Enter file path: src/test/resources/file1.txt
#    Enter file path: src/test/resources/file2.txt
#    Enter file path: src/test/resources/file3.txt


# 4. Checking log:
Run log: log.txt


