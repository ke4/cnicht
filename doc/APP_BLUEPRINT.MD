### **1. Project Architecture & Monorepo Structure**

The repository uses a single git monorepo pattern named cnicht. Inside, the codebases are strictly decoupled: backend/ manages the Java Spring Boot service, and frontend/ hosts the React application.  
cnicht/  
├── .gitignore  
├── build.gradle  
├── settings.gradle  
├── gradlew  
├── gradlew.bat  
├── gradle/  
│ └── wrapper/  
├── backend/  
│ ├── build.gradle  
│ └── src/  
│ ├── main/  
│ │ ├── java/co/uk/kare/dev/cnicht/  
│ │ │ ├── CnichtApplication.java  
│ │ │ ├── controller/  
│ │ │ ├── dto/  
│ │ │ ├── entity/  
│ │ │ ├── repository/  
│ │ │ └── service/  
│ │ └── resources/  
│ │ ├── application.yml  
│ │ └── db/migration/  
│ │ └── V1__Create_Job_Applications_Table.sql  
│ └── test/  
│ └── java/co/uk/kare/dev/cnicht/  
└── frontend/  
├── package.json  
├── vite.config.js  
├── cypress.config.js  
├── index.html  
├── src/  
│ ├── main.jsx  
│ ├── App.jsx  
│ ├── components/  
│ └── assets/  
└── cypress/  
├── fixtures/  
├── support/  
├── e2e/  
└── component/

#### **Monorepo Root Build Configuration (Groovy DSL)**

To coordinate the builds, the root project configures global build parameters and includes the backend subproject1.  
**settings.gradle**

```Groovy  
rootProject.name = 'cnicht'  
include 'backend'
```

**build.gradle**

```Groovy  
plugins {  
    id 'org.springframework.boot' version '4.0.7' apply false  
    id 'io.spring.dependency-management' version '1.1.7' apply false  
    id 'java'  
}

allprojects {  
    group = 'com.cnicht'  
    version = '0.0.1-SNAPSHOT'  
}
```

### **2. Technology Stack & Platform Baselines**

This project utilizes highly stable, enterprise-grade baselines for the 2026 development cycle:

* **Backend Runtime & Framework:** **Java 25 (LTS)**1 alongside **Spring Boot 4.0.7**1. This pairing features granular auto-configuration jars1 and first-class native optimization patterns.
* **Backend Build Tool:** **Gradle (Groovy DSL)**1.
* **Database Integration:** **PostgreSQL** configured with **Flyway** for clean schema migrations.
* **Frontend Library & Bundler:** **React 19** compiled via **Vite** for optimized assets and lightning-fast developer compilation.
* **Testing Suites:**
    * **Backend:** **JUnit 5/6** with **MockMvc**1 (using @MockitoBean for isolated mock assertions1).
    * **Frontend:** **Cypress**1 configured for both highly isolated **Component Testing**1 and full-path **End-to-End (E2E) testing**1.

### **3. Database Mapping**

To import your tracking data cleanly from the Google Spreadsheet, the spreadsheet columns are mapped to a structured, relational SQL schema. An explicit application_id business tracking column (e.g., APP-2026-0001) is introduced alongside a system-managed primary key. We place a unique index on application_id to ensure ![][image1] lookup times during query and database indexing tasks.

| Column Name | Data Type | Constraints | Spreadsheet Source Column |
| :---- | :---- | :---- | :---- |
| id | BIGSERIAL | PRIMARY KEY | - |
| application_id | VARCHAR(50) | UNIQUE NOT NULL | - (Business Generated ID) |
| user_id | BIGINT | NOT NULL | - (Identifies the App User) |
| company | VARCHAR(255) | NOT NULL | Company |
| job_title | VARCHAR(255) | NOT NULL | Job Title |
| interview_stage | VARCHAR(100) | NOT NULL | Interview Stage |
| application_date | DATE | NOT NULL | Application Date |
| hr_agent | VARCHAR(255) | NULL | HR Agent |
| recruiter | VARCHAR(255) | NULL | Recruiter |
| url | VARCHAR(1024) | NULL | URL |
| company_profile | TEXT | NULL | Company Profile |

#### **Flyway Migration File: backend/src/main/resources/db/migration/V1__Create_Job_Applications_Table.sql**

```SQL  
CREATE TABLE job_applications (  
id BIGSERIAL PRIMARY KEY,  
application_id VARCHAR(50) UNIQUE NOT NULL,  
user_id BIGINT NOT NULL,  
company VARCHAR(255) NOT NULL,  
job_title VARCHAR(255) NOT NULL,  
interview_stage VARCHAR(100) NOT NULL,  
application_date DATE NOT NULL,  
hr_agent VARCHAR(255),  
recruiter VARCHAR(255),  
url VARCHAR(1024),  
company_profile TEXT,  
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  
);

CREATE INDEX idx_job_apps_user ON job_applications(user_id);  
CREATE UNIQUE INDEX idx_job_apps_business_id ON job_applications(application_id);
```

### **4. Backend Test-Driven Development (TDD) Pattern**

TDD requires writing the test suite first, validating that it fails, and then implementing the minimum code necessary to make the test pass. The code below illustrates a standard REST endpoints test utilizing modern @MockitoBean mocking annotations1.

#### **backend/build.gradle (Subproject Configuration)**

```Groovy  
plugins {  
    id 'org.springframework.boot'  
    id 'io.spring.dependency-management'  
    id 'java'  
}

java {  
    toolchain {  
        languageVersion = JavaLanguageVersion.of(25)  
    }  
}

repositories {  
    mavenCentral()  
}

dependencies {  
    implementation 'org.springframework.boot:spring-boot-starter-web'  
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'  
    implementation 'org.springframework.boot:spring-boot-starter-validation'  
    implementation 'org.flywaydb:flyway-core'  
    implementation 'org.flywaydb:flyway-database-postgresql'  
    runtimeOnly 'org.postgresql:postgresql'  
    testImplementation 'org.springframework.boot:spring-boot-starter-test'  
}

tasks.named('test') {  
    useJUnitPlatform()  
}
```

#### **Test Class: backend/src/test/java/com/cnicht/controller/JobApplicationControllerTest.java**

```Java  
package com.cnicht.controller;

import com.cnicht.dto.JobApplicationDto;  
import com.cnicht.service.JobApplicationService;  
import com.fasterxml.jackson.databind.ObjectMapper;  
import org.junit.jupiter.api.Test;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;  
import org.springframework.http.MediaType;  
import org.springframework.test.context.bean.override.mockito.MockitoBean;  
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;  
import java.util.Optional;

import static org.mockito.BDDMockito.given;  
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;  
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;  
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobApplicationController.class)  
class JobApplicationControllerTest {

    @Autowired  
    private MockMvc mockMvc;

    @MockitoBean  
    private JobApplicationService jobApplicationService;

    @Autowired  
    private ObjectMapper objectMapper;

    @Test  
    void shouldReturnApplicationByBusinessId() throws Exception {  
        String appId = "APP-2026-0001";  
        JobApplicationDto appDto = new JobApplicationDto(  
            appId, "Tech Corp", "Senior Engineer", "Interviewing",  
            LocalDate.of(2026, 6, 12), "Alice Smith", "Bob Jones",  
            "https://techcorp.com/jobs", "Modern SaaS provider"  
        );

        given(jobApplicationService.findByApplicationId(appId)).willReturn(Optional.of(appDto));

        mockMvc.perform(get("/api/applications/" + appId)  
                .contentType(MediaType.APPLICATION_JSON))  
                .andExpect(status().isOk())  
                .andExpect(jsonPath("$.applicationId").value(appId))  
                .andExpect(jsonPath("$.company").value("Tech Corp"))  
                .andExpect(jsonPath("$.jobTitle").value("Senior Engineer"))  
                .andExpect(jsonPath("$.interviewStage").value("Interviewing"));  
    }  
}
```

### **5. Frontend TDD with Cypress**

In a React TDD workflow using Cypress, we test components in strict isolation via **Cypress Component Testing**1. This mounts and renders components directly in a real browser context using Vitest/Vite, bypassing the need to spin up a local Webpack/Vite server externally1.

#### **Cypress Monorepo Setup**

First, install Cypress in the frontend directory1:

```Bash  
cd frontend  
npm install cypress --save-dev
```

#### **frontend/cypress.config.js**

```JavaScript  
import { defineConfig } from "cypress";

export default defineConfig({  
    e2e: {  
        baseUrl: "http://localhost:5173",  
        supportFile: "cypress/support/e2e.js",  
    },  
        component: {  
            devServer: {  
                framework: "react",  
                bundler: "vite",  
            },  
            supportFile: "cypress/support/component.js",  
        },  
    }
);
```

#### **Step 1: Write the Failing Test (frontend/cypress/component/JobRow.cy.jsx)**

```JavaScript  
import React from 'react';  
import JobRow from '../../src/components/JobRow';

describe('JobRow Component TDD', () => {  
    it('renders company details and triggers click event on select', () => {  
        const mockApp = {  
            applicationId: 'APP-101',  
            company: 'Innovate LLC',  
            jobTitle: 'Frontend Engineer',  
            interviewStage: 'Technical Test'  
        };
    
        const onSelectSpy = cy.spy().as('onSelectSpy');
    
        // Mount the isolated React component  
        cy.mount(<JobRow application={mockApp} onSelect={onSelectSpy} />);
    
        // Assert correct rendering  
        cy.contains('Innovate LLC').should('be.visible');  
        cy.contains('Frontend Engineer').should('be.visible');  
        cy.contains('Technical Test').should('be.visible');
    
        // Interact and assert behavior  
        cy.get('[data-cy="row-container"]').click();  
        cy.get('@onSelectSpy').should('have.been.calledWith', 'APP-101');  
    });  
});
```

#### **Step 2: Write Minimum Code to Pass (frontend/src/components/JobRow.jsx)**

```JavaScript  
import React from 'react';

export default function JobRow({ application, onSelect }) {  
    return (  
        <div   
            data-cy="row-container"   
            onClick={() => onSelect(application.applicationId)}   
            style={{ padding: '10px', borderBottom: '1px solid #ccc', cursor: 'pointer' }}  
        >  
        <h3>{application.company}</h3>  
        <p>{application.jobTitle} - <strong>{application.interviewStage}</strong></p>  
        </div>  
    );  
}
```
### **6. Architectural Evolution: Direct API vs. BFF Pattern**

[ DIRECT API PATTERN ]  
React SPA (Web) ──────────> Spring Boot API REST Endpoints (Port 8080)  
[ REFACTORED BFF PATTERN ]  
React SPA (Web) ───[SameSite Session Cookie]───> Spring Cloud Gateway BFF (Port 80)  
│  
[Bearer JWT] ─┼───> Spring Boot API Core (Port 8080)  
│  
React Native Mobile ─[Direct JWT Access Token]──────────┘

#### **Trade-off Analysis**

##### **Starting Directly with a Web-to-Backend Connection**

* **Pros:** Simpler deployment; minimal infrastructure latency1; faster initial prototype velocity for a single developer.
* **Cons:** Insecure if authentication tokens are stored in the browser1; complicates mobile refactoring since endpoints tailored for the desktop UI will have to be redesigned later to limit payload size for mobile bandwidth1.

##### **Starting Immediately with a Backend for Frontend (BFF)**

* **Pros:** Outstanding security by default1. All access, ID, and refresh tokens live strictly on the server (e.g., in a Spring Cloud Gateway session cache1), with only encrypted, HTTP-only SameSite=Strict cookies sent to the SPA browser environment1. This protects against XSS supply-chain attacks completely1.
* **Cons:** Higher initial setup effort; redundant routing layer with minor latency overhead1.

#### **Recommended Strategy**

Begin with a **Direct Web-App-to-Backend** design, but **emulate BFF security practices from day one**. Rather than creating an independent routing proxy server (like Spring Cloud Gateway) on day one, write your backend so it issues **HttpOnly, Secure, and SameSite=Strict cookies** representing the server-side session directly to your React app1. This ensures top-tier web security immediately.  
When you add a mobile app later, introduce a dedicated routing API Gateway (like Spring Cloud Gateway acting as your Mobile and Web BFF) to handle token conversion1. This approach yields maximum prototype speed with no compromise on security or structural scalability1.

### **7. Execution Plan & Multi-Agent Prompt Roadmap**

This roadmap is designed for direct sequential input to an AI code agent (such as Claude Code, Cursor, or ChatGPT). It enforces the required developer roles as localized cognitive personas.

| Step | Phase | Persona | Deliverables |
| :---- | :---- | :---- | :---- |
| 1 | Infrastructure | Lead Architect | Monorepo structure, Groovy Gradle files |
| 2 | DB Schema | Lead Architect | Flyway schema file with user and application IDs |
| 3 | Core API TDD | Backend Developer | JPA entities, MockMvc controller endpoints |
| 4 | UI Component TDD | Frontend Developer | React files, Cypress component specs |
| 5 | Security / Auth | Lead Architect | Spring Security cookie-session engine |
| 6 | E2E Integration | QA Engineer | Integration endpoints and Cypress E2E flows |

#### **Prompt 1: Monorepo Foundation & Build Infrastructure (Role: Lead Architect)**

**Context:** You are an expert Lead Software Architect specializing in enterprise monorepos.  
**Task:** Initialize a clean monorepo folder structure named cnicht containing a backend subproject using Gradle (Groovy DSL) and a frontend subproject using React + Vite.  
**Requirements:**

1. Set up a root settings.gradle file utilizing Groovy DSL that declares the root project name as cnicht and includes the subproject backend.
2. Set up a root build.gradle file configured with Java 25 LTS as the core target platform and Spring Boot 4.0.7 as the standard framework baseline.
3. Provide a standard enterprise .gitignore file that excludes .gradle/, build/, node_modules/, and Vite distributions.

#### **Prompt 2: Database Schema Creation (Role: Lead Architect)**

**Context:** You are a Database Architect specializing in PostgreSQL and data schema design.  
**Task:** Map a legacy job application tracking Google Spreadsheet to a clean, relational SQL schema.  
**Requirements:**

1. Write a Flyway migration file named V1__Create_Job_Applications_Table.sql.
2. Create the job_applications table with column mappings for: Company, Job Title, Interview Stage, Application Date, HR Agent, Recruiter, URL, and Company Profile.
3. Add system columns: a system-managed primary key id (BIGSERIAL), a unique business-facing identifier application_id (VARCHAR), and a user_id (BIGINT) to support multi-user security boundaries.
4. Ensure proper table indices are created to optimize search and validation performance.

#### **Prompt 3: Backend Controller TDD Implementation (Role: TDD Backend Developer)**

**Context:** You are an expert Spring Boot Developer practicing strict Test-Driven Development.  
**Task:** Implement the JobApplicationController using Java 25 and Spring Boot 4.0.7, mapping controller endpoints to perform CRUD actions.  
**Requirements:**

1. Write the tests first: implement JUnit 5 integration tests using MockMvc and @MockitoBean mocking annotations to verify endpoint behaviors.
2. Include validation checks verifying that requests lacking mandatory fields (such as company, job_title, or application_id) are rejected with 400 Bad Request.
3. Implement the matching JobApplicationController, JobApplicationService, JobApplicationRepository, and domain entities to successfully pass all written integration tests.

#### **Prompt 4: Frontend UI Component TDD (Role: TDD Frontend Developer)**

**Context:** You are a senior React Engineer with expertise in frontend TDD and Cypress component testing.  
**Task:** Create the foundational tracking list interface for cnicht using React 19, Tailwind CSS, and Cypress.  
**Requirements:**

1. Install and configure Cypress for component testing inside the frontend/ subdirectory, outputting a valid cypress.config.js pointing to a Vite bundler.
2. Write Cypress component tests for two components:
    * JobRow: Should render tracking columns cleanly and trigger selected events.
    * JobDashboard: Should mount and render lists of applications, and show error states if loading fails.
3. Implement the React component code required to make all component test assertions pass.

#### **Prompt 5: Security & Session Integration (Role: Lead Architect)**

**Context:** You are an Application Security Expert specializing in OAuth2, Spring Security, and session management.  
**Task:** Add cookie-based security configurations to the cnicht application.  
**Requirements:**

1. Configure Spring Security in the backend using the standard filter chain strategy.
2. Implement authentication using session cookies configured with security flags (httpOnly, Secure, and SameSite=Strict).
3. Write a backend test verifying that unauthorized requests to protected REST paths are rejected with 401 Unauthorized, while requests with a valid session run successfully.

#### **Prompt 6: End-to-End System Testing (Role: QA Specialist)**

**Context:** You are a QA Automation Engineer specializing in full-stack E2E integrations.  
**Task:** Write comprehensive Cypress E2E flows verifying complete, authenticated user workflows.  
**Requirements:**

1. Create a Cypress E2E test file named cypress/e2e/job_tracking_flow.cy.js.
2. Implement steps to:
    * Visit the dashboard (triggering login if unauthenticated).
    * Complete authentication successfully.
    * Click to add a new job application, fill in tracking fields, and save.
    * Verify that the added tracking entry renders with the correct spreadsheet mapping columns and unique application_id.
    * Update the status of the job from "Applied" to "Interviewing" and verify persistence.

#### **Works cited**

1. [https://github.com/gradle/gradle/issues/35204](https://github.com/gradle/gradle/issues/35204)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACsAAAAaCAYAAAAue6XIAAABwklEQVR4Xu2WTytEURjGH/9ZEAvKAgufQWQjf8IHUCxkslD2SvIRlJIsfAcfwYaNZCU2oiyQBRaUQv6+r3MuM49773tGQ9H86qnxO89953TnzjFAkb/DIAuDZkkpy+/QJlmVrEjqaC2OackcywBeWeTDEtyACf93q+RCcv/R+EqL5JxlFvVI3lSl5IWlhX4cOnCTFzxPSB6q11WTa5Sc+LUoSWxJFlmmocOOWWbRB9fpJ98teSDHWJstQ/p6Dmewy9GdXyP/CPtZtTar6PoAS6YHrrhBnmmA612TV1dDjgnZ7IFkhyWjd0YH8TPHjMP1drNcrXcWIZudh90JGqQcwvX0iIro9c4i5D3GYHSaEDZIietNxrg44q5lOmF0om/hHS8QI3A9PtYy3luEbLYDdidoUFKnC/GeSbo+m1HYHdwgvRQd7BW8gM8TwiJks3r8WZ13tLTHUriEOy3S0Gv1X2YaIZvdR+5Jk8oV3MBtuGdYX+tDb6G9GZYePZP1N8Opj77mczpC5wyxLDSzkluWeVIC+84XDH2jcpZ5sC5ZZvlTDEuOWAaivzmeWf40C5IplgH8+kYjMiwM2iVVLIsU+Q+8AcPof4U5yGDQAAAAAElFTkSuQmCC>