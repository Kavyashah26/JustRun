# AuthService


---

## Features

* User registration with email and username validation
* Secure password storage using Spring Security's `PasswordEncoder`
* JWT-based stateless authentication
* Integration with AWS DynamoDB

---

## Technologies Used

* Spring Boot
* Spring Security
* JWT (io.jsonwebtoken)
* AWS SDK (DynamoDB)
* Maven
* Lombok

---

## API Endpoints

### Register User

`POST /api/auth/register`

```json
{
  "username": "your_username",
  "email": "your_email@example.com",
  "password": "your_password"
}
```

### Login User

`POST /api/auth/login`

```json
{
  "username": "your_username",
  "password": "your_password"
}
```

Response (Both register & login):

```json
{
  "token": "jwt_token_here",
  "userId": "user_id",
  "username": "your_username",
  "email": "your_email@example.com"
}
```

---




## Configuration (application.properties)

```
# Server Configuration
server.port=8080

# JWT Configuration
jwt.secret=your_jwt_secret
jwt.expiration=86400000 # 1 day in ms

# AWS Configuration
aws.accessKey=your_access_key
aws.secretKey=your_secret_key
aws.region=your_region

# Logging
logging.level.root=INFO
logging.level.org.springframework.web=INFO
logging.level.org.springframework.security=INFO
```

