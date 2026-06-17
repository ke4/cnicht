CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    application_id VARCHAR(50),
    user_id BIGINT NOT NULL,
    company VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    interview_stage VARCHAR(100) NOT NULL,
    application_date DATE NOT NULL,
    hr_agent VARCHAR,
    recruiter VARCHAR,
    url VARCHAR,
    company_profile TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE interviews (
    id BIGSERIAL PRIMARY KEY,
    job_application_id BIGINT NOT NULL,
    interview_stage VARCHAR(100) NOT NULL,
    interview_date TIMESTAMP NOT NULL,
    details TEXT,
    interviewer VARCHAR(255),
    url VARCHAR(1024),
    is_it_last_stage BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_interviews_job_application FOREIGN KEY (job_application_id) REFERENCES job_applications(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_job_applications_application_id ON job_applications(application_id);
CREATE INDEX idx_job_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_interviews_job_application_id ON interviews(job_application_id);
