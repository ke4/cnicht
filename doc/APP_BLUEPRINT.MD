# **Cnicht: Full-Stack TDD Job Application Tracker Blueprint & Step-by-Step Plan**

This master plan establishes the step-by-step functional use cases, TDD criteria, database schema migrations, local Kubernetes configurations, and automated CI/CD roadmaps for **Cnicht**. It is optimized to be used as a sequence of direct instructions for AI coding agents [cite: 0].

## **1. Project Architecture & Monorepo Structure**

Cnicht is structured as a decoupled monorepo pairing a Java Spring Boot backend with a React SPA frontend.1 To eliminate Cross-Origin Resource Sharing (CORS) complexity and enhance security, the React application is compiled directly into the Spring Boot static classpath during the packaging phase.1 This bundles both components into a single, cohesive executable JAR.1

```
Cnicht/ (Monorepo Root)  
├──.github/workflows/ci-cd.yml          <-- GitHub Actions Automation  
├── gradle/wrapper/                     <-- Gradle wrapper files  
├── build.gradle                        <-- Root Groovy build script  
├── settings.gradle                     <-- Monorepo settings script  
├── k8s/                                <-- Kubernetes Deployment Manifests  
├── backend/                            <-- Spring Boot 4.1.0 / Java 25 Subproject [2]  
└── frontend/                           <-- React 19 / Vite / Cypress Subproject [1, 3]
```

### **Feature-Modular Packaging Conventions**

The Java backend follows a strict **Feature-by-Feature Modular Package Layout** rather than grouping unrelated classes into generic global folders. Developers and AI agents must adhere to the following directory rules:

1. **Module Investigation Check:** Before implementing any new feature, the agent must inspect the folder structure to see if a related module directory (e.g., co.uk.karel.dev.Cnicht.auth, co.uk.karel.dev.Cnicht.application) already exists.
2. **Creating New Modules:** If a module package is missing, the agent must create the package directory under the base package: co.uk.karel.dev.Cnicht.<module-name>.
3. **Module Package Structure:** Each module package must isolate its related classes within these standard sub-packages:
  * co.uk.karel.dev.Cnicht.<module-name>.controller (REST Controllers)
  * co.uk.karel.dev.Cnicht.<module-name>.service (Business Services)
  * co.uk.karel.dev.Cnicht.<module-name>.repository (Spring Data JPA Repositories)
  * co.uk.karel.dev.Cnicht.<module-name>.dto (Request/Response Records)
  * co.uk.karel.dev.Cnicht.<module-name>.entity (JPA Domain Entities)

### **Database Repository Interface Rules**

1. **Zero Raw SQL Constraint:** Direct string-concatenated SQL queries and raw, manual JDBC template operations inside Java service code are strictly forbidden to prevent SQL injection vulnerabilities and keep the persistence layer database-agnostic.
2. **Spring Data Repositories:** All database interactions must extend Spring Data interfaces (such as JpaRepository or custom interfaces extending Repository).
3. **Derived Query Methods & @Query Support:** Developers should favor Spring Data's derived query method names (e.g., findByEmail(String email)) for simple lookup and existence checks. For complex multi-table queries, reporting views, or deep relational joins, the **@Query annotation (using JPQL/HQL, or native SQL if absolutely necessary and there is no easier Spring Data way to express it) is explicitly enabled** and supported.

### **Platform Baselines & Tooling**

* **Backend Runtime:** **Java 25 (LTS)** [cite: 1] with **Spring Boot 4.1.0** [cite: 22].
* **Build Tool:** **Gradle (Groovy DSL)** configured in multi-project mode [cite: 17, 19].
* **Database:** **PostgreSQL** with schema management run via **Flyway** [cite: 4, 5].
* **Frontend Web:** **React 19** bundled via **Vite** [cite: 15].
* **Testing Engines:** **JUnit 6** and **MockMvc** [cite: 29] (using modern @MockitoBean structures [cite: 32]) on the backend; **Cypress** [cite: 23] for both isolated **Component Testing** [cite: 23] and full browser **End-to-End (E2E) testing** [cite: 25, 26] on the frontend.

#### **Root Build File: settings.gradle**

Groovy  
rootProject.name = 'Cnicht'  
include 'backend'

#### **Root Build File: build.gradle**

Groovy  
plugins {  
id 'org.springframework.boot' version '4.1.0' apply false  
id 'io.spring.dependency-management' version '1.1.7' apply false  
id 'com.github.node-gradle.node' version '7.1.0' apply false  
}

allprojects {  
group = 'co.uk.karel.dev.Cnicht'  
version = '1.0.0-SNAPSHOT'  
}

## **2. Relational Database & Spreadsheet Mapping**

To transition your flat job tracking spreadsheet cleanly to PostgreSQL, the database is designed in Third Normal Form (3NF) to eliminate duplication.4 To accommodate multiple interview rounds per job application, a dedicated interviews table is introduced, linked via foreign keys.4

### **Database Table Relationships**

                               ┌─────────────────┐  
                               │      users      │  
                               └────────┬────────┘  
                                        │ 1  
                                        │  
                                        │ 1..*  
                               ┌────────┴────────┐  
                               │job_applications │  
                               └────────┬────────┘  
                                        │ 1  
                                        │  
                                        │ 1..*  
                               ┌────────┴────────┐  
                               │   interviews    │  
                               └─────────────────┘

### **Relational Entity Mapping**

#### **Table 1: users**

* id (BIGSERIAL PRIMARY KEY)
* email (VARCHAR(255) UNIQUE NOT NULL)
* password_hash (VARCHAR(255) NOT NULL)
* created_at (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)

#### **Table 2: job_applications**

* id (BIGSERIAL PRIMARY KEY)
* application_id (VARCHAR(50) UNIQUE NOT NULL) -- Business reference (e.g., APP-2026-0001) 4
* user_id (BIGINT NOT NULL, FK to users(id) ON DELETE CASCADE)
* company (VARCHAR(255) NOT NULL)
* job_title (VARCHAR(255) NOT NULL)
* interview_stage (VARCHAR(100) NOT NULL) -- Overall current status
* application_date (DATE NOT NULL)
* hr_agent (VARCHAR(255))
* recruiter (VARCHAR(255))
* url (VARCHAR(1024))
* company_profile (TEXT)
* created_at (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)
* updated_at (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)

#### **Table 3: interviews**

* id (BIGSERIAL PRIMARY KEY)
* job_application_id (BIGINT NOT NULL, FK to job_applications(id) ON DELETE CASCADE)
* interview_stage (VARCHAR(100) NOT NULL) -- Specific round name (e.g., "Technical Screening") 4
* interview_date (TIMESTAMP NOT NULL)
* details (TEXT) -- Free-text details/notes 4
* interviewer (VARCHAR(255))
* url (VARCHAR(1024)) -- Link to meeting or notes 1
* is_it_last_stage (BOOLEAN NOT NULL DEFAULT FALSE) -- Flag indicating if this is the final round

*Index Strategy:* Unique index applied to job_applications(application_id) to guarantee $O(1)$ search complexity.

## **3. API-First HTTP Contract Specifications**

Following an API-first methodology, we declare our endpoints, payload structures, and expected response matrices before writing application logic.

### **Endpoint Matrix**

| Method | Path | Request Body | Response Success | Response Error |
| :---- | :---- | :---- | :---- | :---- |
| **POST** | /api/auth/register | {"email":"str","password":"str"} | 201 Created (User DTO) | 400 Bad Request / 409 Conflict (Duplicate Email) |
| **POST** | /api/auth/login | {"username":"str","password":"str"} | 200 OK (Sets Secure Session Cookie) | 401 Unauthorized |
| **POST** | /api/auth/logout | None | 200 OK (Invalidates Cookie) | None |
| **GET** | /api/applications | None | 200 OK (Array of Application Objects) | 401 Unauthorized |
| **POST** | /api/applications | {"applicationId":"str","company":"str","jobTitle":"str","interviewStage":"str","applicationDate":"YYYY-MM-DD","hrAgent":"str","recruiter":"str","url":"str","companyProfile":"str"} | 201 Created (Application Object) | 400 Bad Request / 409 Conflict |
| **PUT** | /api/applications/{appId} | Same as POST (Full Update) | 200 OK (Updated Object) | 400 Bad Request / 404 Not Found |

## **4. BFF-Emulated Same-Origin Security Pattern**

Cnicht implements a Backend-for-Frontend (BFF) security posture [cite: 3].

* **No Access Tokens in Browser:** Client-side JavaScript is treated as a hostile environment [cite: 3]. Access tokens never touch localStorage or session variables [cite: 3].
* **HttpOnly Cookies:** Authentication is verified using server-side session cookies flagged with HttpOnly, Secure, and SameSite=Strict.6
* **CORS Removal:** By packaging the compiled SPA assets directly into the backend class-path under /static/, the browser reads the UI and calls the APIs on the exact same origin (e.g., http://localhost:8080), bypassing CORS-related vulnerabilities [cite: 15, 115].
* **CSRF Protection:** State-changing methods (POST, PUT, DELETE) require a double-submit CSRF cookie token exchange, coordinated automatically between Axios and Spring Security [cite: 5, 85].

## **5. Step-by-Step TDD Implementation Plan**

Every coding phase must follow strict **Red-Green-Refactor** loops.7

### **Task 1: INFRASTRUCTURE SETUP (KUBERNETES & CI/CD)**

#### **Use Case 1.1: Local Kubernetes Deployment**

* **Actor:** DevOps Engineer / Local Runner
* **Preconditions:** monorepo layout exists.
* **Postconditions:** PostgreSQL database runs as a StatefulSet with persistent storage.8 The Spring Boot app runs as a multi-replica deployment exposed via a LoadBalancer service.
* **Basic Flow:**
  1. Apply database manifests containing StatefulSet and headless Service.8
  2. Build local Docker container inside the Kubernetes hypervisor (such as minikube).10
  3. Deploy the Spring Boot application configuration pointing to the local cluster database DNS.10

#### **Use Case 1.2: GitHub Actions Automated CI/CD**

* **Actor:** GitHub Runner
* **Preconditions:** Code is pushed to main or develop.
* **Postconditions:** Validates compilation, executes unit and component testing, builds JRE production image, and verifies deployment structures.
* **Basic Flow:**
  1. Set up JDK 25 and Node.js 20 runner environments.
  2. Install NPM modules and run Cypress component tests headless.3
  3. Execute Gradle JUnit backend test suite.
  4. Package final deployment JAR and trigger Docker compilation.

#### **TDD Testing & Verification Criteria**

* **Integration Red Phase:** Create an end-to-end Cypress test (cypress/e2e/job_tracking_flow.cy.js) that runs against a clean environment.3 It asserts a complete, failing user journey (Registration, Login, Add, List, Edit, Logout) because no code is implemented yet.
* **Verification Check:** Verify that pushing this placeholder code triggers the GitHub Actions CI/CD pipeline, compiles the base layout, and fails at the test execution steps as expected.

### **Task 2: SAME-ORIGIN AUTHENTICATION & ACCOUNT LIFECYCLE (REGISTRATION, LOGIN, & LOGOUT)**

#### **Use Case 2.1: Relational Schema DB Initialization**

* **Actor:** System (Flyway Migration Engine) [cite: 4, 5]
* **Preconditions:** Target database exists and is reachable.
* **Postconditions:** Table structures for users are initialized with exact columns, indexes, and user-scoped foreign keys.
* **Basic Flow:** On application startup, Flyway reads the migration directory and executes the init SQL script to configure the database schema.

#### **Use Case 2.2: User Account Creation (Registration)**

* **Actor:** Unauthenticated User
* **Preconditions:** Target email address is not already registered in the system.
* **Postconditions:** A new user record is persisted in the PostgreSQL database with a cryptographically hashed password (BCrypt, strength 12).12
* **Basic Flow:**
  1. User navigates to the signup/registration screen.
  2. User submits their email address and a plaintext password.
  3. Backend checks if a user with that email already exists. If yes, it returns 409 Conflict.
  4. Backend encodes the password using BCrypt and persists the record to the database via UserRepository.
  5. Backend returns 201 Created with a safe User DTO (excluding password).

#### **Use Case 2.3: Secure User Authentication (Login)**

* **Actor:** Unauthenticated User
* **Preconditions:** User record exists in the PostgreSQL database with a pre-hashed password [cite: 9].
* **Postconditions:** Valid server-side session is created. The browser receives an encrypted session cookie (JSESSIONID) flagged as HttpOnly, Secure, and SameSite=Strict [cite: 3].
* **Basic Flow:**
  1. User navigates to the login screen.
  2. User submits their email and password.
  3. Backend validates credentials using BCrypt (strength 12) [cite: 9, 68].
  4. Upon successful validation, the backend generates a session, updates the security context, and returns a 200 OK response with the session cookie.12
  5. On failure, the backend returns a 401 Unauthorized status.12

#### **Use Case 2.4: Authenticated Status Handshake**

* **Actor:** Browser Application
* **Preconditions:** Active session exists.
* **Basic Flow:**
  1. React app boots and makes a silent request to /api/auth/status.
  2. Backend reads the incoming session cookie, verifies its validity, and returns the current user's email with a 200 OK status.
  3. If the session cookie is missing or invalid, the backend returns a 401 Unauthorized response.

#### **Use Case 2.5: Secure Session Termination (Logout)**

* **Actor:** Authenticated User
* **Preconditions:** User is logged in with an active session.
* **Postconditions:** Server-side session is invalidated. Cookie is deleted from browser storage.13
* **Basic Flow:**
  1. User clicks "Log Out" on the dashboard.
  2. React client submits a POST request to /api/auth/logout.
  3. Backend invalidates the HTTP session on the server-side, deletes the JSESSIONID cookie from the response header, and returns 200 OK.13

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase (Modular package: co.uk.karel.dev.Cnicht.auth):**
  1. Create RegistrationControllerTest to assert that submitting a valid POST to /api/auth/register returns 201 Created. Test that registering a duplicate email returns 409 Conflict.
  2. Create an integration test class (AuthControllerTest) to verify that accessing /api/applications without a session cookie returns 401 Unauthorized. Assert that sending valid credentials to the login route returns 200 OK and writes a non-null cookie, while invalid inputs return 401.10
  3. **** **Log Out TDD Verification:** Assert that submitting a POST request to /api/auth/logout with an active session successfully destroys the server-side session, invalidates the JSESSIONID cookie (by adding a Set-Cookie header with maxAge=0), and returns 200 OK.
* **Frontend Red Phase:**
  1. Write a Cypress component spec (RegisterForm.cy.jsx) that mounts the registration form and verifies:
    * Form blocks submission if email format is invalid or password is empty.
    * Submission triggers the callback with valid JSON format.
  2. Write a Cypress component spec (LoginForm.cy.jsx) that mounts the login form and verifies:
    * Submission is blocked and displays an error message if fields are left blank.3
    * Submission executes successfully when valid credentials are typed in.3
  3. **** **Log Out TDD Verification:** Write a Cypress spec (such as in App.cy.jsx or a navigation header spec) verifying that clicking the "Log Out" button triggers a POST request to /api/auth/logout, clears the React authenticated state context, and redirects the viewport back to the /login route.
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Registration & Auth tests (including Logout verification), and compiles successfully.

### **Task 3: ADD A JOB APPLICATION (WITH INTERVIEW DATA)**

#### **Use Case 3.1: Relational Schema DB Initialization**

* **Actor:** System (Flyway Migration Engine) [cite: 4, 5]
* **Preconditions:** Target database exists and is reachable.
* **Postconditions:** Table structures for users, job_applications, and interviews are initialized with exact columns, indexes, and user-scoped foreign keys.4
* **Basic Flow:** On application startup, Flyway reads the migration directory and executes the init SQL script to configure the database schema.14

#### **Use Case 3.2: Add Job Application Record with Optional First Interview Stage**

* **Actor:** Authenticated User
* **Preconditions:** User has a valid authentication session.
* **Postconditions:** A new application record is saved in the database, bound securely to the authenticated user's ID. If interview data is provided, a corresponding record is created in the interviews table.4
* **Basic Flow:**
  1. User fills out the Job Application Form (providing Application ID, Company, Job Title, Stage, Application Date, and optional contact details).
  2. Optionally, the user inputs details for the first scheduled interview stage (Interview Stage, Date, Details, Interviewer, URL, and a toggle for is_it_last_stage).
  3. User submits the form.
  4. React app reads the CSRF token from the browser cookie and appends it to the request header.13
  5. Backend validates inputs (ensuring that company, job_title, and application_id are not blank).
  6. Backend validates that the application_id is unique.4
  7. Backend saves both the job application and the nested interview stage (if provided), linking them to the authenticated user's ID, and returns a 201 Created response.

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase (Modular package: co.uk.karel.dev.Cnicht.application):** Write JobApplicationControllerTest using MockMvc and @WithMockUser to assert that:
  1. Requests without a valid CSRF token are blocked with a 403 Forbidden status.13
  2. Requests with blank required fields return 400 Bad Request.1
  3. Requests with valid inputs (including nested interview rounds) return 201 Created and the saved entity containing the unique business application_id and relational interview details.1
* **Frontend Red Phase:** Write a Cypress component spec (JobApplicationForm.cy.jsx) that mounts the form and verifies that:
  1. Leaving required fields empty triggers validation warnings on the UI.3
  2. Clicking save with valid inputs invokes the save callback with the expected JSON payload structures including nested interview parameters.3
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Application creation tests, and compiles successfully.

### **Task 4: LIST ALL JOB APPLICATIONS (WITH INTERVIEW DATA)**

#### **Use Case 4.1: Scoped Query Fetching with Interview Collection**

* **Actor:** Authenticated User
* **Preconditions:** User is logged in.
* **Postconditions:** System returns only the applications and their nested interview rounds owned by the logged-in user.
* **Basic Flow:**
  1. User opens the dashboard.
  2. React client makes a GET request to /api/applications.
  3. Backend retrieves the current user's ID from the security context, queries the database for records matching that ID, eagerly loads the child interviews table data, and returns them in a JSON array.

#### **Use Case 4.2: Frontend Application Grid Representation**

* **Actor:** Authenticated User
* **Preconditions:** Scoped records are loaded.
* **Basic Flow:**
  1. React app processes the JSON array.
  2. App renders a grid listing all applications with their company, title, date, and current stage badge.16
  3. Each card displays an expandable details section showing its associated interview timeline: round stage name, scheduled date, interviewer, meeting URL, and whether it's marked as the final stage.16
  4. If the list is empty, a message reading "No job applications tracked yet" is displayed.

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase (Modular package: co.uk.karel.dev.Cnicht.application):** Write a test asserting that a GET call to /api/applications returns a 200 OK JSON array containing only records that belong to the mocked user context, with child interview objects correctly populated, ensuring strict data isolation.
* **Frontend Red Phase:** Write a Cypress component spec (ApplicationsList.cy.jsx) that mounts the listing view and asserts that:
  1. An empty list displays the empty state message.3
  2. A populated list correctly renders the Company Name, Job Title, Stage, and unique Application ID.3
  3. Clicking to expand an application reveals the nested interview rounds showing the round stage, date, details, and interviewer.3
  4. **** **Dashboard Logout Button Test:** Assert that a "Log Out" button is rendered on the dashboard view, and clicking it triggers the logout callback prop to securely invoke session destruction.
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Application Listing tests (including dashboard logout trigger tests), and compiles successfully.

### **Task 5: EDIT A JOB APPLICATION (AND ASSOCIATED INTERVIEWS)**

#### **Use Case 5.1: Record Mutation & Integrity Check**

* **Actor:** Authenticated User
* **Preconditions:** The job application record exists and is owned by the logged-in user.
* **Postconditions:** The target record is updated in the database without altering the owner or the unique business identifier.
* **Basic Flow:**
  1. User selects an application card and clicks "Edit".
  2. System pre-fills the form with the current details.
  3. User modifies a field (e.g., updates the status to INTERVIEWING) or adds/updates an interview round, and clicks "Update".
  4. React client submits a PUT request to /api/applications/{appId}.
  5. Backend verifies that the user owns the record. If not, it returns 403 Forbidden or 404 Not Found.
  6. Backend updates the records in both tables and returns a 200 OK status with the updated record.

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase (Modular package: co.uk.karel.dev.Cnicht.application):** Write integration tests asserting that:
  1. **** Sending a PUT request with valid updated data for an owned job application successfully updates the record (including modifying existing or appending new interviews) and returns 200 OK.
  2. Sending a PUT request to a non-existent application_id returns 404 Not Found.
  3. Trying to edit an application owned by a different user returns 403 Forbidden or 404 Not Found.
* **Frontend Red Phase:** Write a Cypress component spec (EditApplication.cy.jsx) verifying that:
  1. Form inputs are correctly pre-filled with the current values.3
  2. **** Submitting the form with updated, valid details triggers the update API callback with correct parameter structures and the target record ID.3
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Application editing tests, passes the complete E2E system suite, and builds successfully.

## **6. Local Kubernetes Cluster Configuration**

To run and verify **Cnicht** locally, we deploy a persistent PostgreSQL database using a Kubernetes StatefulSet and expose our packaged full-stack application.


                │  
                ▼ (Port 80)  
        [ Cnicht-service ] (LoadBalancer)  
                │  
                ▼ (Port 8080)  
        [ Cnicht-deployment ] (App Replica Pods)  
                │  
                ▼ (Port 5432)  
      [ postgres-service ] (ClusterIP headless)  
                │  
                ▼  
       [ postgres-db-0 ] (StatefulSet)

### **Database Stack: k8s/postgres-db.yaml (StatefulSet Configuration)**

YAML  
apiVersion: v1  
kind: Namespace  
metadata:  
name: Cnicht  
---  
apiVersion: v1  
kind: ConfigMap  
metadata:  
name: postgres-config  
namespace: Cnicht  
data:  
POSTGRES_DB: Cnichtdb  
POSTGRES_USER: Cnichtuser  
---  
apiVersion: v1  
kind: Secret  
metadata:  
name: postgres-secret  
namespace: Cnicht  
type: Opaque  
stringData:  
POSTGRES_PASSWORD: "LocalSecureDbPassword" # Decrypted for plain context  
---  
apiVersion: v1  
kind: Service  
metadata:  
name: postgres-service  
namespace: Cnicht  
spec:  
clusterIP: None # Headless service for StatefulSet identity resolution  
selector:  
app: postgres-db  
ports:  
- port: 5432  
---  
apiVersion: apps/v1  
kind: StatefulSet  
metadata:  
name: postgres-db  
namespace: Cnicht  
spec:  
serviceName: postgres-service  
replicas: 1  
selector:  
matchLabels:  
app: postgres-db  
template:  
metadata:  
labels:  
app: postgres-db  
spec:  
containers:  
- name: postgres  
image: postgres:17-alpine  
envFrom:  
- configMapRef:  
name: postgres-config  
env:  
- name: POSTGRES_PASSWORD  
valueFrom:  
secretKeyRef:  
name: postgres-secret  
key: POSTGRES_PASSWORD  
ports:  
- containerPort: 5432  
volumeMounts:  
- name: pg-storage  
mountPath: /var/lib/postgresql/data  
volumeClaimTemplates:  
- metadata:  
name: pg-storage  
spec:  
accessModes:  
- ReadWriteOnce  
resources:  
requests:  
storage: 2Gi # Scaled down for lightweight local deployments

### **App Stack Deployment: k8s/Cnicht-deployment.yaml**

YAML  
apiVersion: apps/v1  
kind: Deployment  
metadata:  
name: Cnicht-app  
namespace: Cnicht  
spec:  
replicas: 2  
selector:  
matchLabels:  
app: Cnicht-app  
template:  
metadata:  
labels:  
app: Cnicht-app  
spec:  
containers:  
- name: Cnicht-app  
image: Cnicht-app:latest # Pulled from the minikube local registry  
imagePullPolicy: Never # Forces use of locally built image  
ports:  
- containerPort: 8080  
env:  
- name: SPRING_DATASOURCE_URL  
value: "jdbc:postgresql://postgres-service.Cnicht.svc.cluster.local:5432/Cnichtdb"  
- name: SPRING_DATASOURCE_USERNAME  
value: "Cnichtuser"  
- name: SPRING_DATASOURCE_PASSWORD  
valueFrom:  
secretKeyRef:  
name: postgres-secret  
key: POSTGRES_PASSWORD

### **App Stack Service: k8s/Cnicht-service.yaml**

YAML  
apiVersion: v1  
kind: Service  
metadata:  
name: Cnicht-service  
namespace: Cnicht  
spec:  
type: LoadBalancer # Dynamically routes local traffic via minikube tunnel  
selector:  
app: Cnicht-app  
ports:  
- protocol: TCP  
port: 80  
targetPort: 8080

## **7. GitHub Actions CI/CD Pipeline**

The pipeline automates compilation, executes both unit and end-to-end testing suites, builds the production container, and coordinates the deployment tasks to a Kubernetes cluster.

### **.github/workflows/ci-cd.yml**

YAML  
name: Cnicht CI/CD pipeline

on:  
push:  
branches: [ "main", "develop" ]  
pull_request:  
branches: [ "main" ]

jobs:  
build-and-test:  
runs-on: ubuntu-latest  
steps:  
- name: Checkout monorepo codebase  
uses: actions/checkout@v4

      - name: Setup Java 25 Environment  
        uses: actions/setup-java@v4  
        with:  
          java-version: '25'  
          distribution: 'temurin'  
          cache: 'gradle'

      - name: Setup Node.js 20 Environment  
        uses: actions/setup-node@v4  
        with:  
          node-version: '20'  
          cache: 'npm'  
          cache-dependency-path: './frontend/package.json'

      - name: Install Frontend Dependencies & Run Cypress Component Tests  
        working-directory:./frontend  
        run: |  
          npm install  
          npx cypress run --component # Executes headless React unit component tests 

      - name: Compile and Test Backend Code  
        run:./gradlew :backend:test --info

      - name: Build Executable JAR with Compiled Assets  
        run:./gradlew :backend:bootJar

      # Build Docker container and push to the local/private image registry  
      - name: Set up Docker Buildx  
        uses: docker/setup-buildx-action@v3

      - name: Build and Package Container Image  
        uses: docker/build-push-action@v5  
        with:  
          context:.  
          file:./backend/Dockerfile  
          push: false # Set to true when integrating with remote registries  
          tags: Cnicht-app:${{ github.sha }}

      # Kubernetes validation block  
      - name: Setup Kubectl  
        uses: azure/setup-kubectl@v3  
        with:  
          version: 'latest'

      - name: Deploy to Kubernetes Cluster  
        if: github.ref == 'refs/heads/main'  
        run: |  
          # Requires KUBECONFIG secrets to be configured in GitHub  
          echo "${{ secrets.KUBECONFIG }}" > kubeconfig.yaml  
          export KUBECONFIG=kubeconfig.yaml  
          kubectl apply -f k8s/postgres-db.yaml  
          kubectl apply -f k8s/Cnicht-deployment.yaml  
          kubectl apply -f k8s/Cnicht-service.yaml  
          kubectl set image deployment/Cnicht-app Cnicht-app=Cnicht-app:${{ github.sha }} -n Cnicht

## **8. Multi-Agent AI Prompts for Code Generation**

These targeted, role-based prompts are designed to be copied and pasted directly into your AI developer assistant (such as Cursor, Claude Code, or ChatGPT) to generate the code for each phase.

### **PROMPT 1: MONOREPO FOUNDATION & KUBERNETES SETUP (Role: Lead Architect)**

Context: You are a Lead Software Architect specializing in Spring Boot and React monorepos.  
Task: Configure the root directory of a project named 'Cnicht' using Java 25 (LTS) and Gradle (Groovy DSL) with a multi-project structure. Create the Kubernetes manifests to deploy this application locally.

Requirements:  
1. Create a root 'settings.gradle' file using Groovy DSL that defines the root project name as 'Cnicht' and includes the 'backend' subproject.  
2. Create a root 'build.gradle' file using Groovy DSL that defines the group 'co.uk.karel.dev.Cnicht' and version '1.0.0-SNAPSHOT'. Declare that we will use Spring Boot 4.1.0 and Java 25.  
3. Configure the gradle-node-plugin (com.github.node-gradle.node) version 7.1.0 inside 'backend/build.gradle' using Groovy DSL, targeting Node.js version 20.11.0, with download = true and '../frontend' as the nodeProjectDir. Set NpmTask 'buildFrontend' to run 'npm run build' inside the frontend project so that React assets are compiled into 'backend/src/main/resources/static'.  
4. Provide a complete enterprise-grade '.gitignore' file.  
5. Create 'k8s/postgres-db.yaml' to configure a persistent local PostgreSQL instance using a Kubernetes StatefulSet (image: postgres:17-alpine) with a Headless Service, a Secret for database credentials, and a VolumeClaimTemplate requesting a 2Gi volume.  
6. Create 'k8s/Cnicht-deployment.yaml' and 'k8s/Cnicht-service.yaml' to run the Spring Boot app with 2 replicas, pulling the image locally, injecting database credentials from the Postgres Secret, and exposing port 8080 via a LoadBalancer service.

### **PROMPT 2: GITHUB ACTIONS PIPELINE & E2E TESTING (Role: Lead Architect / QA Engineer)**

Context: You are a DevOps and QA Specialist focusing on CI/CD pipelines and Cypress E2E flows.  
Task: Configure a GitHub Actions workflow and write a comprehensive E2E integration test for 'Cnicht'.

Requirements:  
1. Create a GitHub Actions workflow '.github/workflows/ci-cd.yml'. It must set up JDK 25 and Node.js 20, run headless Cypress component tests inside the 'frontend/' folder, compile and run JUnit tests inside the 'backend/' folder, and compile the combined bootJar.  
2. Include a Docker build step in the workflow to package the application, using 'eclipse-temurin:25-jre-alpine' as the runtime base image. Include a mock Kubernetes apply block for deployment on main branch pushes.

### **PROMPT 3: FLYWAY SCHEMA INIT WITH INTERVIEWS & USER REGS (Role: Database Architect)**

Context: You are a Database Architect specializing in PostgreSQL and Flyway schema design.  
Task: Create the relational database migrations for the 'Cnicht' application to replace a flat tracking spreadsheet, support user registrations, and support multiple interview rounds.

Requirements:  
1. Write a Flyway migration file named 'V1__Init_Schema.sql' inside 'backend/src/main/resources/db/migration/'.  
2. Create a 'users' table with columns: 
- 'id' (BIGSERIAL PRIMARY KEY), 
- 'email' (VARCHAR(255) UNIQUE NOT NULL), 
- 'password_hash' (VARCHAR(255) NOT NULL), 
- 'created_at' (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)
3. Create a 'job_applications' table with columns:  
- 'id' (BIGSERIAL PRIMARY KEY)  
- 'application_id' (VARCHAR(50)) -- Business sometimes provides this identifier  
- 'user_id' (BIGINT NOT NULL, FK to users(id) ON DELETE CASCADE)  
- 'company' (VARCHAR(255) NOT NULL)  
- 'job_title' (VARCHAR(255) NOT NULL)  
- 'interview_stage' (VARCHAR(100) NOT NULL)  
- 'application_date' (DATE NOT NULL)  
- 'hr_agent', 'recruiter', 'url' (VARCHAR), 'company_profile' (TEXT)  
- 'created_at' (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)
- 'updated_at' (TIMESTAMPS NOT NULL DEFAULT CURRENT_TIMESTAMP)  
4. Create an 'interviews' table with columns:  
- 'id' (BIGSERIAL PRIMARY KEY)  
- 'job_application_id' (BIGINT NOT NULL, FK to job_applications(id) ON DELETE CASCADE)  
- 'interview_stage' (VARCHAR(100) NOT NULL) -- specific round name (e.g. "HR Screen", "Technical")  
- 'interview_date' (TIMESTAMP NOT NULL)  
- 'details' (TEXT) -- free-text field  
- 'interviewer' (VARCHAR(255))  
- 'url' (VARCHAR(1024))  
- 'is_it_last_stage' (BOOLEAN NOT NULL DEFAULT FALSE)  
5. Create indices on foreign keys and unique indexes on 'job_applications(application_id)'.
6. Configure 'backend/src/main/resources/application.properties' to enable Flyway, set migration locations, and configure the datasource connection to the local PostgreSQL instance.

### **PROMPT 4: TASK 2 - MODULE PACKAGING, SAME-ORIGIN SECURITY & USER ACCOUNT TDD (Role: TDD Backend Developer)**

Context: You are a TDD Backend Engineer specializing in Spring Security 7, Java 25, and MockMvc.  
Task: Implement user registration, cookie-based login, status check, and logout endpoints for 'Cnicht' using strict TDD.

Guidelines:  
1. PACKAGE MANAGEMENT RULE: Before writing any code, investigate if a related package exists. Since this relates to authentication and user configuration, find or create the module package: 'co.uk.karel.dev.Cnicht.auth'.  
2. Create sub-packages under the module like 'controller', 'service', 'repository', 'dto', and 'entity' if they are needed. You must organize your classes into these folders.  
3. PERSISTENCE RULE: Direct string-concatenated SQL queries and raw, manual JDBC template operations inside Java service code are strictly forbidden. You must rely on Spring Data Repository interfaces extending JpaRepository. Derived query method naming (e.g. 'findByEmail', 'existsByEmail') should be used for simple lookups. However, the use of Spring Data's '@Query' annotation is explicitly permitted and enabled if it is necessary for complex queries and there is no easier Spring Data way to express it.

Requirements:  
1. Write 'RegistrationControllerTest.java' inside the auth package to verify that POST '/api/auth/register' with valid parameters creates a user record and returns '201 Created', while registering a duplicate email returns '409 Conflict'.  
2. Write 'AuthControllerTest.java' asserting that accessing protected endpoints without a session cookie returns '401 Unauthorized'. Verify login attempts (POST to '/api/auth/login' with URL-encoded params) return '200 OK' and set secure session cookies on success, and '401' on failure.  
3. Write a test for POST '/api/auth/logout' verifying session invalidation on the server, secure destruction of the JSESSIONID session cookie (returning maxAge=0), and response code '200 OK'.  
4. Implement the controller, service, repository, and security filters to pass the tests. Enable session creation to 'IF_REQUIRED', use BCryptPasswordEncoder (strength 12), and use 'CookieCsrfTokenRepository.withHttpOnlyFalse()' to handle Axios CSRF validations. Flag session cookies as httpOnly, Secure, and SameSite=Strict.

### **PROMPT 5: TASK 2 - FRONTEND USER LIFECYCLE (REGISTRATION & LOGIN) SCREEN TDD (Role: TDD Frontend Developer)**

Context: You are a senior React 19 and Cypress developer practicing strict TDD.  
Task: Create the registration screen, login screen, and testing components for 'Cnicht'.

Requirements:  
1. Write a Cypress component test file 'RegisterForm.cy.jsx' using 'cy.mount()'. Assert that:  
- Clicking register with missing parameters or invalid email pattern displays validation messages and blocks submit.  
- Submitting valid signup information correctly fires the 'onRegister' callback parameter.  
2. Write a Cypress component test file 'LoginForm.cy.jsx' using 'cy.mount()'. Assert that:  
- Submitting empty inputs prompts an error reading "Email and password are required".  
- Submitting valid login criteria executes 'onLogin' with the target credentials.  
3. Write a Cypress component test verifying that clicking the "Log Out" button on the dashboard component triggers a POST request to '/api/auth/logout', clears client authentication state context, and redirects the user to the login screen.  
4. Write the React components 'LoginForm.jsx' and 'RegisterForm.jsx' in Tailwind CSS using 'data-cy' attributes for your Cypress test selectors. Create the states, handlers, and Axios configurations required to pass all component tests.
5. Create a complete Cypress E2E spec 'cypress/e2e/job_tracking_flow.cy.js' to verify the entire system flow (Login, Logout) against the running application.

### **PROMPT 6: TASKS 3, 4, 5 - MODULAR REPOS, APPLICATIONS & NESTED INTERVIEWS REST API TDD (Role: TDD Backend Developer)**

Context: You are a TDD Spring Boot Engineer specializing in Spring Data JPA and JSpecify null safety.  
Task: Create the API endpoints to create, list, and edit job applications and associated nested interviews using strict TDD.

Guidelines:  
1. PACKAGE MANAGEMENT RULE: Before writing any code, check if there is an existing related module folder. Find or create the module package: 'co.uk.karel.dev.Cnicht.application'. Organize your classes into: 'co.uk.karel.dev.Cnicht.application.controller', '.service', '.repository', '.dto', and '.entity'.  
2. PERSISTENCE RULE: Raw SQL string-concatenations inside Java code are banned. All query mechanisms must extend Spring Data Repository interfaces (like 'JpaRepository' or 'CrudRepository'). You are explicitly encouraged to use derived query method mappings where possible. However, you are fully permitted to use the '@Query' annotation (utilizing JPQL, HQL, or native SQL as needed) when implementing complex, multi-table joins or advanced operations that cannot easily be expressed via derived methods.

Requirements:  
1. Write JUnit 6 integration tests verifying:  
- POST '/api/applications' creates a new application record, supporting nested interview array data. If required fields like 'company' or 'application_id' are missing, return '400 Bad Request'.  
- POST requests without a valid CSRF token in the headers return '403 Forbidden'.  
- GET '/api/applications' retrieves only the records owned by the current authenticated user, eagerly mapping and loading the nested collection of interviews.  
- PUT '/api/applications/{appId}' updates the application details (including modifying existing or appending new interviews in the interviews table), returning '200 OK' for successful updates, '404 Not Found' if the record does not exist, or '403 Forbidden' if it belongs to a different user.  
2. Implement 'JobApplicationController.java', 'JobApplicationService.java', 'JobApplicationRepository.java', 'InterviewRepository.java', and matching JPA domain entities to successfully pass all tests.  
3. Apply JSpecify '@NullMarked' and '@Nullable' annotations to your request and response record DTOs to enforce compile-time null safety.

### **PROMPT 7: TASKS 3, 4, 5 - FRONTEND JOB TRACKING & INTERVIEW COMPONENTS TDD (Role: TDD Frontend Developer)**

Context: You are a React developer specializing in Cypress Component Testing and Axios API integration.  
Task: Create the components to add, list, and edit job applications and interviews in 'Cnicht' using strict TDD.

Requirements:  
1. Write Cypress component tests for 'JobApplicationForm.cy.jsx' (verifying fields for adding application metadata and optional nested first interview round, ensuring validation warnings trigger on empty required fields) and 'ApplicationsList.cy.jsx' (verifying empty states, record grid renders, the display of associated interview stages, and the rendering of a "Log Out" button on the dashboard header).  
2. Write component tests for 'EditApplication.cy.jsx' asserting that:  
- Form inputs are correctly pre-filled with the current application and interview values.  
- Submitting the form with updated, valid details triggers the update API callback with correct parameter structures.  
3. Implement the React components using React 19 and Tailwind CSS to pass these tests.  
4. Configure your Axios client globally in 'App.jsx' with 'withCredentials = true' and set up an interceptor to read the 'XSRF-TOKEN' cookie and append it to the 'X-XSRF-TOKEN' header for all state-changing requests.
5. Add the following actions to the E2E spec 'cypress/e2e/job_tracking_flow.cy.js' to verify the entire system flow against the running application: Add Job, List, Edit Job & interviews.

#### **Works cited**

1. Bundling React (Vite) with Spring Boot | Articles - Jessy, accessed on June 11, 2026, [https://www.jessym.com/articles/bundling-react-vite-with-spring-boot](https://www.jessym.com/articles/bundling-react-vite-with-spring-boot)
2. Test React components with Cypress - CircleCI, accessed on June 11, 2026, [https://circleci.com/blog/cypress-react-component-testing/](https://circleci.com/blog/cypress-react-component-testing/)
3. Free Job Tracker Spreadsheet Template - Softr, accessed on June 11, 2026, [https://www.softr.io/database-template/spreadsheet/job-tracker](https://www.softr.io/database-template/spreadsheet/job-tracker)
4. Free Job Application Tracker Spreadsheet Template - MaxOfJob, accessed on June 11, 2026, [https://maxofjob.com/job-application-tracker-spreadsheet](https://maxofjob.com/job-application-tracker-spreadsheet)
5. BFF security architecture: Surviving supply chain attacks | FusionAuth, accessed on June 11, 2026, [https://fusionauth.io/blog/backend-for-frontend-security-architecture](https://fusionauth.io/blog/backend-for-frontend-security-architecture)
6. Using MockMvc With SpringBootTest vs. Using WebMvcTest | Baeldung, accessed on June 11, 2026, [https://www.baeldung.com/spring-mockmvc-vs-webmvctest](https://www.baeldung.com/spring-mockmvc-vs-webmvctest)
7. FREE Job Application Tracker Spreadsheet - Dartassist, accessed on June 11, 2026, [https://www.dartassist.com/free-tools/job-application-tracker-spreadsheet](https://www.dartassist.com/free-tools/job-application-tracker-spreadsheet)
8. Java 26: Better Language, Better APIs, Better Runtime - Inside.java, accessed on June 11, 2026, [https://inside.java/2026/05/19/javaone-better-jdk26/](https://inside.java/2026/05/19/javaone-better-jdk26/)
9. The best practice to save jwt token in httpcookie or memory? : r/angular - Reddit, accessed on June 11, 2026, [https://www.reddit.com/r/angular/comments/1qdjidt/the_best_practice_to_save_jwt_token_in_httpcookie/](https://www.reddit.com/r/angular/comments/1qdjidt/the_best_practice_to_save_jwt_token_in_httpcookie/)
10. Testing Types | Cypress Documentation, accessed on June 11, 2026, [https://docs.cypress.io/app/core-concepts/testing-types](https://docs.cypress.io/app/core-concepts/testing-types)
11. Spring Boot Security Best Practices: Authentication, JWT, and OAuth2 | Katyella, accessed on June 11, 2026, [https://katyella.com/blog/spring-boot-security-best-practices/](https://katyella.com/blog/spring-boot-security-best-practices/)
12. Securing your web apps: Spring security OAuth 2.0 + BFF pattern - ProductDock, accessed on June 11, 2026, [https://productdock.com/securing-your-web-apps-spring-security-oauth-2-0-bff-pattern/](https://productdock.com/securing-your-web-apps-spring-security-oauth-2-0-bff-pattern/)
13. Should I use the BFF pattern or store access token in memory and a refresh token as an http only cookie? Or scrap jwts and just use sessions, accessed on June 11, 2026, [https://stackoverflow.com/questions/79830891/should-i-use-the-bff-pattern-or-store-access-token-in-memory-and-a-refresh-token](https://stackoverflow.com/questions/79830891/should-i-use-the-bff-pattern-or-store-access-token-in-memory-and-a-refresh-token)
14. Which framework feels most production-ready today: LangGraph, CrewAI, AutoGen, or OpenAI Agents? : r/AI_Agents - Reddit, accessed on June 11, 2026, [https://www.reddit.com/r/AI_Agents/comments/1twixwr/which_framework_feels_most_productionready_today/](https://www.reddit.com/r/AI_Agents/comments/1twixwr/which_framework_feels_most_productionready_today/)
15. Backend for Frontend (BFF): Tailoring APIs for UI - Abstract Algorithms, accessed on June 11, 2026, [https://abstractalgorithms.dev/backend-for-frontend-pattern](https://abstractalgorithms.dev/backend-for-frontend-pattern)