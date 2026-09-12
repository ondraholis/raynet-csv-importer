# RaynetCRM
RaynetCRM is a Java application for importing client data from CSV files into a CRM system.

## Features
- Import client data from CSV files
- Process client data and update existing clients or create new ones in the CRM system
- Asynchronous processing for improved performance
- Integration with external CRM system via REST API
- API-key authentication protecting the upload endpoint

## Requirements
- Gradle
- Docker
- Docker Compose

## Getting Raynet CRM access and an API key
The app authenticates against the real Raynet CRM API using HTTP Basic auth (username + API key) plus an
`X-Instance-Name` header, so you need a Raynet CRM instance and a generated API key before it will run at all (see
the "Important" note below).

1. You need access to a Raynet CRM instance (an existing company account, or a trial) on the **PROFESSIONAL** tier or
   higher — the **START** tier does not support API keys at all.
2. Only a user with access to the **Nastavení** (Settings) button can generate API keys, and generation itself
   requires administrator rights on most accounts. In Nastavení, open **API klíče** in the left-hand menu, then click
   the green **Nový API klíč** button.
3. Pick the user account the key should be generated for (usually an admin account, or a separate integration-only
   license). Name the key, then **copy it before saving** — Raynet only shows the generated key once. This copied
   value is your `RCRM_API_KEY`.
   (Full walkthrough: [Jak vygenerovat API klíč?](https://support.raynetcrm.com/hc/cs/articles/360032478072-Jak-vygenerovat-API-kl%C3%AD%C4%8D))
4. `RCRM_API_USERNAME` is the login/email of the user account the key was generated for in step 3.
5. `RCRM_API_INSTANCENAME` is your Raynet instance name, e.g. for `curl -u 'user:key' -H 'X-Instance-Name:
   moje-crm' https://app.raynet.cz/api/v2/company/` the instance name is `moje-crm`. You can also look it up via the
   `security/info` API endpoint if you're not sure of it.
6. Full API reference (auth details, rate limits, endpoints): [app.raynet.cz/api/doc](https://app.raynet.cz/api/doc/).

## Installation
- Clone the repository: git clone https://github.com/sajf/raynet-csv-importer.git
- Navigate to the project directory: cd raynet-crm
- Copy **.env.example** to **.env** and fill in **RCRM_API_KEY**, **RCRM_API_USERNAME**, **RCRM_API_INSTANCENAME**
  (see above), **RCRM_API_EMAIL_TO**, **SPRING_MAIL_PASSWORD** and **IMPORT_API_KEY**. `.env` is git-ignored and is
  the only place secrets/credentials should live — do not put them back into `application.properties` or
  `docker-compose.yml`.
- **Important:** the app calls the real Raynet CRM API once on startup (to read the initial rate limit) before it
  will accept any requests. Without a valid `RCRM_API_KEY`/`RCRM_API_INSTANCENAME` for an actual Raynet instance, the
  application **fails to start**, both in Docker and when run locally — see [Local development](#local-development)
  for a workaround if you don't have real credentials.
- Build the project: **./gradlew build**
- The application is prepared to run in docker containers, run: **docker-compose up -d** to start the java and mysql
  containers
- Whenever you change a value in `.env`, rerun **docker-compose up -d --build** so the app image picks up the new
  settings (a plain restart is not enough since the jar is only rebuilt on `build`).

## Tests
- Run **./gradlew test** to trigger the unit tests

## Local development
You don't need to run the whole stack in Docker to work on the app:
- Start only the database: **docker-compose up -d mysql**
- Run `RaynetCrmApplication` from your IDE as usual. The defaults in `application.properties` already point at
  `localhost:3306` with the same credentials the `mysql` container exposes, so no extra configuration is needed as
  long as `.env` (used for the MySQL container) matches the datasource defaults.
- The Raynet API startup call described above still applies locally. `RCRM_API_URL` defaults to the real production
  API (`https://app.raynet.cz/api/v2/`) and shouldn't be changed — the app requires valid credentials for a real
  Raynet instance to start, see [Getting Raynet CRM access and an API key](#getting-raynet-crm-access-and-an-api-key).

## Authentication

The `/uploadData` endpoint is protected by a static API key. Set the key via the **IMPORT_API_KEY** environment
variable (backed by the `security.api-key` property); it is required on every request through the **`X-API-Key`**
header. The application **fails to start** when the key is blank, so the endpoint can never be exposed
unauthenticated. Requests without a valid key are rejected with **401 Unauthorized**.

Because the key is a bearer secret, always call the endpoint over TLS in production and keep the key in `.env`
(or a secret manager), never in source control.

## Usage
- Prepare your client data in a CSV file with the following format:
regNumber;title;email;phone
123456;Example Company;example@example.com;123-456-7890
- Call the **localhost:8080/uploadData** endpoint with the csv file as a request body, sending your API key in the
  `X-API-Key` header, e.g.:

```
curl -F 'file=@clients.csv;type=text/csv' -H "X-API-Key: $IMPORT_API_KEY" http://localhost:8080/uploadData
```
- The application will process the CSV file asynchronously, updating existing clients or creating new ones in the CRM system
- Not processed clients (after rate limit hit) will be processed in hourly scheduled job

## Technologies Used
- Java
- Gradle
- Spring Framework
- Spring Boot
- Lombok
- Flyway
- OpenCSV
- RESTful API
