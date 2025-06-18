# API-Scoring-App

A lightweight application that takes an OpenAPI (v3.x) spec (in YAML or JSON), evaluates its structure and content against industry best practices, and produces a comprehensive scorecard (0–100) with actionable feedback.

## Features

- Accepts OpenAPI v3.x specifications in YAML or JSON format
- Send spec directly or via URI/local file path
- Analyzes API specs for best practices and common pitfalls
- Generates a scorecard (0–100) with detailed, actionable feedback

## Installation

```bash
git clone https://github.com/Cimbir/API-Scoring-App.git
cd api-scoring-app
```
   
## How to run

### Command Line

```bash
cd core
./gradlew bootRun
```
### Intellij

1. Open in IntelliJ IDEA
2. Build Gradle project
   - Ensure you have the Gradle plugin installed
   - Use the Gradle tool window to run `bootRun` task
3. Run the `CoreApplication` class

## API Endpoints

### Endpoint Documentation: POST `/score-input`

This endpoint accepts an OpenAPI specification in JSON or YAML format and returns a scorecard with detailed feedback.

#### Request

- **URL**: `http://localhost:8080/score-input`
- **Method**: `POST`
- **Content-Type**: `application/json` or `application/x-yaml`
- **Body**: Raw OpenAPI specification in JSON or YAML format.

#### Example Request (JSON)

```bash
curl -X POST http://localhost:8080/score-input \
  -H "Content-Type: application/json" \
  --data-binary @path/to/openapi-spec.json
```

#### Example Request (YAML)

```bash
curl -X POST http://localhost:8080/score-input \
  -H "Content-Type: application/x-yaml" \
  --data-binary @path/to/openapi-spec.yaml
```

#### Response

- **200 OK**: Returns the scorecard object with detailed feedback.
  ```json
  {
      "totalScore": 99,
      "grade": "A",
      "schemaScore": { },
      "descriptionScore": { },
      "pathsScore": { },
      "responseScore": { },
      "exampleScore": { },
      "securityScore": { },
      "bestPracticesScore": { }
  }
  ```

- **400 Bad Request**: Invalid OpenAPI JSON or YAML.
  ```json
  {
      "message": "Invalid OpenAPI JSON or YAML",
      "details": "details about the specific parsing error"
  }
  ```

- **500 Internal Server Error**: Unexpected server error.
  ```json
  {
      "message": "Internal Server Error",
      "details": "details about the server error"
  }
  ```
### Endpoint Documentation: POST `/score-uri-or-local`

This endpoint accepts a URI or a local file path pointing to an OpenAPI specification and returns a scorecard with detailed feedback.

#### Request

- **URL**: `http://localhost:8080/api/scoring/score-uri-or-local`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Body**: A string containing the URI or local file path to the OpenAPI specification.

#### Example Request

```bash
curl -X POST http://localhost:8080/api/scoring/score-uri-or-local \
  -H "Content-Type: application/json" \
  -d 'https://example.com/openapi-spec.json'
```

#### Response

- **200 OK**: Returns the scorecard object with detailed feedback.
  ```json
  {
      "totalScore": 95,
      "grade": "A",
      "schemaScore": { },
      "descriptionScore": { },
      "pathsScore": { },
      "responseScore": { },
      "exampleScore": { },
      "securityScore": { },
      "bestPracticesScore": { }
  }
  ```

- **400 Bad Request**: Invalid URI or local file path.
  ```json
  {
      "message": "Invalid URI or Local",
      "details": "details about the specific error"
  }
  ```

- **500 Internal Server Error**: Unexpected server error.
  ```json
  {
      "message": "Internal Server Error",
      "details": "details about the server error"
  }
  ```

## Full Response Structure

```json
{
    "totalScore": 99,
    "grade": "A",
    "schemaScore": {
        "score": 19,
        "maxScore": 20,
        "categoryName": "Schema & Types",
        "issues": [
            {
                "location": "#/components/schemas/Station",
                "description": "Missing data type for schema (still infered as object)",
                "severity": "LOW",
                "suggestion": "Define an 'object' data type for the schema"
            }
        ],
        "strengths": []
    },
    "descriptionScore": {
        "score": 20,
        "maxScore": 20,
        "categoryName": "Descriptions & Documentation",
        "issues": [],
        "strengths": [
            "All API elements have proper descriptions"
        ]
    },
    "pathsScore": {
        "score": 15,
        "maxScore": 15,
        "categoryName": "Paths & Operations",
        "issues": [],
        "strengths": [
            "Consistent path naming convention detected: kebab-case"
        ]
    },
    "responseScore": {
        "score": 15,
        "maxScore": 15,
        "categoryName": "Response Codes",
        "issues": [],
        "strengths": [
            "All operations have appropriate response codes"
        ]
    },
    "exampleScore": {
        "score": 10,
        "maxScore": 10,
        "categoryName": "Examples & Samples",
        "issues": [],
        "strengths": [
            "Good coverage of request/response examples: 100%"
        ]
    },
    "securityScore": {
        "score": 10,
        "maxScore": 10,
        "categoryName": "Security",
        "issues": [],
        "strengths": [
            "Security schemes are defined"
        ]
    },
    "bestPracticesScore": {
        "score": 10,
        "maxScore": 10,
        "categoryName": "Best Practices",
        "issues": [],
        "strengths": [
            "API version is specified"
        ]
    }
}
```

### Score Categories

| Category | Max Points |
|----------|------------|
| Schema & Types | 20 |
| Descriptions & Documentation | 20 |
| Paths & Operations | 15 |
| Response Codes | 15 |
| Examples & Samples | 10 |
| Security | 10 |
| Best Practices | 10 |
| **Total** | **100** |

### Scoring Breakdown

- **Schema & Types**: Evaluates the structure and data types for
  - Schemas and Components
  - Request and Response bodies
- **Descriptions & Documentation**: Checks for comprehensive descriptions for
  - Overall API
  - Operations
  - Parameters
  - Requests
  - Responses
  - Schemas
- **Paths & Operations**: Assesses the organization and naming conventions for
  - Naming consistency
  - Crud operations
  - Path overlap
- **Response Codes**: Validates the use of appropriate HTTP response codes for
  - Success responses
  - Error responses
  - Default responses
- **Examples & Samples**: Reviews the presence and quality of examples for
  - Request bodies
  - Response bodies
- **Security**: Checks for security measures in place, such as
  - Security schemes
  - Operation-level security
  - Global security definitions
- **Best Practices**: Evaluates adherence to best practices, including
  - Versioning
  - Servers array
  - Tags usage
  - Component reusability
  - Operation IDs

### Grade Scale

| Grade | Score Range |
|-------|------------|
| A     | 91-100     |
| B     | 81-90      |
| C     | 71-80      |
| D     | 61-70      |
| E     | 51-60      |
| F     | 0-50       |

### Issue Severity

- **LOW**: Minor improvements
- **MEDIUM**: Important fixes needed
- **HIGH**: Critical issues

### Fields

- `totalScore`: Overall quality score (0-100)
- `grade`: Letter grade based on total score
- `issues[]`: Array of identified problems
- `strengths[]`: Array of positive findings
- `location`: JSONPath to the issue location
- `severity`: Issue importance level
- `suggestion`: Recommended fix

## Design Decisions

### Framework and Language
- **Spring Boot**: Chosen for its simplicity and ability to quickly build RESTful APIs.
- **Java**: Selected for its robustness, widespread use, and compatibility with Spring Boot.

### Build Tool
- **Gradle**: Used for dependency management and build automation due to its flexibility and performance.

### OpenAPI Parsing
- **Swagger Parser**: Utilized to parse and validate OpenAPI specifications (v3.x) in JSON or YAML format.

### Scoring Logic
- **Modular Scoring System**: Each category (e.g., Schema, Paths, Security) is scored independently to provide detailed feedback.
- **Weighted Scoring**: Categories are assigned weights to reflect their importance in API design.

### Error Handling
- **Custom Exceptions**: `OpenAPILoadException` and other specific exceptions are used for better error reporting.
- **Standardized Error Responses**: All errors return structured JSON responses with `message` and `details` fields.

### API Endpoints
- **POST `/score-input`**: Accepts raw OpenAPI specs in JSON or YAML format.
- **POST `/score-uri-or-local`**: Accepts a URI or local file path pointing to an OpenAPI spec.

### Response Structure
- **Scorecard Object**: Includes `totalScore`, `grade`, `issues`, and `strengths` for detailed feedback.
- **Severity Levels**: Issues are categorized as LOW, MEDIUM, or HIGH for prioritization.

### Deployment
- **Local Development**: Runs on `localhost:8080` for testing, debugging and ease of use.

### Testing
- **JUnit**: Used for unit testing the scoring logic and API endpoints.
- **Unit Tests**: Ensure each scoring category works as expected and handles general cases.

## Additional Notes

- Due to the limited time and resources, the testing coverage of the application is not exhaustive. However, the core functionality has been tested to ensure it meets the requirements.
- The application could be decomposed into smaller services, but due to the scale of the project, I decided to keep it compact
