# Pharma — Pharmacy Management System

Pharma is a pharmacy management project for a 300-SKU pilot in Damanhour, Beheira. It supports catalogue intake, stock reconciliation and movements, batch receiving and expiry monitoring, owner-approved price updates, purchasing, customer credit and refill registers, operational reporting, role permissions, and data exports and backups.

Developed for Advanced Software Engineering (CSE454) at Alamein International University.

## Project links

- Repository: [Pharma-System](https://github.com/Sherif34/Pharma-System)
- Jira board: [PHAR Scrum board](https://shirefkelany64.atlassian.net/jira/software/projects/PHAR/boards/2/backlog)
- GUI design: [Pharma CSE454 GUI](https://www.figma.com/design/9DH4MLYnKboqNoSYEqjctv/Pharma-%E2%80%94-CSE454-GUI?node-id=0-1)

## Team

| Student ID | Name | GitHub username |
| --- | --- | --- |
| 22100371 | Sherif Kilani | [Sherif34](https://github.com/Sherif34) |
| 20100359 | Ahmed Abdelazeem | To be provided |
| 22100807 | Rober Mikhail | [RoberSamy1](https://github.com/RoberSamy1) |
| 21100804 | Amr tamer | To be provided |

## Services and ownership

The service ownership table in proposal version 0.3 defines the following allocation.

| Service | Responsibilities | Requirements | Owner | Feature branch |
| --- | --- | --- | --- | --- |
| catalogue-service | Pilot catalogue and CSV import | FR1–FR2 | Sherif Kilani | feature/catalogue-service |
| inventory-service | Opening counts and stock movements | FR3–FR4 | Sherif Kilani | feature/inventory-service |
| receiving-expiry-service | Supplier deliveries and expiry monitoring | FR5–FR6 | Ahmed Abdelazeem | feature/receiving-expiry-service |
| pricing-service | Price submission and approval | FR7–FR8 | Amr tamer | feature/pricing-service |
| purchasing-service | Supplier requests and order suggestions | FR9–FR10 | Sherif Kilani | feature/purchasing-service |
| stock-review-service | Shift exceptions and adjustment approval | FR11–FR12 | Ahmed Abdelazeem | feature/stock-review-service |
| customer-register-service | Customer credit and refill reminders | FR13–FR14 | Ahmed Abdelazeem | feature/customer-register-service |
| operations-reporting-service | Inventory accuracy and expiry-loss reporting | FR15–FR16 | Rober Mikhail | feature/operations-reporting-service |
| access-control-service | Staff roles and permission enforcement | FR17–FR18 | Rober Mikhail | feature/access-control-service |
| data-continuity-service | Local exports and daily backups | FR19–FR20 | Amr tamer | feature/data-continuity-service |

Rober Mikhail owns access-control-service, as confirmed by the team.

## Planned technology stack

The team plans to use the following technology stack.

| Layer | Planned technology |
| --- | --- |
| Client and user interface | React and TypeScript |
| Application runtime and framework | Node.js and Express |
| Database | PostgreSQL |
| Hosting and deployment | Docker containers; hosting provider to be selected |
| Backup storage | Daily PostgreSQL backups to local storage; final storage location to be selected |
| Source control and collaboration | Git and GitHub |
| Project management | Jira |
| UI design | Figma |

## Planned folder structure

```text
Pharma-System/
├── README.md
├── .github/
│   └── CODEOWNERS
├── frontend/
├── docs/
│   ├── proposal/
│   └── diagrams/
└── services/
    ├── catalogue-service/
    ├── inventory-service/
    ├── receiving-expiry-service/
    ├── pricing-service/
    ├── purchasing-service/
    ├── stock-review-service/
    ├── customer-register-service/
    ├── operations-reporting-service/
    ├── access-control-service/
    └── data-continuity-service/
```

## Branch workflow

- `main`: stable integration branch, protected through GitHub settings. Require a pull request and at least one approving review before merging.
- `develop`: integration branch for ongoing development.
- `feature/<service-name>`: service work maintained by the corresponding service owner listed above. Open pull requests into `develop`; promote reviewed integration changes into `main`.

Service ownership is recorded in the table above and in `.github/CODEOWNERS` for owners whose GitHub usernames are available. Owners maintain the corresponding feature branches. All four team members and both teaching assistants require repository access; outstanding usernames and invitation acceptance must be completed.

## Pilot scope

The pilot covers the top 300 fast-moving SKUs. The existing POS continues during a parallel run. The route for bringing ordinary POS sales quantities into Pharma must be established before relying on its stock records. Catalogue CSV import alone does not establish stock synchronization.

Prescriptions, insurance, and full-catalogue migration are outside the initial pilot. Refill reminders are an internal due list; automated customer messaging is not specified.
