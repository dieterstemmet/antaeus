## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

## What are we building?

Pleo's monthly subscription charging service

## Why are we building it?

To automate and simplify Pleo's invoice charging process

## Thought process

  1. Structure - Navigate the project to see what goes where, check out build options and instructions, get a feel for the language and start up an instance
  2. Data - Run curls toward rest endpoints, make sure they all work, as well as note down what the data looks like
  3. Next steps - List a few to-do items to get the basic logic in place
  4. Tests - Start building tests for the desired outcomes
  5. Exception logic - Get the basic exception handling in place and test
  6. Expose functionality - Add rest calls to expose the billing service
  7. Invoice logic - Introduce logic for invoice statuses
  8. Counters - Add success and failure tracking mechanisms
  9. Logging - Add basic logging
  10. Retries - Build in retries on payment failures
  11. Further testing - Introduce additional mock payment providers and tweak some tests
  12. Scheduling - Add automatic payment run scheduling
  13. Test run - Start up the app, run a few payments and ensure logs clearly communicate what's happening behind the scenes

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
