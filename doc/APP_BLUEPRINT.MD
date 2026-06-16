# **Cnicht: Full-Stack TDD Job Application Tracker Blueprint & Step-by-Step Plan**

This master plan establishes the step-by-step functional use cases, TDD criteria, database schema migrations, local Kubernetes configurations, and automated CI/CD roadmaps for **Cnicht**. It is optimized to be used as a sequence of direct instructions for AI coding agents [cite: 0].

## **1. Project Architecture & Monorepo Structure**

Cnicht is structured as a decoupled monorepo pairing a Java Spring Boot backend with a React SPA frontend.
To eliminate Cross-Origin Resource Sharing (CORS) complexity and enhance security, the React application is compiled directly into the Spring Boot static classpath during the packaging phase.
This bundles both components into a single, cohesive executable JAR.

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

### **Platform Baselines & Tooling**

* **Backend Runtime:** **Java 25 (LTS)** [cite: 1] with **Spring Boot 4.1.0** [cite: 22].
* **Build Tool:** **Gradle (Groovy DSL)** configured in multi-project mode [cite: 17, 19].
* **Database:** **PostgreSQL** with schema management run via **Flyway** [cite: 4, 5].
* **Frontend Web:** **React 19** bundled via **Vite** [cite: 15].
* **Testing Engines:** **JUnit 6** and **MockMvc** [cite: 29] (using modern @MockitoBean structures [cite: 32]) on the backend; **Cypress** [cite: 23] for both isolated **Component Testing** [cite: 23] and full browser **End-to-End (E2E) testing** [cite: 25, 26] on the frontend.

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
* application_id (VARCHAR(50)) -- Business reference (e.g., APP-2026-0001) 4
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

*Index Strategy:* Unique index applied to job_applications(application_id) to guarantee ![][image1] search complexity.

## **3. BFF-Emulated Same-Origin Security Pattern**

Cnicht implements a Backend-for-Frontend (BFF) security posture [cite: 3].

* **No Access Tokens in Browser:** Client-side JavaScript is treated as a hostile environment [cite: 3]. Access tokens never touch localStorage or session variables [cite: 3].
* **HttpOnly Cookies:** Authentication is verified using server-side session cookies flagged with HttpOnly, Secure, and SameSite=Strict.6
* **CORS Removal:** By packaging the compiled SPA assets directly into the backend class-path under /static/, the browser reads the UI and calls the APIs on the exact same origin (e.g., http://localhost:8080), bypassing CORS-related vulnerabilities [cite: 15, 115].
* **CSRF Protection:** State-changing methods (POST, PUT, DELETE) require a double-submit CSRF cookie token exchange, coordinated automatically between Axios and Spring Security [cite: 5, 85].

## **4. Step-by-Step TDD Implementation Plan**

Every coding phase must follow strict **Red-Green-Refactor** loops.

### **Task 1: INFRASTRUCTURE SETUP (KUBERNETES & CI/CD)**

#### **Use Case 1.1: Local Kubernetes Deployment**

* **Actor:** DevOps Engineer / Local Runner
* **Preconditions:** monorepo layout exists.
* **Postconditions:** PostgreSQL database runs as a StatefulSet with persistent storage. The Spring Boot app runs as a multi-replica deployment exposed via a LoadBalancer service.
* **Basic Flow:**
    1. Apply database manifests containing StatefulSet and headless Service.
    2. Build local Docker container inside the Kubernetes hypervisor (such as minikube).
    3. Deploy the Spring Boot application configuration pointing to the local cluster database DNS.

#### **Use Case 1.2: GitHub Actions Automated CI/CD**

* **Actor:** GitHub Runner
* **Preconditions:** Code is pushed to main or develop.
* **Postconditions:** Validates compilation, executes unit and component testing, builds JRE production image, and verifies deployment structures.
* **Basic Flow:**
    1. Set up JDK 25 and Node.js 20 runner environments.
    2. Install NPM modules and run Cypress component tests headless.
    3. Execute Gradle JUnit backend test suite.
    4. Package final deployment JAR and trigger Docker compilation.

#### **TDD Testing & Verification Criteria**

NONE for now. It is going to be implemented in the next task.

### **Task 2: SAME-ORIGIN AUTHENTICATION LOOP (LOGIN & LOGOUT)**

#### **Use Case 2.1: Relational Schema DB Initialization**

* **Actor:** System (Flyway Migration Engine) [cite: 4, 5]
* **Preconditions:** Target database exists and is reachable.
* **Postconditions:** Table structures for users initialized with exact columns, indexes, and user-scoped foreign keys.
* **Basic Flow:** On application startup, Flyway reads the migration directory and executes the init SQL script to configure the database schema.


#### **Use Case 2.2: Secure User Authentication (Login)**

* **Actor:** Unauthenticated User
* **Preconditions:** User record exists in the PostgreSQL database with a pre-hashed password [cite: 9].
* **Postconditions:** Valid server-side session is created. The browser receives an encrypted session cookie (JSESSIONID) flagged as HttpOnly, Secure, and SameSite=Strict [cite: 3].
* **Basic Flow:**
    1. User navigates to the login screen.
    2. User submits their email and password.
    3. Backend validates credentials using BCrypt (strength 12) [cite: 9, 68].
    4. Upon successful validation, the backend generates a session, updates the securitxy context, and returns a 200 OK response with the session cookie.
    5. On failure, the backend returns a 401 Unauthorized status.

#### **Use Case 2.3: Authenticated Status Handshake**

* **Actor:** Browser Application
* **Preconditions:** Active session exists.
* **Basic Flow:**
    1. React app boots and makes a silent request to /api/auth/status.
    2. Backend reads the incoming session cookie, verifies its validity, and returns the current user's email with a 200 OK status.
    3. If the session cookie is missing or invalid, the backend returns a 401 Unauthorized response.

#### **Use Case 2.4: Secure Session Termination (Logout)**

* **Actor:** Authenticated User
* **Preconditions:** User is logged in with an active session.
* **Postconditions:** Server-side session is invalidated. Cookie is deleted from browser storage.13
* **Basic Flow:**
    1. User clicks "Log Out" on the dashboard.
    2. React client submits a POST request to /api/auth/logout.
    3. Backend invalidates the HTTP session on the server-side, deletes the JSESSIONID cookie from the response header, and returns 200 OK.

#### **TDD Testing & Verification Criteria**


* **Integration Phase:** Create an end-to-end Cypress test (cypress/e2e/job_tracking_flow.cy.js) that runs against a clean environment.
  It is going to assert a complete user journey: Login, Add, List, Edit, Logout eventually.
  We will start with the Login and Logout step for now. 
  In the following steps we will add all the other steps: Add, List, Edit as we implement the code for these actions.
* **Verification Check:** Verify that pushing this code triggers the GitHub Actions CI/CD pipeline, compiles the base layout, and execution succeds as expected.



* **Backend Red Phase:** Create an integration test class (AuthControllerTest) to verify that accessing /api/applications without a session cookie returns 401 Unauthorized. Assert that sending valid credentials to the login route returns 200 OK and writes a non-null cookie, while invalid inputs return 401.
* **Frontend Red Phase:** Write a Cypress component spec (LoginForm.cy.jsx) that mounts the login form and verifies:
    1. Submission is blocked and displays an error message if fields are left blank.
    2. Submission executes successfully when valid credentials are typed in.
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Auth tests, and compiles successfully.

### **Task 3: ADD A JOB APPLICATION (WITH INTERVIEW DATA)**

#### **Use Case 3.1: Add related tables to the DB Schema**

* **Actor:** System (Flyway Migration Engine) [cite: 4, 5]
* **Preconditions:** Target database exists and is reachable.
* **Postconditions:** Table structures for job_applications, and interviews are added with exact columns, indexes.
* **Basic Flow:** On application startup, Flyway reads the migration directory and executes the init SQL script to configure the database schema.

#### **Use Case 3.2: Add Job Application Record with Optional First Interview Stage**

* **Actor:** Authenticated User
* **Preconditions:** User has a valid authentication session.
* **Postconditions:** A new application record is saved in the database, bound securely to the authenticated user's ID. If interview data is provided, a corresponding record is created in the interviews table.
* **Basic Flow:**
    1. User fills out the Job Application Form (providing Company, Job Title, Stage, Application Date and optional contact details and application ID.
    2. Optionally, the user inputs details for the first scheduled interview stage (Interview Stage, Date, Details, Interviewer, URL, and a toggle for is_it_last_stage).
    3. User submits the form.
    4. React app reads the CSRF token from the browser cookie and appends it to the request header.
    5. Backend validates inputs (ensuring that company, job_title are not blank).
    6. Backend saves both the job application and the nested interview stage (if provided), linking them to the authenticated user's ID, and returns a 201 Created response.

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase:** Write JobApplicationControllerTest using MockMvc and @WithMockUser to assert that:
    1. Requests without a valid CSRF token are blocked with a 403 Forbidden status.
    2. Requests with blank required fields return 400 Bad Request.
    3. Requests with valid inputs (including nested interview rounds) return 201 Created and the saved entity containing the optional interview details.
* **Frontend Red Phase:** Write a Cypress component spec (JobApplicationForm.cy.jsx) that mounts the form and verifies that:
    1. Leaving required fields empty triggers validation warnings on the UI.
    2. Clicking save with valid inputs invokes the save callback with the expected JSON payload structures including nested interview parameters.
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
    2. App renders a grid listing all applications with their company, title, date, and current stage badge.
    3. Each card displays an expandable details section showing its associated interview timeline: round stage name, scheduled date, interviewer, meeting URL, and whether it's marked as the final stage.
    4. If the list is empty, a message reading "No job applications tracked yet" is displayed.

#### **TDD Testing & Verification Criteria**

* **Backend Red Phase:** Write a test asserting that a GET call to /api/applications returns a 200 OK JSON array containing only records that belong to the mocked user context, with child interview objects correctly populated, ensuring strict data isolation.
* **Frontend Red Phase:** Write a Cypress component spec (ApplicationsList.cy.jsx) that mounts the listing view and asserts that:
    1. An empty list displays the empty state message.
    2. A populated list correctly renders the Company Name, Job Title, Stage, and unique Application ID.
    3. Clicking to expand an application reveals the nested interview rounds showing the round stage, date, details, and interviewer.
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Application Listing tests, and compiles successfully.

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

* **Backend Red Phase:** Write integration tests asserting that:
    1. **** Sending a PUT request with valid updated data for an owned job application successfully updates the record (including modifying existing or appending new interviews) and returns 200 OK.
    2. Sending a PUT request to a non-existent application_id returns 404 Not Found.
    3. Trying to edit an application owned by a different user returns 403 Forbidden or 404 Not Found.
* **Frontend Red Phase:** Write a Cypress component spec (EditApplication.cy.jsx) verifying that:
    1. Form inputs are correctly pre-filled with the current values.
    2. **** Submitting the form with updated, valid details triggers the update API callback with correct parameter structures and the target record ID.
* **Verification Check:** Check if the GitHub Actions CI/CD pipeline triggers on push, executes all Application editing tests, passes the complete E2E system suite, and builds successfully.

## **5. Multi-Agent AI Prompts for Code Generation**

These targeted, role-based prompts are designed to be copied and pasted directly into your AI developer assistant (such as JetBrains Genie, Cursor, Claude Code, or ChatGPT) to generate the code for each phase.

### **PROMPT 1: MONOREPO FOUNDATION & KUBERNETES SETUP (Role: Lead Architect)**

Context: You are a Lead Software Architect specializing in Spring Boot and React monorepos.  
Task: Configure the root directory of a project named 'Cnicht' using Java 25 (LTS) and Gradle (Groovy DSL) with a multi-project structure. Create the Kubernetes manifests to deploy this application locally.

Requirements:  
1. Create a root 'settings.gradle' file using Groovy DSL that defines the root project name as 'Cnicht' and includes the 'backend' subproject.  
2. Create a root 'build.gradle' file using Groovy DSL that defines the group 'com.Cnicht' and version '1.0.0-SNAPSHOT'. Declare that we will use Spring Boot 4.1.0 and Java 25.  
3. Configure the gradle-node-plugin (com.github.node-gradle.node) version 7.1.0 inside 'backend/build.gradle' using Groovy DSL, targeting Node.js version 20.11.0, with download = true and '../frontend' as the nodeProjectDir. Set NpmTask 'buildFrontend' to run 'npm run build' inside the frontend project so that React assets are compiled into 'backend/src/main/resources/static'.  
4. Provide a complete enterprise-grade '.gitignore' file.  
5. Create 'k8s/postgres-db.yaml' to configure a persistent local PostgreSQL instance using a Kubernetes StatefulSet (image: postgres:17-alpine) with a Headless Service, a Secret for database credentials, and a VolumeClaimTemplate requesting a 2Gi volume.  
6. Create 'k8s/Cnicht-deployment.yaml' and 'k8s/Cnicht-service.yaml' to run the Spring Boot app with 2 replicas, pulling the image locally, injecting database credentials from the Postgres Secret, and exposing port 8080 via a LoadBalancer service.

### **PROMPT 2: GITHUB ACTIONS PIPELINE (Role: Lead Architect)**

Context: You are a DevOps and QA Specialist focusing on CI/CD pipelines.  
Task: Configure a GitHub Actions workflow.

Requirements:  
1. Create a GitHub Actions workflow '.github/workflows/ci-cd.yml'. It must set up JDK 25 and Node.js 20, run headless Cypress component tests inside the 'frontend/' folder, compile and run JUnit tests inside the 'backend/' folder, and compile the combined bootJar.  
2. Include a Docker build step in the workflow to package the application, using 'eclipse-temurin:25-jre-alpine' as the runtime base image. Include a mock Kubernetes apply block for deployment on main branch pushes.  


### **PROMPT 3: FLYWAY SCHEMA INIT WITH INTERVIEWS (Role: Database Architect)**

Context: You are a Database Architect specializing in PostgreSQL and Flyway schema design.  
Task: Create the relational database migrations for the 'Cnicht' application to replace a flat tracking spreadsheet and support multiple interview rounds.

Requirements:  
1. Write a Flyway migration file named 'V1__Init_Schema.sql' inside 'backend/src/main/resources/db/migration/'.  
2. Create a 'users' table with columns: 'id' (BIGSERIAL PRIMARY KEY), 'email' (VARCHAR(255) UNIQUE NOT NULL), 'password_hash' (VARCHAR(255) NOT NULL), and 'created_at' (TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP).  
3. Create a 'job_applications' table with columns:  
- 'id' (BIGSERIAL PRIMARY KEY)  
- 'application_id' (VARCHAR(50) UNIQUE NOT NULL) -- Business generated unique identifier  
- 'user_id' (BIGINT NOT NULL, FK to users(id) ON DELETE CASCADE)  
- 'company' (VARCHAR(255) NOT NULL)  
- 'job_title' (VARCHAR(255) NOT NULL)  
- 'interview_stage' (VARCHAR(100) NOT NULL)  
- 'application_date' (DATE NOT NULL)  
- 'hr_agent', 'recruiter', 'url' (VARCHAR), 'company_profile' (TEXT)  
- 'created_at' and 'updated_at' (TIMESTAMPS NOT NULL DEFAULT CURRENT_TIMESTAMP)  
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

### **PROMPT 4: TASK 2 - SAME-ORIGIN SECURITY & AUTHENTICATION TDD (Role: TDD Backend Developer)**

Context: You are a TDD Backend Engineer specializing in Spring Security 7, Java 25, and MockMvc.  
Task: Implement cookie-based login, status check, and logout endpoints for 'Cnicht' using strict TDD.

Requirements:  
1. Write a JUnit 6 controller integration test file 'AuthControllerTest.java'. It must assert that accessing '/api/applications' without an active session cookie returns '401 Unauthorized'.  
2. Write tests to assert that posting valid credentials (using MockMvc URL-encoded params 'username' and 'password') to '/api/auth/login' returns '200 OK' and sets a session cookie, while invalid inputs return '401'.  
3. Write a test for '/api/auth/logout' asserting that it invalidates the current session, deletes the cookie, and returns '200 OK'.  
4. Implement 'SecurityConfig.java' to pass these tests. Configure the session creation policy to 'IF_REQUIRED' and enable stateful session tracking.  
5. Configure BCryptPasswordEncoder with a strength of 12 for password verification.  
6. Enable cookie-based CSRF protection using 'CookieCsrfTokenRepository.withHttpOnlyFalse()' so React can read the token.  
7. Set cookie configurations so that the session cookie is flagged as httpOnly, Secure, and SameSite=Strict.

### **PROMPT 5: TASK 2 - FRONTEND LOGIN SCREEN TDD (Role: TDD Frontend Developer)**

Context: You are a senior React 19 and Cypress developer practicing strict TDD.  
Task: Create the login screen component and its test suite for the 'Cnicht' application in the frontend project.

Requirements:  
1. Write a Cypress component test file 'LoginForm.cy.jsx' using 'cy.mount()'.  
2. The tests must assert:  
- Clicking submit while fields are empty prevents execution and displays an error message reading "Email and password are required".  
- Inputting 'user@Cnicht.com' and a password, then clicking submit, correctly triggers the 'onLogin' prop callback with the corresponding values.  
3. Write the React component 'LoginForm.jsx' in Tailwind CSS with 'data-cy' target attributes matching your Cypress selectors. Implement the state and submit handlers required to pass your component tests. 
4. Create a complete Cypress E2E spec 'cypress/e2e/job_tracking_flow.cy.js' to verify the entire system flow (Login, Logout) against the running application.

### **PROMPT 6: TASKS 3, 4, 5 - JOB APPLICATIONS & NESTED INTERVIEWS REST API TDD (Role: TDD Backend Developer)**

Context: You are a TDD Spring Boot Engineer specializing in Spring Data JPA and JSpecify null safety.  
Task: Create the API endpoints to create, list, and edit job applications and associated nested interviews using strict TDD.

Requirements:  
1. Write JUnit 6 integration tests verifying:  
- POST '/api/applications' creates a new application record, supporting nested interview array data. If required fields like 'company' or 'application_id' are missing, return '400 Bad Request'.  
- POST requests without a valid CSRF token in the headers return '403 Forbidden'.  
- GET '/api/applications' retrieves only the records owned by the current authenticated user, with eagerly mapped collections of interview rounds.  
- PUT '/api/applications/{appId}' updates the application fields (including nested interviews), returning '200 OK' for successful updates, '404 Not Found' if the record does not exist, or '403 Forbidden' if it belongs to a different user.  
2. Implement 'JobApplicationController.java', 'JobApplicationService.java', 'JobApplicationRepository.java', 'InterviewRepository.java', and matching domain JPA entities.  
3. Apply JSpecify '@NullMarked' and '@Nullable' annotations to your request and response record DTOs to enforce strict compile-time null safety.

### **PROMPT 7: TASKS 3, 4, 5 - FRONTEND JOB TRACKING & INTERVIEW COMPONENTS TDD (Role: TDD Frontend Developer)**

Context: You are a React developer specializing in Cypress Component Testing and Axios API integration.  
Task: Create the components to add, list, and edit job applications and interviews in 'Cnicht' using strict TDD.

Requirements:  
1. Write Cypress component tests for 'JobApplicationForm.cy.jsx' (verifying fields for adding application metadata and optional nested first interview round, ensuring validation warnings trigger on empty required fields) and 'ApplicationsList.cy.jsx' (verifying empty states, record grid renders, and the display of associated interview stages showing Stage, Date, Details, Interviewer, URL, and 'Last Stage' badge).  
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

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACsAAAAaCAYAAAAue6XIAAAChklEQVR4Xu2WXWiOYRjHL9/kcxiZJAfSUg4mlJYdTO1AyoQj5LPtZIiSNeU9IqXGgRM5GGsnO9JW5CPkhHAkac1XZLXsQNF8f/3/Xc+be/+e994zmaL3V79W/+vas/t+dt/3c5sV+XcogRM0jDAWlmr4O4yDq+AaOEdqaZTBu3CqFiJwYrdhuRaysgh2wEcwB4/Bt/AynP2rbQCc2B24XQsJE+EBDRPWwmdwlhYGoxH2wq1wdJDPh/fgczgpyPOcMK+PCDIuiUPwGvwI+4Oacgm2aRijGX6Cy7WQsA3+gEckn2L+5ldKzjfVAKvgLYsPlv9NTmixFtJYZz6Q01oI4Jpkz33J98KHkilcQrHBkuvwlIYKN0QP/GA+oEJMht/hG8mvwguSKVkGe8YGn7TtN39jbI7BfzP7nkr+xHzNxsgy2IPmz5+uhRBuADbt0oJQZ97XGWSj4GdYH2RpZBnsevPnF1y3Y8w3FZtWSE3hINm3L8h4SjCrDrI0ONj3GgpLzJ/FDZkKz0euQ5p2JOXhbNnzCo4P8grzP8CfMThY7okY88yftVoLIVyDbIptrnPmPVskX5DkGyRXsgyWX0s+a6EWQs6bN23SQgK/Sl/hHi2YnxD8XR7+MThYnqMxdsBv5veFgvC7z+OI3+gZUtsJv8DdkodwaZzVULhhvhFjS+0ofKlhGsvgA/NjiG/wOOyGV2Bl0JdGC7wpGeHEH5t/ovmFo3wpXbAm6MvTDi9qWAjeBZbCzeY3Le70LHC99tnAjTdURppPrFYLfxr+Id7QmrQwBLjMXpif28MOr3nv4FwtZGAafA03amE4OQxbNczASfM7818nB2dqGIGXKG5mLqUiRf4rfgLYyoIyFobQfgAAAABJRU5ErkJggg==>